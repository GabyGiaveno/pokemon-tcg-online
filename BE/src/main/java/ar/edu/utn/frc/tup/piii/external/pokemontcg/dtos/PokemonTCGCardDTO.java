package ar.edu.utn.frc.tup.piii.external.pokemontcg.dtos;

import lombok.Data;

import java.util.List;

/**
 * DTO mapping a single card response from the pokemontcg.io API v2.
 */
@Data
public class PokemonTCGCardDTO {
    private String id;
    private String name;
    private String supertype;
    private List<String> subtypes;
    private String hp;
    private List<String> types;
    private String evolvesFrom;
    private Object attacks;
    private Object weaknesses;
    private Object resistances;
    private List<String> retreatCost;
    private PokemonTCGSetDTO set;
    private PokemonTCGImagesDTO images;
}
