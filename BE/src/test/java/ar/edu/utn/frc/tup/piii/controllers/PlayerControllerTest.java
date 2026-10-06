package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.request.EquipSkinRequest;
import ar.edu.utn.frc.tup.piii.dtos.request.UpdateProfileRequest;
import ar.edu.utn.frc.tup.piii.dtos.response.AchievementResponse;
import ar.edu.utn.frc.tup.piii.dtos.response.BadgeResponse;
import ar.edu.utn.frc.tup.piii.dtos.response.CustomizationItemResponse;
import ar.edu.utn.frc.tup.piii.dtos.response.ProfileResponse;
import ar.edu.utn.frc.tup.piii.dtos.response.SkinResponse;
import ar.edu.utn.frc.tup.piii.exceptions.GlobalExceptionHandler;
import ar.edu.utn.frc.tup.piii.exceptions.ResourceNotFoundException;
import ar.edu.utn.frc.tup.piii.security.JwtAuthenticationFilter;
import ar.edu.utn.frc.tup.piii.security.CurrentPlayerService;
import ar.edu.utn.frc.tup.piii.security.JwtService;
import ar.edu.utn.frc.tup.piii.services.PlayerService;
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

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PlayerController.class,
        excludeFilters = @ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PlayerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlayerService playerService;

    @MockitoBean
    private CurrentPlayerService currentPlayerService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void getMyProfile_returnsProfile() throws Exception {
        when(currentPlayerService.getCurrentPlayerId()).thenReturn(7L);
        when(playerService.getProfile(7L)).thenReturn(profileResponse());

        mockMvc.perform(get("/api/players/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.username").value("ash"))
                .andExpect(jsonPath("$.level").value(12));
    }

    @Test
    void getMyProfile_whenMissing_returns404() throws Exception {
        when(currentPlayerService.getCurrentPlayerId()).thenReturn(7L);
        when(playerService.getProfile(7L)).thenThrow(new ResourceNotFoundException("Player not found: 7"));

        mockMvc.perform(get("/api/players/me"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Player not found: 7"));
    }

    @Test
    void equipSkin_callsServiceAndReturnsOk() throws Exception {
        when(currentPlayerService.getCurrentPlayerId()).thenReturn(7L);
        doNothing().when(playerService).equipSkin(7L, "fire");

        mockMvc.perform(put("/api/players/me/skin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skinId\":\"fire\"}"))
                .andExpect(status().isOk());

        verify(playerService).equipSkin(7L, "fire");
    }

    @Test
    void updateProfile_returnsUpdatedProfile() throws Exception {
        when(currentPlayerService.getCurrentPlayerId()).thenReturn(7L);
        when(playerService.updateProfile(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.any(UpdateProfileRequest.class)))
                .thenReturn(profileResponse());

        mockMvc.perform(put("/api/players/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bio\":\"Nuevo bio\",\"favoriteRegion\":\"Johto\",\"favoritePokemon\":\"Chikorita\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("Gotta catch em all"))
                .andExpect(jsonPath("$.favoriteRegion").value("Kanto"));

        verify(playerService).updateProfile(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.any(UpdateProfileRequest.class));
    }

    @Test
    void getAchievements_returnsList() throws Exception {
        when(currentPlayerService.getCurrentPlayerId()).thenReturn(7L);
        when(playerService.getAchievements(7L)).thenReturn(List.of(achievementResponse()));

        mockMvc.perform(get("/api/players/me/achievements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("first-win"))
                .andExpect(jsonPath("$[0].unlocked").value(true));
    }

    @Test
    void getBadges_returnsList() throws Exception {
        when(currentPlayerService.getCurrentPlayerId()).thenReturn(7L);
        when(playerService.getBadges(7L)).thenReturn(List.of(badgeResponse()));

        mockMvc.perform(get("/api/players/me/badges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("first-badge"))
                .andExpect(jsonPath("$[0].unlocked").value(true));
    }

    @Test
    void getSkins_returnsList() throws Exception {
        when(currentPlayerService.getCurrentPlayerId()).thenReturn(7L);
        when(playerService.getSkins(7L)).thenReturn(List.of(skinResponse()));

        mockMvc.perform(get("/api/players/me/skins"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("fire"))
                .andExpect(jsonPath("$[0].equipped").value(true));
    }

    @Test
    void getCustomizationItems_returnsList() throws Exception {
        when(currentPlayerService.getCurrentPlayerId()).thenReturn(7L);
        when(playerService.getCustomizationItems(7L)).thenReturn(List.of(customizationItemResponse()));

        mockMvc.perform(get("/api/players/me/customization"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("hat-01"))
                .andExpect(jsonPath("$[0].unlocked").value(false));
    }

    private ProfileResponse profileResponse() {
        ProfileResponse response = new ProfileResponse();
        response.setId(7L);
        response.setUsername("ash");
        response.setEmail("ash@example.com");
        response.setCreatedAt(LocalDateTime.of(2026, 6, 24, 18, 0, 0));
        response.setDecksCount(3);
        response.setXpPercent(50);
        response.setBio("Gotta catch em all");
        response.setLevel(12);
        response.setFavoriteRegion("Kanto");
        response.setFavoritePokemon("Pikachu");
        response.setTotalCards(250);
        response.setWins(18);
        response.setLosses(4);
        response.setStreak(5);
        response.setTournamentsWon(1);
        response.setPacksOpened(12);
        response.setDecksCreated(3);
        return response;
    }

    private AchievementResponse achievementResponse() {
        AchievementResponse response = new AchievementResponse();
        response.setId("first-win");
        response.setName("Campeón Novato");
        response.setDescription("Obtén tu primera victoria.");
        response.setIcon("🏆");
        response.setUnlocked(true);
        return response;
    }

    private BadgeResponse badgeResponse() {
        BadgeResponse response = new BadgeResponse();
        response.setId("first-badge");
        response.setLabel("Primera Insignia");
        response.setIcon("⭐");
        response.setUnlocked(true);
        response.setDescription("Badge de prueba.");
        response.setHowToUnlock("Jugar una partida.");
        return response;
    }

    private SkinResponse skinResponse() {
        SkinResponse response = new SkinResponse();
        response.setId("fire");
        response.setName("Fire Trainer");
        response.setHatColor("red");
        response.setShirtColor("black");
        response.setPantsColor("gray");
        response.setSkinTone("light");
        response.setEquipped(true);
        return response;
    }

    private CustomizationItemResponse customizationItemResponse() {
        CustomizationItemResponse response = new CustomizationItemResponse();
        response.setId("hat-01");
        response.setName("Cap");
        response.setCategory("hat");
        response.setUnlocked(false);
        response.setColor("blue");
        return response;
    }

}
