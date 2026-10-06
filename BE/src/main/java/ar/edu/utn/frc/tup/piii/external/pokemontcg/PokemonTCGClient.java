package ar.edu.utn.frc.tup.piii.external.pokemontcg;

import ar.edu.utn.frc.tup.piii.external.pokemontcg.dtos.PokemonTCGCardDTO;
import ar.edu.utn.frc.tup.piii.external.pokemontcg.dtos.PokemonTCGSetDTO;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/** WebClient-based reactive HTTP client for the pokemontcg.io REST API. */
@Component
public class PokemonTCGClient {

    private final WebClient webClient;

    public PokemonTCGClient() {
        this.webClient = WebClient.create("https://api.pokemontcg.io/v2");
    }

    public PokemonTCGSetDTO getSet(String setId) {
        return webClient.get()
                .uri("/sets/{id}", setId)
                .retrieve()
                .bodyToMono(PokemonTCGSetDTO.class)
                .block();
    }

    public List<PokemonTCGCardDTO> getCardsBySet(String setId) {
        Map response = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/cards")
                        .queryParam("q", "set.id:" + setId)
                        .queryParam("pageSize", 250)
                        .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey("data")) {
            return List.of();
        }

        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
        return data.stream().map(m -> {
            PokemonTCGCardDTO dto = new PokemonTCGCardDTO();
            dto.setId((String) m.get("id"));
            dto.setName((String) m.get("name"));
            dto.setSupertype((String) m.get("supertype"));
            return dto;
        }).toList();
    }
}
