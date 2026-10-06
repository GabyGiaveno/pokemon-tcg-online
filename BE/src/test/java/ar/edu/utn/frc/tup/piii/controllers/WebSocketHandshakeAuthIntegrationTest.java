package ar.edu.utn.frc.tup.piii.controllers;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class WebSocketHandshakeAuthIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @Disabled("Merge-debt (engram #85): pre-existing red on Produccion. register 404 in default profile — V009 uses Postgres `ON CONFLICT` while tests run on H2. Owner: profile/gamification team.")
    void wsInfo_withValidToken_isNotUnauthorized() {
        String unique = String.valueOf(System.currentTimeMillis());
        String username = "wsuser" + unique;
        String email = "ws" + unique + "@test.com";
        String password = "Pass123!";

        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);

        String registerBody = """
            {"username": "%s", "email": "%s", "password": "%s"}
            """.formatted(username, email, password);

        ResponseEntity<String> registerResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/auth/register",
                new HttpEntity<>(registerBody, jsonHeaders),
                String.class);
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        String loginBody = """
            {"username": "%s", "password": "%s"}
            """.formatted(username, password);

        ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/auth/login",
                new HttpEntity<>(loginBody, jsonHeaders),
                String.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        String token = JsonPath.read(loginResponse.getBody(), "$.token");

        ResponseEntity<String> wsInfoResponse = restTemplate.exchange(
                "http://localhost:" + port + "/ws/info?token=" + token,
                HttpMethod.GET,
                null,
                String.class);

        assertThat(wsInfoResponse.getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
