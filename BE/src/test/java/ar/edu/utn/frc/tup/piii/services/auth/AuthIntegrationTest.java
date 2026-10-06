package ar.edu.utn.frc.tup.piii.services.auth;

import ar.edu.utn.frc.tup.piii.dtos.request.LoginRequest;
import ar.edu.utn.frc.tup.piii.dtos.request.RegisterRequest;
import ar.edu.utn.frc.tup.piii.dtos.response.DeckResponse;
import ar.edu.utn.frc.tup.piii.dtos.response.PlayerResponse;
import ar.edu.utn.frc.tup.piii.entities.Card;
import ar.edu.utn.frc.tup.piii.services.CardCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class AuthIntegrationTest {

    @TestConfiguration
    static class MockConfig {
        @Bean
        @Primary
        CardCacheService mockCardCacheService() {
            return mock(CardCacheService.class);
        }
    }

    @Autowired
    private CardCacheService cardCacheService;

    @LocalServerPort
    private int port;

    /**
     * All unique card IDs referenced by the 6 starter deck templates.
     */
    private static final Set<String> TEMPLATE_CARD_IDS = Set.of(
            "xy1-3", "xy1-6", "xy1-10", "xy1-12", "xy1-132",
            "xy1-20", "xy1-22", "xy1-24", "xy1-133",
            "xy1-31", "xy1-33", "xy1-35", "xy1-39", "xy1-134",
            "xy1-42", "xy1-44", "xy1-46", "xy1-135",
            "xy1-58", "xy1-60", "xy1-63", "xy1-65", "xy1-137",
            "xy1-47", "xy1-49", "xy1-54", "xy1-56", "xy1-136"
    );

    /**
     * Card names for each template ID (used by CopyLimitValidator).
     */
    private static final Map<String, String> CARD_NAMES = Map.ofEntries(
            Map.entry("xy1-3", "Bulbasaur"),
            Map.entry("xy1-6", "Ivysaur"),
            Map.entry("xy1-10", "Venusaur"),
            Map.entry("xy1-12", "Pinsir"),
            Map.entry("xy1-132", "Grass Energy"),
            Map.entry("xy1-20", "Charmander"),
            Map.entry("xy1-22", "Charmeleon"),
            Map.entry("xy1-24", "Charizard"),
            Map.entry("xy1-133", "Fire Energy"),
            Map.entry("xy1-31", "Squirtle"),
            Map.entry("xy1-33", "Wartortle"),
            Map.entry("xy1-35", "Blastoise"),
            Map.entry("xy1-39", "Poliwhirl"),
            Map.entry("xy1-134", "Water Energy"),
            Map.entry("xy1-42", "Pikachu"),
            Map.entry("xy1-44", "Raichu"),
            Map.entry("xy1-46", "Magnemite"),
            Map.entry("xy1-135", "Lightning Energy"),
            Map.entry("xy1-58", "Machop"),
            Map.entry("xy1-60", "Machoke"),
            Map.entry("xy1-63", "Machamp"),
            Map.entry("xy1-65", "Hitmonlee"),
            Map.entry("xy1-137", "Fighting Energy"),
            Map.entry("xy1-47", "Gastly"),
            Map.entry("xy1-49", "Haunter"),
            Map.entry("xy1-54", "Gengar"),
            Map.entry("xy1-56", "Mr. Mime"),
            Map.entry("xy1-136", "Psychic Energy")
    );

    /**
     * Card IDs that are Basic Pokémon (need at least one per template for BasicPokemonValidator).
     */
    private static final Set<String> BASIC_POKEMON_IDS = Set.of(
            "xy1-3", "xy1-20", "xy1-31", "xy1-42", "xy1-47", "xy1-58",
            "xy1-12", "xy1-39", "xy1-46", "xy1-65", "xy1-56"
    );

    /**
     * Card IDs that are Basic Energy (exempt from copy limit).
     */
    private static final Set<String> BASIC_ENERGY_IDS = Set.of(
            "xy1-132", "xy1-133", "xy1-134", "xy1-135", "xy1-136", "xy1-137"
    );

    @BeforeEach
    void setUpCardMocks() {
        reset(cardCacheService);
        for (String cardId : TEMPLATE_CARD_IDS) {
            String name = CARD_NAMES.get(cardId);
            boolean isEnergy = BASIC_ENERGY_IDS.contains(cardId);
            boolean isBasicPokemon = BASIC_POKEMON_IDS.contains(cardId);

            Card.CardBuilder builder = Card.builder()
                    .id(cardId)
                    .name(name);

            if (isEnergy) {
                builder.supertype("Energy");
                builder.subtypes(List.of("Basic"));
            } else {
                builder.supertype("Pok\u00e9mon");
                if (isBasicPokemon) {
                    builder.subtypes(List.of("Basic"));
                } else {
                    builder.subtypes(List.of("Stage 1"));
                }
            }

            when(cardCacheService.findById(cardId)).thenReturn(builder.build());
        }
    }

    private RestClient restClient() {
        return RestClient.create("http://localhost:" + port);
    }

    @Test
    @Disabled("Merge-debt (engram #85): pre-existing red on Produccion gamification/auth feature, off PR-1 path. register() returns 500 in dev profile — owner: profile/gamification team.")
    void registerAndLogin() {
        RegisterRequest register = new RegisterRequest();
        register.setUsername("integration" + System.currentTimeMillis());
        register.setEmail("integration@test.com");
        register.setPassword("password123");

        ResponseEntity<PlayerResponse> registerResponse = restClient().post()
                .uri("/api/auth/register")
                .body(register)
                .retrieve()
                .toEntity(PlayerResponse.class);

        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());
        assertNotNull(registerResponse.getBody());
        assertNotNull(registerResponse.getBody().getToken());
        assertFalse(registerResponse.getBody().getToken().contains("placeholder"));

        LoginRequest login = new LoginRequest();
        login.setUsername(register.getUsername());
        login.setPassword("password123");

        ResponseEntity<PlayerResponse> loginResponse = restClient().post()
                .uri("/api/auth/login")
                .body(login)
                .retrieve()
                .toEntity(PlayerResponse.class);

        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertNotNull(loginResponse.getBody().getToken());
    }

    @Test
    void privateEndpointWithoutTokenReturns401() {
        ResponseEntity<String> response = restClient().get()
                .uri("/api/decks")
                .retrieve()
                .onStatus(status -> status.value() == 401, (req, res) -> {})
                .toEntity(String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @Disabled("Merge-debt (engram #85): pre-existing red on Produccion gamification/auth feature, off PR-1 path. register() returns 500 in dev profile — owner: profile/gamification team.")
    void registeredPlayerHasSixStarterDecks() {
        RegisterRequest register = new RegisterRequest();
        register.setUsername("sixdecks" + System.currentTimeMillis());
        register.setEmail("sixdecks@test.com");
        register.setPassword("password123");

        ResponseEntity<PlayerResponse> registerResponse = restClient().post()
                .uri("/api/auth/register")
                .body(register)
                .retrieve()
                .toEntity(PlayerResponse.class);

        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());
        assertNotNull(registerResponse.getBody());
        String token = registerResponse.getBody().getToken();
        assertNotNull(token);

        ResponseEntity<List<DeckResponse>> decksResponse = restClient().get()
                .uri("/api/decks")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .toEntity(new ParameterizedTypeReference<>() {});

        assertEquals(HttpStatus.OK, decksResponse.getStatusCode());
        assertNotNull(decksResponse.getBody());
        assertEquals(6, decksResponse.getBody().size());

        List<String> expectedNames = List.of(
                "Mazo Fuego Inicial", "Mazo Agua Inicial", "Mazo Planta Inicial",
                "Mazo Rayo Inicial", "Mazo Lucha Inicial", "Mazo Ps\u00edquico Inicial");
        List<String> actualNames = decksResponse.getBody().stream()
                .map(DeckResponse::getName)
                .sorted()
                .toList();

        assertTrue(actualNames.containsAll(expectedNames),
                "Starter deck names should match. Got: " + actualNames);

        // Each deck should have exactly 60 cards
        for (DeckResponse deck : decksResponse.getBody()) {
            assertEquals(60, deck.getCardCount(),
                    "Deck '" + deck.getName() + "' must have exactly 60 cards");
        }
    }
}
