package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.request.CreateDeckRequest;
import ar.edu.utn.frc.tup.piii.dtos.response.DeckCardResponse;
import ar.edu.utn.frc.tup.piii.dtos.response.DeckResponse;
import ar.edu.utn.frc.tup.piii.dtos.response.DeckValidationError;
import ar.edu.utn.frc.tup.piii.dtos.response.DeckValidationResponse;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.Deck;
import ar.edu.utn.frc.tup.piii.entities.DeckCard;
import ar.edu.utn.frc.tup.piii.entities.Player;
import ar.edu.utn.frc.tup.piii.exceptions.ForbiddenException;
import ar.edu.utn.frc.tup.piii.exceptions.ResourceNotFoundException;
import ar.edu.utn.frc.tup.piii.repositories.DeckRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerRepository;
import ar.edu.utn.frc.tup.piii.repositories.PlayerStatsRepository;
import ar.edu.utn.frc.tup.piii.services.deck.validators.DeckValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DeckService {

    private static final Logger log = LoggerFactory.getLogger(DeckService.class);

    private final DeckRepository deckRepository;
    private final PlayerRepository playerRepository;
    private final PlayerStatsRepository playerStatsRepository;
    private final CardCacheService cardCacheService;
    private final List<DeckValidator> validators;

    public DeckService(DeckRepository deckRepository, PlayerRepository playerRepository,
                       PlayerStatsRepository playerStatsRepository,
                       CardCacheService cardCacheService, List<DeckValidator> validators) {
        this.deckRepository = deckRepository;
        this.playerRepository = playerRepository;
        this.playerStatsRepository = playerStatsRepository;
        this.cardCacheService = cardCacheService;
        this.validators = validators;
    }

    /**
     * Runs all validation rules against a proposed list of cards for a deck.
     * @param deckCards The list of cards (expanded by quantity).
     * @return A list of errors, empty if the deck is perfectly valid.
     */
    public List<DeckValidationError> validateDeck(List<Card> deckCards) {
        return validators.stream()
                .flatMap(validator -> validator.validate(deckCards).stream())
                .toList();
    }

    /**
     * Returns all decks belonging to the given player.
     */
    @Transactional(readOnly = true)
    public List<DeckResponse> getDecksByPlayer(Long playerId) {
        playerRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + playerId));

        return deckRepository.findByPlayerIdWithCards(playerId).stream()
                .map(this::toDeckResponse)
                .collect(Collectors.toList());
    }

    /**
     * Returns a single deck by ID, enforcing player ownership.
     */
    @Transactional(readOnly = true)
    public DeckResponse getDeckById(Long playerId, Long deckId) {
        Deck deck = deckRepository.findByIdWithCards(deckId)
                .orElseThrow(() -> new ResourceNotFoundException("Deck not found: " + deckId));

        enforceOwnership(deck, playerId);
        return toDeckResponse(deck);
    }

    /**
     * Creates a new deck for the player, validates it, and persists.
     */
    @Transactional
    public DeckResponse createDeck(Long playerId, CreateDeckRequest request) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + playerId));
        return createDeckForPlayer(player, request, null);
    }

    /**
     * Creates a new deck for an already-loaded Player, optionally tagging it with a
     * default-key marker for idempotent provisioning. Reuses the same validation,
     * merge, and expansion logic as {@link #createDeck(Long, CreateDeckRequest)}.
     *
     * @param player     the owning Player (must already be persisted)
     * @param request    the deck creation request with name and card entries
     * @param defaultKey nullable stable key for default/starter decks, e.g. "starter-fire"
     * @return the persisted DeckResponse, including validation errors if any
     */
    @Transactional
    public DeckResponse createDeckForPlayer(Player player, CreateDeckRequest request, String defaultKey) {
        List<CreateDeckRequest.CardEntry> mergedEntries = mergeCardEntries(request.getCards());
        Map<String, Card> cardMap = loadCardMap(mergedEntries);
        List<Card> expanded = expandForValidation(mergedEntries, cardMap);
        List<DeckValidationError> errors = validateDeck(expanded);
        boolean isValid = errors.isEmpty();

        Deck deck = Deck.builder()
                .name(request.getName())
                .player(player)
                .defaultKey(defaultKey)
                .isValid(isValid)
                .createdAt(LocalDateTime.now())
                .cards(new ArrayList<>())
                .build();

        for (CreateDeckRequest.CardEntry entry : mergedEntries) {
            DeckCard deckCard = DeckCard.builder()
                    .deck(deck)
                    .cardId(entry.getCardId())
                    .quantity(entry.getQuantity())
                    .build();
            deck.getCards().add(deckCard);
        }

        Deck savedDeck = deckRepository.save(deck);

        if (defaultKey == null) {
            playerStatsRepository.findByPlayerId(player.getId()).ifPresent(stats -> {
                stats.setDecksCreated(stats.getDecksCreated() + 1);
                playerStatsRepository.save(stats);
            });
        }

        DeckResponse response = toDeckResponse(savedDeck);
        response.setValidationErrors(errors);
        return response;
    }

    /**
     * Updates an existing deck: replaces name and cards, re-validates.
     */
    @Transactional
    public DeckResponse updateDeck(Long playerId, Long deckId, CreateDeckRequest request) {
        playerRepository.findById(playerId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + playerId));

        Deck deck = deckRepository.findByIdWithCards(deckId)
                .orElseThrow(() -> new ResourceNotFoundException("Deck not found: " + deckId));

        enforceOwnership(deck, playerId);

        List<CreateDeckRequest.CardEntry> mergedEntries = mergeCardEntries(request.getCards());
        Map<String, Card> cardMap = loadCardMap(mergedEntries);
        List<Card> expanded = expandForValidation(mergedEntries, cardMap);
        List<DeckValidationError> errors = validateDeck(expanded);
        boolean isValid = errors.isEmpty();

        deck.setName(request.getName());
        deck.setValid(isValid);
        replaceDeckCards(deck, mergedEntries);

        Deck savedDeck = deckRepository.save(deck);
        DeckResponse response = toDeckResponse(savedDeck);
        response.setValidationErrors(errors);
        return response;
    }

    /**
     * Deletes a deck by ID, enforcing player ownership.
     */
    @Transactional
    public void deleteDeck(Long playerId, Long deckId) {
        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new ResourceNotFoundException("Deck not found: " + deckId));

        enforceOwnership(deck, playerId);
        deckRepository.delete(deck);
    }

    /**
     * Validates an existing persisted deck and returns the validation result.
     * Persists the validity flag on the deck entity.
     */
    @Transactional
    public DeckValidationResponse validateDeck(Long playerId, Long deckId) {
        Deck deck = deckRepository.findByIdWithCards(deckId)
                .orElseThrow(() -> new ResourceNotFoundException("Deck not found: " + deckId));

        enforceOwnership(deck, playerId);

        List<Card> expanded = new ArrayList<>();
        int totalCards = 0;
        for (DeckCard dc : deck.getCards()) {
            Card card = cardCacheService.findById(dc.getCardId());
            for (int i = 0; i < dc.getQuantity(); i++) {
                expanded.add(card);
            }
            totalCards += dc.getQuantity();
        }

        List<DeckValidationError> errors = validateDeck(expanded);
        boolean valid = errors.isEmpty();

        deck.setValid(valid);
        deckRepository.save(deck);

        return DeckValidationResponse.builder()
                .valid(valid)
                .errors(errors)
                .cardCount(totalCards)
                .build();
    }

    // ===================== Private helpers =====================

    private void enforceOwnership(Deck deck, Long playerId) {
        if (!deck.getPlayer().getId().equals(playerId)) {
            throw new ForbiddenException(
                    "Deck " + deck.getId() + " does not belong to player " + playerId);
        }
    }

    /**
     * Normalizes the card list by merging entries with the same cardId,
     * summing their quantities. This prevents unique constraint violations
     * on (deck_id, card_id) and ensures one DeckCard row per distinct card.
     */
    private List<CreateDeckRequest.CardEntry> mergeCardEntries(List<CreateDeckRequest.CardEntry> entries) {
        return entries.stream()
                .collect(Collectors.groupingBy(
                        CreateDeckRequest.CardEntry::getCardId,
                        Collectors.summingInt(CreateDeckRequest.CardEntry::getQuantity)
                ))
                .entrySet().stream()
                .map(e -> new CreateDeckRequest.CardEntry(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Loads all Card entities referenced in the request entries and returns a
     * cardId-to-Card map. Throws ResourceNotFoundException if any card ID is not found.
     */
    private Map<String, Card> loadCardMap(List<CreateDeckRequest.CardEntry> entries) {
        List<String> missingIds = new ArrayList<>();
        List<Card> cards = new ArrayList<>();

        for (CreateDeckRequest.CardEntry entry : entries) {
            try {
                Card card = cardCacheService.findById(entry.getCardId());
                cards.add(card);
            } catch (IllegalArgumentException e) {
                missingIds.add(entry.getCardId());
            } catch (RuntimeException e) {
                log.warn("Failed to load card {} from cache: {}", entry.getCardId(), e.getMessage());
                missingIds.add(entry.getCardId());
            }
        }

        if (!missingIds.isEmpty()) {
            throw new ResourceNotFoundException("Cards not found: " + missingIds);
        }

        return cards.stream().distinct().collect(Collectors.toMap(Card::getId, Function.identity()));
    }

    /**
     * Expands CardEntry list into a flat List of Card instances, repeating each
     * card according to its quantity. This is essential because validators
     * operate on a flat List<Card> and expect each copy to appear individually.
     */
    private List<Card> expandForValidation(
            List<CreateDeckRequest.CardEntry> entries, Map<String, Card> cardMap) {
        List<Card> expanded = new ArrayList<>();
        for (CreateDeckRequest.CardEntry entry : entries) {
            Card card = cardMap.get(entry.getCardId());
            for (int i = 0; i < entry.getQuantity(); i++) {
                expanded.add(card);
            }
        }
        return expanded;
    }

    /**
     * Replaces the deck's cards with the entries from the request,
     * preserving existing DeckCard DB ids when possible.
     * <p>
     * Avoids the unique constraint violation caused by {@code clear()} + mass re-insert
     * because Hibernate can flush new inserts before physically deleting old rows when
     * the {@link DeckCard} collection uses {@code orphanRemoval=true} and no identifier
     * is assigned to the new instances (IDs from the DB).
     * </p>
     */
    private void replaceDeckCards(Deck deck, List<CreateDeckRequest.CardEntry> entries) {
        Map<String, DeckCard> existingByCardId = deck.getCards().stream()
                .collect(Collectors.toMap(DeckCard::getCardId, Function.identity()));

        Set<String> requestedCardIds = entries.stream()
                .map(CreateDeckRequest.CardEntry::getCardId)
                .collect(Collectors.toSet());

        // Remove cards that are no longer in the request — orphanRemoval=true handles DB deletion
        deck.getCards().removeIf(dc -> !requestedCardIds.contains(dc.getCardId()));

        // Update existing cards' quantities or add new cards
        for (CreateDeckRequest.CardEntry entry : entries) {
            DeckCard existing = existingByCardId.get(entry.getCardId());
            if (existing != null) {
                existing.setQuantity(entry.getQuantity());
            } else {
                DeckCard newCard = DeckCard.builder()
                        .deck(deck)
                        .cardId(entry.getCardId())
                        .quantity(entry.getQuantity())
                        .build();
                deck.getCards().add(newCard);
            }
        }
    }

    private DeckResponse toDeckResponse(Deck deck) {
        List<DeckCardResponse> cardResponses = deck.getCards().stream()
                .map(this::toDeckCardResponse)
                .collect(Collectors.toList());

        int totalCards = deck.getCards().stream()
                .mapToInt(DeckCard::getQuantity)
                .sum();

        return DeckResponse.builder()
                .id(deck.getId())
                .name(deck.getName())
                .valid(deck.isValid())
                .cardCount(totalCards)
                .cards(cardResponses)
                .createdAt(deck.getCreatedAt())
                .build();
    }

    private DeckCardResponse toDeckCardResponse(DeckCard deckCard) {
        Card card = cardCacheService.findById(deckCard.getCardId());
        return DeckCardResponse.builder()
                .cardId(card.getId())
                .cardName(card.getName())
                .quantity(deckCard.getQuantity())
                .supertype(card.getSupertype())
                .types(card.getTypes())
                .subtypes(card.getSubtypes())
                .imageUrlSmall(card.getImageUrlSmall())
                .build();
    }
}
