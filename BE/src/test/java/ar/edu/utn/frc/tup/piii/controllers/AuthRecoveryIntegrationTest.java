package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.mail.EmailService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class AuthRecoveryIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private EmailService emailService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void forgotPassword_withAnyEmail_returnsGenericMessage() throws Exception {
        String body = """
            {"email": "anyone@test.com"}
            """;

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void forgotPassword_withNonExistingEmail_doesNotSendEmail() throws Exception {
        String body = """
            {"email": "nonexistent@test.com"}
            """;

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk());

        verify(emailService, never()).sendRecoveryEmail(anyString(), anyString(), anyString());
    }

    @Test
    @Disabled("Merge-debt (engram #85): pre-existing red on Produccion. register 404 in default profile — V009 uses Postgres `ON CONFLICT` while tests run on H2. Owner: profile/gamification team.")
    void fullRecoveryFlow() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis());
        String username = "recouser" + unique;
        String email = "reco" + unique + "@test.com";

        String registerBody = """
            {"username": "%s", "email": "%s", "password": "Pass123!"}
            """.formatted(username, email);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody))
                .andExpect(status().isCreated());

        String forgotBody = """
            {"email": "%s"}
            """.formatted(email);

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(forgotBody))
                .andExpect(status().isOk());

        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> userCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);

        verify(emailService).sendRecoveryEmail(emailCaptor.capture(), userCaptor.capture(), linkCaptor.capture());

        assertEquals(email, emailCaptor.getValue());
        assertEquals(username, userCaptor.getValue());
        assertTrue(linkCaptor.getValue().contains("/auth/reset-password?token="));

        String token = linkCaptor.getValue().substring(linkCaptor.getValue().indexOf("token=") + 6);
        assertNotNull(token);
        assertFalse(token.isEmpty());

        String resetBody = """
            {"token": "%s", "newPassword": "NewPass456!"}
            """.formatted(token);

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(resetBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isString());

        String loginBody = """
            {"username": "%s", "password": "NewPass456!"}
            """.formatted(username);

        String loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String returnedUsername = JsonPath.read(loginResult, "$.username");
        assertEquals(username, returnedUsername);
    }

    @Test
    void resetPassword_withInvalidToken_returns401() throws Exception {
        String body = """
            {"token": "invalid-token", "newPassword": "newPass123"}
            """;

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Disabled("Merge-debt (engram #85): pre-existing red on Produccion. register 404 in default profile — V009 uses Postgres `ON CONFLICT` while tests run on H2. Owner: profile/gamification team.")
    void resetPassword_withUsedToken_returns403() throws Exception {
        String unique = String.valueOf(System.currentTimeMillis());
        String username = "dupetoken" + unique;
        String email = "dupe" + unique + "@test.com";

        String registerBody = """
            {"username": "%s", "email": "%s", "password": "Pass123!"}
            """.formatted(username, email);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody))
                .andExpect(status().isCreated());

        String forgotBody = """
            {"email": "%s"}
            """.formatted(email);

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(forgotBody))
                .andExpect(status().isOk());

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendRecoveryEmail(anyString(), anyString(), linkCaptor.capture());

        String token = linkCaptor.getValue().substring(linkCaptor.getValue().indexOf("token=") + 6);

        String resetBody = """
            {"token": "%s", "newPassword": "NewPass456!"}
            """.formatted(token);

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(resetBody))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(resetBody))
                .andExpect(status().isForbidden());
    }
}
