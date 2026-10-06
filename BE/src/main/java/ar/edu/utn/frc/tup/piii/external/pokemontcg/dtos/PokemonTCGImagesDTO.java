package ar.edu.utn.frc.tup.piii.external.pokemontcg.dtos;

import lombok.Data;

/**
 * DTO mapping the images object within a card response from the pokemontcg.io API.
 */
@Data
public class PokemonTCGImagesDTO {
    private String small;
    private String large;
}
