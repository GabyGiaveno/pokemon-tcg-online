package ar.edu.utn.frc.tup.piii.external.pokemontcg.dtos;

import lombok.Data;

import java.util.List;

/**
 * DTO mapping the top-level wrapper response from the pokemontcg.io API v2 cards endpoint.
 */
@Data
public class PokemonTCGWrapperDTO {
    private List<PokemonTCGCardDTO> data;
}
