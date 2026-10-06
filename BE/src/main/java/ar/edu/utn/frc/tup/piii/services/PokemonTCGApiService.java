package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.CardSet;
import ar.edu.utn.frc.tup.piii.external.pokemontcg.dtos.PokemonTCGCardDTO;
import ar.edu.utn.frc.tup.piii.external.pokemontcg.dtos.PokemonTCGWrapperDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calls the pokemontcg.io external API v2 to fetch card data, merges with local parsed effects,
 * and returns the in-memory models.
 */
@Service
public class PokemonTCGApiService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Map<String, Map<String, Object>> parsedEffectsCache;

    public PokemonTCGApiService(
            @Value("${pokemon-tcg.api.base-url}") String baseUrl,
            @Value("${pokemon-tcg.api.key:}") String apiKey,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder) {
        this.objectMapper = objectMapper;
        this.parsedEffectsCache = loadParsedEffects();

        if (apiKey != null && !apiKey.isBlank()) {
            restClientBuilder.defaultHeader("X-Api-Key", apiKey);
        }

        // Use a request factory with longer timeouts — the Pokemon TCG API can be slow
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(60));

        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(factory)
                .build();
    }

    /**
     * Triggers an HTTP call to the external API to fetch the given set ID.
     * Merges parsed effects from xy1_parsed.json if present.
     */
    public List<Card> fetchSet(String setId) {
        PokemonTCGWrapperDTO response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/cards")
                        .queryParam("q", "set.id:" + setId)
                        .build())
                .retrieve()
                .body(PokemonTCGWrapperDTO.class);

        if (response == null || response.getData() == null || response.getData().isEmpty()) {
            return List.of();
        }

        // Determine el CardSet: primera carta con objeto set, o fallback al setId
        CardSet cardSet = null;
        List<PokemonTCGCardDTO> data = response.getData();

        for (PokemonTCGCardDTO dto : data) {
            if (dto.getSet() != null) {
                cardSet = mapSet(dto.getSet());
                break;
            }
        }

        if (cardSet == null) {
            // Fallback: construir CardSet mínimo desde el setId del parámetro
            cardSet = CardSet.builder()
                    .id(setId)
                    .name(setId)
                    .build();
        }

        List<Card> cards = new ArrayList<>();
        for (PokemonTCGCardDTO dto : data) {
            cards.add(toEntity(dto, cardSet));
        }

        // Merge parsed effects from startup cache
        for (Card card : cards) {
            Map<String, Object> effects = parsedEffectsCache.get(card.getId());
            if (effects != null) {
                try {
                    card.setParsedEffects(objectMapper.writeValueAsString(effects));
                } catch (Exception e) {
                    System.err.println("Failed to serialize parsed effects for card " + card.getId());
                }
            }
        }

        return cards;
    }

    private Map<String, Map<String, Object>> loadParsedEffects() {
        Map<String, Map<String, Object>> result = new HashMap<>();
        try (InputStream is = getClass().getResourceAsStream("/data/xy1_parsed.json")) {
            if (is != null) {
                List<Map<String, Object>> parsedCards = objectMapper.readValue(is, new TypeReference<>() {});
                for (Map<String, Object> cardData : parsedCards) {
                    String id = (String) cardData.get("id");
                    if (id != null) {
                        Map<String, Object> effectsPayload = Map.of(
                                "attacks", cardData.getOrDefault("attacks", List.of()),
                                "abilities", cardData.getOrDefault("abilities", List.of()),
                                "trainerEffects", cardData.getOrDefault("trainerEffects", List.of())
                        );
                        result.put(id, effectsPayload);
                    }
                }
            } else {
                System.out.println("[ParsedEffects] No xy1_parsed.json found in resources/data.");
            }
        } catch (Exception e) {
            System.err.println("Failed to load xy1_parsed.json: " + e.getMessage());
        }
        return result;
    }

    private CardSet mapSet(ar.edu.utn.frc.tup.piii.external.pokemontcg.dtos.PokemonTCGSetDTO dto) {
        return CardSet.builder()
                .id(dto.getId())
                .name(dto.getName())
                .series(dto.getSeries())
                .printedTotal(dto.getPrintedTotal())
                .releaseDate(dto.getReleaseDate() != null && !dto.getReleaseDate().isBlank()
                        ? LocalDate.parse(dto.getReleaseDate(), DateTimeFormatter.ofPattern("yyyy/MM/dd")).atStartOfDay()
                        : null)
                .build();
    }

    private String safeToJson(Object obj) {
        if (obj == null) return "[]";
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }

    private Card toEntity(PokemonTCGCardDTO dto, CardSet cardSet) {
        Integer hp = parseHp(dto.getHp());

        return Card.builder()
                .id(dto.getId())
                .name(dto.getName())
                .supertype(dto.getSupertype())
                .subtypes(dto.getSubtypes() != null ? dto.getSubtypes() : List.of())
                .hp(hp)
                .types(dto.getTypes() != null ? dto.getTypes() : List.of())
                .attacks(safeToJson(dto.getAttacks()))
                .weaknesses(safeToJson(dto.getWeaknesses()))
                .resistances(safeToJson(dto.getResistances()))
                .retreatCost(dto.getRetreatCost() != null ? dto.getRetreatCost() : List.of())
                .evolvesFrom(dto.getEvolvesFrom())
                .cardSet(cardSet)
                .imageUrlLarge(dto.getImages() != null ? dto.getImages().getLarge() : null)
                .imageUrlSmall(dto.getImages() != null ? dto.getImages().getSmall() : null)
                .build();
    }

    private Integer parseHp(String hp) {
        if (hp == null || hp.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(hp.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
