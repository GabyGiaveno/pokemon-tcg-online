package ar.edu.utn.frc.tup.piii.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/** In-memory model caching a Pokemon TCG card from pokemontcg.io (id String, name, supertype, attacks JSONB). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Card {

	private String id;
	private String name;
	private String supertype;
	private List<String> subtypes;
	private Integer hp;
	private List<String> types;
	private String attacks;
	private String parsedEffects;
	private String weaknesses;
	private String resistances;
	private List<String> retreatCost;
	private String evolvesFrom;
	private CardSet cardSet;
	private String imageUrlLarge;
	private String imageUrlSmall;
	private List<ar.edu.utn.frc.tup.piii.models.cards.effects.trainer.TrainerEffect> parsedTrainerEffects;

}
