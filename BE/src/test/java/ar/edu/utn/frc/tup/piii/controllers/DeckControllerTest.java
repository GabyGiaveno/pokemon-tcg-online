package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.request.CreateDeckRequest;
import ar.edu.utn.frc.tup.piii.dtos.response.DeckResponse;
import ar.edu.utn.frc.tup.piii.dtos.response.DeckValidationResponse;
import ar.edu.utn.frc.tup.piii.exceptions.GlobalExceptionHandler;
import ar.edu.utn.frc.tup.piii.exceptions.ForbiddenException;
import ar.edu.utn.frc.tup.piii.exceptions.ResourceNotFoundException;
import ar.edu.utn.frc.tup.piii.security.JwtAuthenticationFilter;
import ar.edu.utn.frc.tup.piii.security.CurrentPlayerService;
import ar.edu.utn.frc.tup.piii.security.JwtService;
import ar.edu.utn.frc.tup.piii.services.DeckService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DeckController.class,
        excludeFilters = @ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DeckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeckService deckService;

    @MockitoBean
    private CurrentPlayerService currentPlayerService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void getDecks_returnsDeckList() throws Exception {
        when(currentPlayerService.getCurrentPlayerId()).thenReturn(7L);
        when(deckService.getDecksByPlayer(7L)).thenReturn(List.of(deckResponse(1L, "Starter")));

        mockMvc.perform(get("/api/decks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Starter"));
    }

    @Test
    void getDeckById_returnsDeck() throws Exception {
        when(currentPlayerService.getCurrentPlayerId()).thenReturn(7L);
        when(deckService.getDeckById(7L, 1L)).thenReturn(deckResponse(1L, "Starter"));

        mockMvc.perform(get("/api/decks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Starter"));
    }

    @Test
    void getDeckById_whenMissing_returns404() throws Exception {
        when(currentPlayerService.getCurrentPlayerId()).thenReturn(7L);
        when(deckService.getDeckById(7L, 999L)).thenThrow(new ResourceNotFoundException("Deck not found: 999"));

        mockMvc.perform(get("/api/decks/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void createDeck_returnsCreated() throws Exception {
        when(currentPlayerService.getCurrentPlayerId()).thenReturn(7L);
        when(deckService.createDeck(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.any(CreateDeckRequest.class)))
                .thenReturn(deckResponse(10L, "Mi mazo"));

        mockMvc.perform(post("/api/decks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mi mazo\",\"cards\":[{\"cardId\":\"xy1-1\",\"quantity\":4}]}") )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Mi mazo"));

        verify(deckService).createDeck(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.any(CreateDeckRequest.class));
    }

    @Test
    void createDeck_withInvalidBody_returns400() throws Exception {
        when(currentPlayerService.getCurrentPlayerId()).thenReturn(7L);

        String body = """
                {"name":"","cards":[]}
                """;

        mockMvc.perform(post("/api/decks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void validateDeck_returnsValidationSummary() throws Exception {
        when(currentPlayerService.getCurrentPlayerId()).thenReturn(7L);
        when(deckService.validateDeck(7L, 1L)).thenReturn(DeckValidationResponse.builder()
                .valid(true)
                .errors(List.of())
                .cardCount(60)
                .build());

        mockMvc.perform(post("/api/decks/1/validate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.cardCount").value(60));
    }

    @Test
    void deleteDeck_returnsNoContent() throws Exception {
        when(currentPlayerService.getCurrentPlayerId()).thenReturn(7L);

        mockMvc.perform(delete("/api/decks/1"))
                .andExpect(status().isNoContent());

        verify(deckService).deleteDeck(7L, 1L);
    }

    @Test
    void deleteDeck_whenForbidden_returns403() throws Exception {
        when(currentPlayerService.getCurrentPlayerId()).thenReturn(7L);
        org.mockito.Mockito.doThrow(new ForbiddenException("Deck 1 does not belong to player 7"))
                .when(deckService).deleteDeck(7L, 1L);

        mockMvc.perform(delete("/api/decks/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void updateDeck_returnsUpdatedDeck() throws Exception {
        when(currentPlayerService.getCurrentPlayerId()).thenReturn(7L);
        when(deckService.updateDeck(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any(CreateDeckRequest.class)))
                .thenReturn(deckResponse(1L, "Actualizado"));

        mockMvc.perform(put("/api/decks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Actualizado\",\"cards\":[{\"cardId\":\"xy1-1\",\"quantity\":4}]}") )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Actualizado"));
    }

    private DeckResponse deckResponse(Long id, String name) {
        return DeckResponse.builder()
                .id(id)
                .name(name)
                .valid(true)
                .cardCount(60)
                .cards(List.of())
                .validationErrors(List.of())
                .createdAt(LocalDateTime.of(2026, 6, 24, 18, 0, 0))
                .build();
    }
}
