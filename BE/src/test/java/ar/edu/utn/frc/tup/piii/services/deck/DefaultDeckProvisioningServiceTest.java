package ar.edu.utn.frc.tup.piii.services.deck;

import ar.edu.utn.frc.tup.piii.dtos.request.CreateDeckRequest;
import ar.edu.utn.frc.tup.piii.dtos.response.DeckResponse;
import ar.edu.utn.frc.tup.piii.entities.Player;
import ar.edu.utn.frc.tup.piii.repositories.DeckRepository;
import ar.edu.utn.frc.tup.piii.services.DeckService;
import ar.edu.utn.frc.tup.piii.services.DefaultDeckProvisioningService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultDeckProvisioningServiceTest {

    @Mock
    private DeckRepository deckRepository;

    @Mock
    private DeckService deckService;

    @Captor
    private ArgumentCaptor<CreateDeckRequest> requestCaptor;

    private DefaultDeckProvisioningService provisioningService;
    private Player player;

    @BeforeEach
    void setUp() {
        provisioningService = new DefaultDeckProvisioningService(deckRepository, deckService);
        player = Player.builder().id(1L).username("testuser").build();
    }

    @Test
    void provisionsSixStarterDecksForNewPlayer() {
        when(deckRepository.findDefaultKeysByPlayerId(1L)).thenReturn(Set.of());
        when(deckService.createDeckForPlayer(any(Player.class), any(CreateDeckRequest.class), anyString()))
                .thenReturn(mock(DeckResponse.class));

        provisioningService.provisionDefaults(player);

        verify(deckService, times(6)).createDeckForPlayer(
                eq(player), any(CreateDeckRequest.class), anyString());
    }

    @Test
    void skipsExistingDefaultKeysOnRerun() {
        when(deckRepository.findDefaultKeysByPlayerId(1L)).thenReturn(
                Set.of("starter-fire", "starter-water", "starter-grass"));

        provisioningService.provisionDefaults(player);

        verify(deckService, times(3)).createDeckForPlayer(
                eq(player), any(CreateDeckRequest.class), anyString());
    }

    @Test
    void skipsAllKeysWhenAllAlreadyExist() {
        when(deckRepository.findDefaultKeysByPlayerId(1L)).thenReturn(
                Set.of("starter-fire", "starter-water", "starter-grass",
                       "starter-lightning", "starter-fighting", "starter-psychic"));

        provisioningService.provisionDefaults(player);

        verify(deckService, never()).createDeckForPlayer(
                any(Player.class), any(CreateDeckRequest.class), anyString());
    }

    @Test
    void createsFuegoDeckWithCorrectTemplate() {
        when(deckRepository.findDefaultKeysByPlayerId(1L)).thenReturn(Set.of());
        when(deckService.createDeckForPlayer(any(Player.class), any(CreateDeckRequest.class), anyString()))
                .thenReturn(mock(DeckResponse.class));

        provisioningService.provisionDefaults(player);

        verify(deckService, atLeastOnce()).createDeckForPlayer(
                eq(player), requestCaptor.capture(), eq("starter-fire"));
        CreateDeckRequest fuegoRequest = requestCaptor.getValue();
        assertEquals("Mazo Fuego Inicial", fuegoRequest.getName());
        assertEquals(60, fuegoRequest.getCards().stream().mapToInt(CreateDeckRequest.CardEntry::getQuantity).sum());
    }

    @Test
    void eachTemplateHasExactly60Cards() {
        when(deckRepository.findDefaultKeysByPlayerId(1L)).thenReturn(Set.of());
        when(deckService.createDeckForPlayer(any(Player.class), any(CreateDeckRequest.class), anyString()))
                .thenReturn(mock(DeckResponse.class));

        provisioningService.provisionDefaults(player);

        verify(deckService, times(6)).createDeckForPlayer(
                eq(player), requestCaptor.capture(), anyString());
        for (CreateDeckRequest req : requestCaptor.getAllValues()) {
            int total = req.getCards().stream().mapToInt(CreateDeckRequest.CardEntry::getQuantity).sum();
            assertEquals(60, total, "Template '" + req.getName() + "' must have exactly 60 cards");
        }
    }

    @Test
    void propagatesExceptionWhenDeckServiceFails() {
        when(deckRepository.findDefaultKeysByPlayerId(1L)).thenReturn(Set.of());
        when(deckService.createDeckForPlayer(any(Player.class), any(CreateDeckRequest.class), anyString()))
                .thenThrow(new RuntimeException("Card API unavailable"));

        assertThrows(RuntimeException.class,
                () -> provisioningService.provisionDefaults(player));
    }

    @Test
    void leavesExistingDecksIntactWhenProvisioningFailsPartway() {
        when(deckRepository.findDefaultKeysByPlayerId(1L)).thenReturn(Set.of());
        when(deckService.createDeckForPlayer(any(Player.class), any(CreateDeckRequest.class), eq("starter-fire")))
                .thenReturn(mock(DeckResponse.class));
        when(deckService.createDeckForPlayer(any(Player.class), any(CreateDeckRequest.class), eq("starter-water")))
                .thenThrow(new RuntimeException("fail"));

        assertThrows(RuntimeException.class,
                () -> provisioningService.provisionDefaults(player));

        verify(deckService, times(1)).createDeckForPlayer(
                eq(player), any(CreateDeckRequest.class), eq("starter-fire"));
        verify(deckService, times(1)).createDeckForPlayer(
                eq(player), any(CreateDeckRequest.class), eq("starter-water"));
    }
}
