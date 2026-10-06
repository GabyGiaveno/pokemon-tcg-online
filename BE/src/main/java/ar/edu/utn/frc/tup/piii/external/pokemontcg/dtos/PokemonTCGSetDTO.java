package ar.edu.utn.frc.tup.piii.external.pokemontcg.dtos;

import lombok.Data;

/**
 * DTO mapping the set object embedded in each card response from the pokemontcg.io API.
 */
@Data
public class PokemonTCGSetDTO {
    private String id;
    private String name;
    private String series;
    private int printedTotal;
    /** ISO date string from API, e.g. "2014-02-05". */
    private String releaseDate;
}
