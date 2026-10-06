package ar.edu.utn.frc.tup.piii.services.deck;

import ar.edu.utn.frc.tup.piii.dtos.request.CreateDeckRequest;
import ar.edu.utn.frc.tup.piii.dtos.response.DeckResponse;
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
import ar.edu.utn.frc.tup.piii.services.CardCacheService;
import ar.edu.utn.frc.tup.piii.services.DeckService;
import ar.edu.utn.frc.tup.piii.services.deck.validators.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeckServiceTest {

    @Mock
    private DeckRepository deckRepository;
    @Mock
    private CardCacheService cardCacheService;
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private PlayerStatsRepository playerStatsRepository;

    private DeckService deckService;
    private Player player1;
    private Player player2;
    private Card basicPokemon;
    private Card energy;

    @BeforeEach
    void setUp() {
        List<DeckValidator> validators = List.of(
                new ExactSizeValidator(),
                new CopyLimitValidator(),
                new BasicPokemonValidator()
        );
        deckService = new DeckService(deckRepository, playerRepository, playerStatsRepository, cardCacheService, validators);

        player1 = Player.builder().id(1L).username("player1").build();
        player2 = Player.builder().id(2L).username("player2").build();

        basicPokemon = Card.builder()
                .id("xy1-1")
                .name("Venusaur")
                .supertype("Pokémon")
                .subtypes(List.of("Basic"))
                .build();

        energy = Card.builder()
                .id("xy1-2")
                .name("Fire Energy")
                .supertype("Energy")
                .subtypes(List.of("Basic"))
                .build();
    }

    @Test
    void createDeck() {
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player1));
        when(cardCacheService.findById("xy1-1")).thenReturn(basicPokemon);
        when(cardCacheService.findById("xy1-2")).thenReturn(energy);
        when(deckRepository.save(any(Deck.class))).thenAnswer(i -> {
            Deck d = i.getArgument(0);
            d.setId(1L);
            return d;
        });

        List<CreateDeckRequest.CardEntry> entries = new ArrayList<>();
        entries.add(new CreateDeckRequest.CardEntry("xy1-1", 1));
        entries.add(new CreateDeckRequest.CardEntry("xy1-2", 59));

        CreateDeckRequest request = new CreateDeckRequest();
        request.setName("Test Deck");
        request.setCards(entries);

        DeckResponse response = deckService.createDeck(1L, request);

        assertNotNull(response);
        assertEquals("Test Deck", response.getName());
        assertEquals(60, response.getCardCount());
    }

    @Test
    void createDeck_with61Cards_returnsValidationError() {
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player1));
        when(cardCacheService.findById("xy1-1")).thenReturn(basicPokemon);
        when(cardCacheService.findById("xy1-2")).thenReturn(energy);
        when(deckRepository.save(any(Deck.class))).thenAnswer(i -> {
            Deck d = i.getArgument(0);
            d.setId(1L);
            return d;
        });

        List<CreateDeckRequest.CardEntry> entries = new ArrayList<>();
        entries.add(new CreateDeckRequest.CardEntry("xy1-1", 1));
        entries.add(new CreateDeckRequest.CardEntry("xy1-2", 60));

        CreateDeckRequest request = new CreateDeckRequest();
        request.setName("Too Large");
        request.setCards(entries);

        DeckResponse response = deckService.createDeck(1L, request);

        assertFalse(response.isValid());
        assertTrue(response.getValidationErrors().stream()
                .anyMatch(error -> "WRONG_TOTAL".equals(error.getCode())));
    }

    @Test
    void createDeck_withFiveCopiesOfSamePokemon_returnsCopyLimitError() {
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player1));
        when(cardCacheService.findById("xy1-1")).thenReturn(basicPokemon);
        when(cardCacheService.findById("xy1-2")).thenReturn(energy);
        when(deckRepository.save(any(Deck.class))).thenAnswer(i -> {
            Deck d = i.getArgument(0);
            d.setId(1L);
            return d;
        });

        List<CreateDeckRequest.CardEntry> entries = new ArrayList<>();
        entries.add(new CreateDeckRequest.CardEntry("xy1-1", 5));
        entries.add(new CreateDeckRequest.CardEntry("xy1-2", 55));

        CreateDeckRequest request = new CreateDeckRequest();
        request.setName("Too Many Copies");
        request.setCards(entries);

        DeckResponse response = deckService.createDeck(1L, request);

        assertFalse(response.isValid());
        assertTrue(response.getValidationErrors().stream()
                .anyMatch(error -> "TOO_MANY_COPIES".equals(error.getCode())));
    }

    @Test
    void createDeck_withoutBasicPokemon_returnsBasicPokemonError() {
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player1));
        when(cardCacheService.findById("xy1-2")).thenReturn(energy);
        when(deckRepository.save(any(Deck.class))).thenAnswer(i -> {
            Deck d = i.getArgument(0);
            d.setId(1L);
            return d;
        });

        List<CreateDeckRequest.CardEntry> entries = List.of(
                new CreateDeckRequest.CardEntry("xy1-2", 60)
        );

        CreateDeckRequest request = new CreateDeckRequest();
        request.setName("No Basics");
        request.setCards(entries);

        DeckResponse response = deckService.createDeck(1L, request);

        assertFalse(response.isValid());
        assertTrue(response.getValidationErrors().stream()
                .anyMatch(error -> "NO_BASIC_POKEMON".equals(error.getCode())));
    }

    @Test
    void createDeckWithUnknownCard() {
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player1));
        when(cardCacheService.findById("unknown")).thenThrow(new IllegalArgumentException("Card not found"));

        List<CreateDeckRequest.CardEntry> entries = new ArrayList<>();
        entries.add(new CreateDeckRequest.CardEntry("unknown", 1));

        CreateDeckRequest request = new CreateDeckRequest();
        request.setName("Bad Deck");
        request.setCards(entries);

        assertThrows(ResourceNotFoundException.class,
                () -> deckService.createDeck(1L, request));
    }

    @Test
    void userCannotAccessOtherUsersDeck() {
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player1));
        Deck deck = Deck.builder().id(1L).player(player2).cards(new ArrayList<>()).build();
        when(deckRepository.findById(1L)).thenReturn(Optional.of(deck));
        when(deckRepository.findByIdWithCards(1L)).thenReturn(Optional.of(deck));

        assertThrows(ForbiddenException.class,
                () -> deckService.getDeckById(1L, 1L));

        CreateDeckRequest updateRequest = new CreateDeckRequest();
        updateRequest.setName("New Name");
        updateRequest.setCards(new ArrayList<>());
        assertThrows(ForbiddenException.class,
                () -> deckService.updateDeck(1L, 1L, updateRequest));

        assertThrows(ForbiddenException.class,
                () -> deckService.deleteDeck(1L, 1L));

        assertThrows(ForbiddenException.class,
                () -> deckService.validateDeck(1L, 1L));
    }

    @Test
    void deleteDeckSuccess() {
        Deck deck = Deck.builder().id(1L).player(player1).cards(new ArrayList<>()).build();
        when(deckRepository.findById(1L)).thenReturn(Optional.of(deck));

        deckService.deleteDeck(1L, 1L);
        verify(deckRepository).delete(deck);
    }

    @Test
    void validateDeck_usesFindByIdWithCards() {
        Deck deck = Deck.builder().id(1L).player(player1).cards(new ArrayList<>()).build();
        when(deckRepository.findByIdWithCards(1L)).thenReturn(Optional.of(deck));

        DeckValidationResponse response = deckService.validateDeck(1L, 1L);
        assertNotNull(response);
        verify(deckRepository).findByIdWithCards(1L);
        verify(deckRepository, never()).findById(1L);
    }

    @Test
    void validateDeck_recalculatesCardCount() {
        List<DeckCard> cards = new ArrayList<>();
        cards.add(DeckCard.builder().cardId("xy1-1").quantity(2).build());
        cards.add(DeckCard.builder().cardId("xy1-2").quantity(58).build());
        Deck deck = deckWithCards(1L, player1, cards);

        when(deckRepository.findByIdWithCards(1L)).thenReturn(Optional.of(deck));
        when(cardCacheService.findById("xy1-1")).thenReturn(basicPokemon);
        when(cardCacheService.findById("xy1-2")).thenReturn(energy);
        when(deckRepository.save(any(Deck.class))).thenAnswer(i -> i.getArgument(0));

        DeckValidationResponse response = deckService.validateDeck(1L, 1L);

        assertEquals(60, response.getCardCount());
    }

    @Test
    void validateDeck_updatesIsValid() {
        // Only Energy cards → invalid (no Basic Pokémon)
        List<DeckCard> cards = new ArrayList<>();
        cards.add(DeckCard.builder().cardId("xy1-2").quantity(60).build());
        Deck deck = deckWithCards(1L, player1, cards);

        when(deckRepository.findByIdWithCards(1L)).thenReturn(Optional.of(deck));
        when(cardCacheService.findById("xy1-2")).thenReturn(energy);
        when(deckRepository.save(any(Deck.class))).thenAnswer(i -> i.getArgument(0));

        DeckValidationResponse response = deckService.validateDeck(1L, 1L);

        assertFalse(response.isValid());
        // Verify the deck's isValid flag was persisted
        verify(deckRepository).save(argThat(savedDeck -> !savedDeck.isValid()));
    }

    // ── UpdateDeck tests ─────────────────────────────────────────

    private Deck deckWithCards(Long id, Player player, List<DeckCard> cards) {
        Deck deck = Deck.builder().id(id).name("Original").player(player).isValid(true)
                .cards(new ArrayList<>(cards)).build();
        for (DeckCard dc : deck.getCards()) {
            dc.setDeck(deck);
        }
        return deck;
    }

    @Test
    void updateDeck_changesQuantity_preservesDeckCardId() {
        List<DeckCard> existing = new ArrayList<>();
        existing.add(DeckCard.builder().id(1L).cardId("xy1-1").quantity(1).build());
        existing.add(DeckCard.builder().id(2L).cardId("xy1-2").quantity(59).build());
        Deck deck = deckWithCards(1L, player1, existing);

        when(playerRepository.findById(1L)).thenReturn(Optional.of(player1));
        when(deckRepository.findByIdWithCards(1L)).thenReturn(Optional.of(deck));
        when(cardCacheService.findById("xy1-1")).thenReturn(basicPokemon);
        when(cardCacheService.findById("xy1-2")).thenReturn(energy);
        when(deckRepository.save(any(Deck.class))).thenAnswer(i -> i.getArgument(0));

        List<CreateDeckRequest.CardEntry> entries = List.of(
                new CreateDeckRequest.CardEntry("xy1-1", 2),
                new CreateDeckRequest.CardEntry("xy1-2", 58)
        );
        CreateDeckRequest request = new CreateDeckRequest();
        request.setName("Updated");
        request.setCards(entries);

        DeckResponse response = deckService.updateDeck(1L, 1L, request);

        assertEquals("Updated", response.getName());
        assertEquals(60, response.getCardCount());
        assertEquals(2, response.getCards().size());

        // Verify the existing DeckCard ids are preserved (no new inserts)
        assertEquals(1L, deck.getCards().stream()
                .filter(c -> c.getCardId().equals("xy1-1"))
                .findFirst().orElseThrow().getId());
        assertEquals(2L, deck.getCards().stream()
                .filter(c -> c.getCardId().equals("xy1-2"))
                .findFirst().orElseThrow().getId());
        // Quantities updated
        assertEquals(2, deck.getCards().stream()
                .filter(c -> c.getCardId().equals("xy1-1"))
                .findFirst().orElseThrow().getQuantity());
        assertEquals(58, deck.getCards().stream()
                .filter(c -> c.getCardId().equals("xy1-2"))
                .findFirst().orElseThrow().getQuantity());
    }

    @Test
    void updateDeck_removesOldCard_addsNewCard() {
        List<DeckCard> existing = new ArrayList<>();
        existing.add(DeckCard.builder().id(1L).cardId("xy1-1").quantity(1).build());
        existing.add(DeckCard.builder().id(2L).cardId("xy1-2").quantity(59).build());
        Deck deck = deckWithCards(1L, player1, existing);

        when(playerRepository.findById(1L)).thenReturn(Optional.of(player1));
        when(deckRepository.findByIdWithCards(1L)).thenReturn(Optional.of(deck));
        when(cardCacheService.findById("xy1-1")).thenReturn(basicPokemon);
        when(deckRepository.save(any(Deck.class))).thenAnswer(i -> i.getArgument(0));

        // Remove xy1-2 (energy), keep only xy1-1 (basic pokemon)
        List<CreateDeckRequest.CardEntry> entries = List.of(
                new CreateDeckRequest.CardEntry("xy1-1", 60)
        );
        CreateDeckRequest request = new CreateDeckRequest();
        request.setName("Only Venusaur");
        request.setCards(entries);

        DeckResponse response = deckService.updateDeck(1L, 1L, request);

        assertEquals("Only Venusaur", response.getName());
        assertEquals(60, response.getCardCount());
        assertEquals(1, response.getCards().size());
        assertEquals("xy1-1", response.getCards().get(0).getCardId());
        assertEquals(60, response.getCards().get(0).getQuantity());
        // Old card was removed
        assertEquals(1, deck.getCards().size());
    }

    @Test
    void updateDeck_mergesDuplicateRequestEntries() {
        List<DeckCard> existing = new ArrayList<>();
        existing.add(DeckCard.builder().id(1L).cardId("xy1-1").quantity(1).build());
        Deck deck = deckWithCards(1L, player1, existing);

        when(playerRepository.findById(1L)).thenReturn(Optional.of(player1));
        when(deckRepository.findByIdWithCards(1L)).thenReturn(Optional.of(deck));
        when(cardCacheService.findById("xy1-1")).thenReturn(basicPokemon);
        when(deckRepository.save(any(Deck.class))).thenAnswer(i -> i.getArgument(0));

        // Duplicate entries with the same cardId
        List<CreateDeckRequest.CardEntry> entries = List.of(
                new CreateDeckRequest.CardEntry("xy1-1", 2),
                new CreateDeckRequest.CardEntry("xy1-1", 2)
        );
        CreateDeckRequest request = new CreateDeckRequest();
        request.setName("Merge Test");
        request.setCards(entries);

        DeckResponse response = deckService.updateDeck(1L, 1L, request);

        assertEquals(4, response.getCardCount());    // merged: 2+2 = 4
        assertEquals(1, response.getCards().size()); // 1 unique card
        assertEquals(4, response.getCards().get(0).getQuantity());
    }

    @Test
    void updateDeck_recalculatesIsValidAndCardCount() {
        // Invalid deck: only Energy, no Basic Pokémon
        List<DeckCard> existing = new ArrayList<>();
        existing.add(DeckCard.builder().id(1L).cardId("xy1-2").quantity(60).build());
        Deck deck = deckWithCards(1L, player1, existing);

        when(playerRepository.findById(1L)).thenReturn(Optional.of(player1));
        when(deckRepository.findByIdWithCards(1L)).thenReturn(Optional.of(deck));
        when(cardCacheService.findById("xy1-2")).thenReturn(energy);
        when(deckRepository.save(any(Deck.class))).thenAnswer(i -> i.getArgument(0));

        List<CreateDeckRequest.CardEntry> entries = List.of(
                new CreateDeckRequest.CardEntry("xy1-2", 60)
        );
        CreateDeckRequest request = new CreateDeckRequest();
        request.setName("Invalid Deck");
        request.setCards(entries);

        DeckResponse response = deckService.updateDeck(1L, 1L, request);

        assertFalse(response.isValid());
        assertNotNull(response.getValidationErrors());
        assertFalse(response.getValidationErrors().isEmpty());
        assertEquals(60, response.getCardCount());
    }

    @Test
    void updateDeck_sameCardsSameQuantities_isNoOp() {
        List<DeckCard> existing = new ArrayList<>();
        existing.add(DeckCard.builder().id(1L).cardId("xy1-1").quantity(1).build());
        existing.add(DeckCard.builder().id(2L).cardId("xy1-2").quantity(59).build());
        Deck deck = deckWithCards(1L, player1, existing);

        when(playerRepository.findById(1L)).thenReturn(Optional.of(player1));
        when(deckRepository.findByIdWithCards(1L)).thenReturn(Optional.of(deck));
        when(cardCacheService.findById("xy1-1")).thenReturn(basicPokemon);
        when(cardCacheService.findById("xy1-2")).thenReturn(energy);
        when(deckRepository.save(any(Deck.class))).thenAnswer(i -> i.getArgument(0));

        List<CreateDeckRequest.CardEntry> entries = List.of(
                new CreateDeckRequest.CardEntry("xy1-1", 1),
                new CreateDeckRequest.CardEntry("xy1-2", 59)
        );
        CreateDeckRequest request = new CreateDeckRequest();
        request.setName("Same");
        request.setCards(entries);

        DeckResponse response = deckService.updateDeck(1L, 1L, request);

        assertEquals(60, response.getCardCount());
        assertEquals(2, response.getCards().size());
        // DeckCard ids unchanged
        assertEquals(1L, deck.getCards().stream()
                .filter(c -> c.getCardId().equals("xy1-1"))
                .findFirst().orElseThrow().getId());
        assertEquals(2L, deck.getCards().stream()
                .filter(c -> c.getCardId().equals("xy1-2"))
                .findFirst().orElseThrow().getId());
        // Same quantities
        assertEquals(1, deck.getCards().stream()
                .filter(c -> c.getCardId().equals("xy1-1"))
                .findFirst().orElseThrow().getQuantity());
    }

    @Test
    void getDecksByPlayer() {
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player1));
        when(deckRepository.findByPlayerIdWithCards(1L)).thenReturn(List.of(
                Deck.builder().id(1L).name("Deck 1").player(player1).cards(new ArrayList<>()).build(),
                Deck.builder().id(2L).name("Deck 2").player(player1).cards(new ArrayList<>()).build()
        ));

        List<DeckResponse> decks = deckService.getDecksByPlayer(1L);
        assertEquals(2, decks.size());
    }

    // ── createDeckForPlayer tests ─────────────────────────────────

    @Test
    void createDeckForPlayer_persistsDefaultKey() {
        when(cardCacheService.findById("xy1-1")).thenReturn(basicPokemon);
        when(cardCacheService.findById("xy1-2")).thenReturn(energy);
        when(deckRepository.save(any(Deck.class))).thenAnswer(i -> {
            Deck d = i.getArgument(0);
            d.setId(1L);
            return d;
        });

        List<CreateDeckRequest.CardEntry> entries = List.of(
                new CreateDeckRequest.CardEntry("xy1-1", 1),
                new CreateDeckRequest.CardEntry("xy1-2", 59)
        );
        CreateDeckRequest request = new CreateDeckRequest();
        request.setName("Starter Fire");
        request.setCards(entries);

        DeckResponse response = deckService.createDeckForPlayer(player1, request, "starter-fire");

        assertNotNull(response);
        assertEquals("Starter Fire", response.getName());
        assertEquals(60, response.getCardCount());

        verify(deckRepository).save(argThat(savedDeck ->
                "starter-fire".equals(savedDeck.getDefaultKey())
        ));
    }

    @Test
    void createDeckForPlayer_nullDefaultKey_doesNotSetKey() {
        when(cardCacheService.findById("xy1-1")).thenReturn(basicPokemon);
        when(cardCacheService.findById("xy1-2")).thenReturn(energy);
        when(deckRepository.save(any(Deck.class))).thenAnswer(i -> {
            Deck d = i.getArgument(0);
            d.setId(1L);
            return d;
        });

        List<CreateDeckRequest.CardEntry> entries = List.of(
                new CreateDeckRequest.CardEntry("xy1-1", 1),
                new CreateDeckRequest.CardEntry("xy1-2", 59)
        );
        CreateDeckRequest request = new CreateDeckRequest();
        request.setName("No Key Deck");
        request.setCards(entries);

        DeckResponse response = deckService.createDeckForPlayer(player1, request, null);

        assertNotNull(response);
        verify(deckRepository).save(argThat(savedDeck ->
                savedDeck.getDefaultKey() == null
        ));
    }

    @Test
    void createDeckForPlayer_invalidDeck_stillReturnsWithErrors() {
        when(cardCacheService.findById("xy1-2")).thenReturn(energy);
        when(deckRepository.save(any(Deck.class))).thenAnswer(i -> {
            Deck d = i.getArgument(0);
            d.setId(1L);
            return d;
        });

        // Only Energy — no Basic Pokémon → invalid
        List<CreateDeckRequest.CardEntry> entries = List.of(
                new CreateDeckRequest.CardEntry("xy1-2", 60)
        );
        CreateDeckRequest request = new CreateDeckRequest();
        request.setName("Invalid Default");
        request.setCards(entries);

        DeckResponse response = deckService.createDeckForPlayer(player1, request, "starter-bad");

        assertNotNull(response);
        assertFalse(response.isValid());
        assertNotNull(response.getValidationErrors());
        assertFalse(response.getValidationErrors().isEmpty());
        // Default key should still be set even if invalid
        verify(deckRepository).save(argThat(savedDeck ->
                "starter-bad".equals(savedDeck.getDefaultKey())
        ));
    }
}
