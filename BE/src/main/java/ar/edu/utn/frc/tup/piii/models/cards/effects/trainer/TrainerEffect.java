package ar.edu.utn.frc.tup.piii.models.cards.effects.trainer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

/**
 * Base abstract class for parsed Trainer card effects.
 * Jackson uses the "type" property to instantiate the correct DTO.
 * Unknown / not-yet-implemented types fall back to {@link UnknownTrainerEffect} (inert).
 */
@Data
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type",
        visible = true,
        defaultImpl = UnknownTrainerEffect.class
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = DrawCardsTrainerEffect.class, name = "DRAW_CARDS"),
        @JsonSubTypes.Type(value = HealTrainerEffect.class, name = "HEAL"),
        @JsonSubTypes.Type(value = DiscardHandDrawTrainerEffect.class, name = "DISCARD_HAND_DRAW"),
        @JsonSubTypes.Type(value = ShuffleHandTrainerEffect.class, name = "SHUFFLE_HAND"),
        @JsonSubTypes.Type(value = DiscardEnergyTrainerEffect.class, name = "DISCARD_ENERGY"),
        @JsonSubTypes.Type(value = CoinFlipTrainerEffect.class, name = "COIN_FLIP"),
        @JsonSubTypes.Type(value = SearchDeckTrainerEffect.class, name = "SEARCH_DECK"),
        @JsonSubTypes.Type(value = ShufflePokemonIntoDeckTrainerEffect.class, name = "SHUFFLE_POKEMON_TO_DECK"),
        @JsonSubTypes.Type(value = RecycleTrainerEffect.class, name = "RECYCLE")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class TrainerEffect {
    // Shared properties across all trainer effects can go here
}
