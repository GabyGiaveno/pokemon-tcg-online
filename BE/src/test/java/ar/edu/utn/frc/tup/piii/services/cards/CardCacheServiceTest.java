package ar.edu.utn.frc.tup.piii.services.cards;

import ar.edu.utn.frc.tup.piii.dtos.response.CardResponse;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.entities.CardSet;
import ar.edu.utn.frc.tup.piii.services.CardCacheService;
import ar.edu.utn.frc.tup.piii.services.PokemonTCGApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardCacheServiceTest {

    @Mock
    private PokemonTCGApiService apiService;

    private CardCacheService cardCacheService;
    private Card card1;
    private Card card2;
    private CardSet set;

    @Test
    void findById_instanceUuid_failsFastWithoutHittingExternalApi() {
        // tasks 5.8: a board instanceId must not trigger an HTTP fetch for a junk "set"
        assertThrows(IllegalArgumentException.class,
                () -> cardCacheService.findById("3f8a2c1b-aaaa-bbbb-cccc-1234567890ab"));
        verifyNoInteractions(apiService);
    }

    @BeforeEach
    void setUp() {
        cardCacheService = new CardCacheService(apiService);

        set = CardSet.builder().id("xy1").name("XY Base Set").build();

        card1 = Card.builder()
                .id("xy1-1")
                .name("Pikachu")
                .supertype("Pokémon")
                .types(List.of("Lightning"))
                .cardSet(set)
                .build();

        card2 = Card.builder()
                .id("xy1-2")
                .name("Charmander")
                .supertype("Pokémon")
                .types(List.of("Fire"))
                .cardSet(set)
                .build();
    }

    @Test
    void getCardById() {
        when(apiService.fetchSet("xy1")).thenReturn(List.of(card1, card2));

        CardResponse response = cardCacheService.getCardById("xy1-1");

        assertNotNull(response);
        assertEquals("Pikachu", response.getName());
    }

    @Test
    void getCardByIdNotFound() {
        when(apiService.fetchSet("nonexistent")).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class,
                () -> cardCacheService.getCardById("nonexistent"));
    }

    @Test
    void getCardsWithPagination() {
        when(apiService.fetchSet("xy1")).thenReturn(List.of(card1, card2));

        Page<CardResponse> result = cardCacheService.getCards(null, null, null, null, Pageable.ofSize(10));

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
    }

    @Test
    void filterByName() {
        when(apiService.fetchSet("xy1")).thenReturn(List.of(card1, card2));

        Page<CardResponse> result = cardCacheService.getCards("Pikachu", null, null, null, Pageable.ofSize(10));

        assertEquals(1, result.getTotalElements());
        assertEquals("Pikachu", result.getContent().get(0).getName());
    }

    @Test
    void getCards_withCombinedFilters_returnsOnlyMatchingCard() {
        Card trainer = Card.builder()
                .id("xy1-3")
                .name("Potion")
                .supertype("Trainer")
                .cardSet(set)
                .build();
        when(apiService.fetchSet("xy1")).thenReturn(List.of(card1, card2, trainer));

        Page<CardResponse> result = cardCacheService.getCards("char", "", "Pokémon", "Fire", Pageable.ofSize(10));

        assertEquals(1, result.getTotalElements());
        assertEquals("Charmander", result.getContent().get(0).getName());
    }

    @Test
    void warmCache_whenApiFails_doesNotThrow() {
        when(apiService.fetchSet("xy1")).thenThrow(new RuntimeException("boom"));

        assertDoesNotThrow(() -> cardCacheService.warmCache());
        verify(apiService).fetchSet("xy1");
    }

    @Test
    void syncDelegatesToApiService() {
        when(apiService.fetchSet("xy1")).thenReturn(List.of(card1, card2));

        int count = cardCacheService.syncSet("xy1");

        assertEquals(2, count);
        verify(apiService).fetchSet("xy1");
    }
}
