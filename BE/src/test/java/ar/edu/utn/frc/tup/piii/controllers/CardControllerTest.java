package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.response.CardResponse;
import ar.edu.utn.frc.tup.piii.exceptions.GlobalExceptionHandler;
import ar.edu.utn.frc.tup.piii.security.JwtAuthenticationFilter;
import ar.edu.utn.frc.tup.piii.security.JwtService;
import ar.edu.utn.frc.tup.piii.services.CardCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.config.SpringDataWebConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CardController.class,
        excludeFilters = @ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, SpringDataWebConfiguration.class})
class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CardCacheService cardCacheService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void getCards_returnsPaginatedCards() throws Exception {
        CardResponse card = CardResponse.builder()
                .id("xy1-1")
                .name("Venusaur-EX")
                .supertype("Pokémon")
                .types(List.of("Grass"))
                .build();

        when(cardCacheService.getCards(null, null, null, null, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(card), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/cards?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.data[0].id").value("xy1-1"))
                .andExpect(jsonPath("$.data[0].name").value("Venusaur-EX"));
    }

    @Test
    void getCards_filtersByName() throws Exception {
        when(cardCacheService.getCards("Venusaur", null, null, null, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/api/cards?name=Venusaur&page=0&size=10"))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(cardCacheService).getCards("Venusaur", null, null, null, PageRequest.of(0, 10));
    }

    @Test
    void getCards_filtersByType() throws Exception {
        when(cardCacheService.getCards(null, null, null, "Fire", PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/api/cards?type=Fire&page=0&size=10"))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(cardCacheService).getCards(null, null, null, "Fire", PageRequest.of(0, 10));
    }

    @Test
    void getCards_filtersBySetAndSupertype() throws Exception {
        when(cardCacheService.getCards(null, "xy1", "Pokémon", null, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/api/cards?set=xy1&supertype=Pokémon&page=0&size=10"))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(cardCacheService).getCards(null, "xy1", "Pokémon", null, PageRequest.of(0, 10));
    }

    @Test
    void getCardById_returnsCard() throws Exception {
        CardResponse card = CardResponse.builder()
                .id("xy1-1")
                .name("Venusaur-EX")
                .supertype("Pokémon")
                .types(List.of("Grass"))
                .build();

        when(cardCacheService.getCardById("xy1-1")).thenReturn(card);

        mockMvc.perform(get("/api/cards/xy1-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("xy1-1"))
                .andExpect(jsonPath("$.name").value("Venusaur-EX"));
    }

    @Test
    void getCardById_whenMissing_returns400() throws Exception {
        when(cardCacheService.getCardById("xyz-999"))
                .thenThrow(new IllegalArgumentException("Card not found: xyz-999"));

        mockMvc.perform(get("/api/cards/xyz-999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void syncSet_returnsSyncResponse() throws Exception {
        when(cardCacheService.syncSet("xy1")).thenReturn(146);

        mockMvc.perform(post("/api/cards/sync?set=xy1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Sync completed"))
                .andExpect(jsonPath("$.set").value("xy1"))
                .andExpect(jsonPath("$.cardsImported").value(146));
    }
}
