package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.response.PlayerResponse;
import ar.edu.utn.frc.tup.piii.exceptions.GlobalExceptionHandler;
import ar.edu.utn.frc.tup.piii.security.JwtAuthenticationFilter;
import ar.edu.utn.frc.tup.piii.services.PlayerService;
import ar.edu.utn.frc.tup.piii.services.RecoveryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlayerService playerService;

    @MockitoBean
    private RecoveryService recoveryService;

    @Test
    void register_returnsCreatedPlayer() throws Exception {
        PlayerResponse response = new PlayerResponse();
        response.setId(7L);
        response.setUsername("ash");
        response.setToken("jwt-token");
        when(playerService.register(eq("ash"), eq("ash@test.com"), eq("Pass123!"))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"username\":\"ash\"," +
                                "\"email\":\"ash@test.com\"," +
                                "\"password\":\"Pass123!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7L))
                .andExpect(jsonPath("$.username").value("ash"))
                .andExpect(jsonPath("$.token").value("jwt-token"));

        verify(playerService).register("ash", "ash@test.com", "Pass123!");
    }

    @Test
    void register_withInvalidBody_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"username\":\"a\"," +
                                "\"email\":\"not-an-email\"," +
                                "\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(allOf(
                        containsString("entre 3 y 50 caracteres"),
                        containsString("formato válido"),
                        containsString("obligatoria"))));
    }

    @Test
    void login_returnsToken() throws Exception {
        PlayerResponse response = new PlayerResponse();
        response.setId(7L);
        response.setUsername("ash");
        response.setToken("jwt-token");
        when(playerService.login(eq("ash"), eq("Pass123!"))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"username\":\"ash\"," +
                                "\"password\":\"Pass123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7L))
                .andExpect(jsonPath("$.token").value("jwt-token"));

        verify(playerService).login("ash", "Pass123!");
    }

    @Test
    void login_withWrongCredentials_returns401() throws Exception {
        when(playerService.login(eq("ash"), eq("wrong")))
                .thenThrow(new SecurityException("Usuario o contraseña incorrectos."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"username\":\"ash\"," +
                                "\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Usuario o contraseña incorrectos."));
    }

    @Test
    void forgotPassword_returnsGenericMessage() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ash@test.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                        "Si el correo existe, recibirás un enlace de recuperación."));

        verify(recoveryService).requestRecovery("ash@test.com");
    }

    @Test
    void resetPassword_returnsSuccessMessage() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"token\":\"reset-token\"," +
                                "\"newPassword\":\"NewPass456!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Contraseña restablecida correctamente."));

        verify(recoveryService).resetPassword("reset-token", "NewPass456!");
    }

    @Test
    void resetPassword_withInvalidToken_returns401() throws Exception {
        doThrow(new SecurityException("Invalid or expired token"))
                .when(recoveryService).resetPassword(eq("bad-token"), eq("NewPass456!"));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"token\":\"bad-token\"," +
                                "\"newPassword\":\"NewPass456!\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired token"));
    }
}
