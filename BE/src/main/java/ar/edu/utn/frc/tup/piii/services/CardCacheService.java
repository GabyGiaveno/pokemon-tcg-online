package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.response.CardResponse;
import ar.edu.utn.frc.tup.piii.entities.Card;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages the local card cache synchronized from pokemontcg.io.
 * Uses Caffeine for in-memory caching with stale fallback.
 */
@Service
public class CardCacheService {

    private static final Logger log = LoggerFactory.getLogger(CardCacheService.class);

    private final PokemonTCGApiService apiService;
    private final LoadingCache<String, List<Card>> setCache;

    public CardCacheService(PokemonTCGApiService apiService) {
        this.apiService = apiService;
        // TTL of 24 hours. refreshAfterWrite allows returning stale cache while asynchronously fetching new data.
        this.setCache = Caffeine.newBuilder()
                .refreshAfterWrite(Duration.ofHours(24))
                .build(apiService::fetchSet);
    }

    /**
     * Warms the local card cache at startup so subsequent requests (deck provisioning,
     * card lookups, pokedex) don't fail with a cold cache.
     * If the external API is unreachable, logs a warning and continues — the
     * {@link LoadingCache} will retry on the next {@link #getAllCards()} / {@link #findById(String)} call.
     */
    @PostConstruct
    public void warmCache() {
        log.info("Warming card cache from external API...");
        try {
            List<Card> cards = apiService.fetchSet("xy1");
            setCache.put("xy1", cards);
            log.info("Card cache warmed successfully with {} cards from set 'xy1'", cards.size());
        } catch (Exception e) {
            log.warn("Failed to warm card cache at startup: {}. Will retry on first access.",
                    e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
        }
    }

    /**
     * Searches the local card cache with optional filters and pagination.
     */
    public Page<CardResponse> getCards(String name, String set, String supertype, String type, Pageable pageable) {
        String targetSet = (set != null && !set.isBlank()) ? set : "xy1";
        List<Card> cards;
        try {
            cards = setCache.get(targetSet);
        } catch (RuntimeException e) {
            log.error("Failed to load cards from external API (set={}): {}", targetSet,
                    e.getCause() != null ? e.getCause().getMessage() : e.getMessage(), e);
            throw new RuntimeException("No se pudieron cargar las cartas desde la API externa. Verificá la conexión a internet o intentá más tarde.");
        }
        if (cards == null) {
            cards = List.of();
            }

        Stream<Card> stream = cards.stream();
        if (name != null && !name.isBlank()) {
            stream = stream.filter(c -> c.getName() != null && c.getName().toLowerCase().contains(name.toLowerCase()));
            }
            if (supertype != null && !supertype.isBlank()) {
            stream = stream.filter(c -> c.getSupertype() != null && c.getSupertype().equalsIgnoreCase(supertype));
            }
            if (type != null && !type.isBlank()) {
            stream = stream.filter(c -> c.getTypes() != null && c.getTypes().stream().anyMatch(t -> t.equalsIgnoreCase(type)));
            }

        List<Card> filtered = stream.collect(Collectors.toList());
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());

        List<CardResponse> paged;
        if (start > filtered.size()) {
            paged = List.of();
        } else {
            paged = filtered.subList(start, end).stream().map(this::toCardResponse).collect(Collectors.toList());
        }

        return new PageImpl<>(paged, pageable, filtered.size());
    }

    /**
     * Finds a single card by its API ID from the local cache.
     */
    public CardResponse getCardById(String id) {
        return toCardResponse(findById(id));
    }

    /** Instance UUIDs (e.g. "3f8a2c1b-aaaa-...") must never be treated as card IDs. */
    private static final java.util.regex.Pattern UUID_PATTERN = java.util.regex.Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    /**
     * Resolves a {@link Card} entity by its API ID from the local cache.
     */
    public Card findById(String cardId) {
        // Guard: a board instanceId (UUID) is not a pokemontcg.io card id. Without this,
        // every UUID lookup fired a real HTTP fetch for a nonexistent "set" (the UUID's
        // first segment) before the caller's fallback kicked in.
        if (cardId == null || UUID_PATTERN.matcher(cardId).matches()) {
            throw new IllegalArgumentException("Card not found: " + cardId);
        }
        String setId = cardId.split("-")[0];
        List<Card> cards;
        try {
            cards = setCache.get(setId);
        } catch (RuntimeException e) {
            log.error("Failed to load card from external API (id={}, set={}): {}", cardId, setId,
                    e.getCause() != null ? e.getCause().getMessage() : e.getMessage(), e);
            throw new RuntimeException("No se pudieron cargar las cartas desde la API externa. Verificá la conexión a internet o intentá más tarde.");
        }
        if (cards == null) {
            throw new IllegalArgumentException("Card not found: " + cardId);
        }
        return cards.stream()
                .filter(c -> cardId.equals(c.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Card not found: " + cardId));
    }

    /**
     * Retrieves all cards for MVP initialization logic.
     */
    public List<Card> getAllCards() {
        try {
            return setCache.get("xy1");
        } catch (RuntimeException e) {
            log.error("Failed to load all cards from external API: {}",
                    e.getCause() != null ? e.getCause().getMessage() : e.getMessage(), e);
            throw new RuntimeException("No se pudieron cargar las cartas desde la API externa. Verificá la conexión a internet o intentá más tarde.");
        }
    }

    /**
     * Triggers a full sync of the given set. (Now just a cache refresh).
     */
    public int syncSet(String setId) {
        List<Card> cards;
        try {
            cards = apiService.fetchSet(setId);
        } catch (RuntimeException e) {
            log.error("Failed to sync cards from external API (set={}): {}", setId,
                    e.getCause() != null ? e.getCause().getMessage() : e.getMessage(), e);
            throw new RuntimeException("No se pudieron sincronizar las cartas desde la API externa. Verificá la conexión a internet o intentá más tarde.");
        }
        setCache.put(setId, cards);
        return cards.size();
    }

    private CardResponse toCardResponse(Card card) {
        return CardResponse.builder()
                .id(card.getId())
                .name(card.getName())
                .supertype(card.getSupertype())
                .subtypes(card.getSubtypes())
                .hp(card.getHp())
                .types(card.getTypes())
                .cardSetId(card.getCardSet() != null ? card.getCardSet().getId() : null)
                .cardSetName(card.getCardSet() != null ? card.getCardSet().getName() : null)
                .imageUrlSmall(card.getImageUrlSmall())
                .imageUrlLarge(card.getImageUrlLarge())
                .evolvesFrom(card.getEvolvesFrom())
                .attacks(card.getAttacks())
                .weaknesses(card.getWeaknesses())
                .resistances(card.getResistances())
                .retreatCost(card.getRetreatCost())
                .build();
    }
}
