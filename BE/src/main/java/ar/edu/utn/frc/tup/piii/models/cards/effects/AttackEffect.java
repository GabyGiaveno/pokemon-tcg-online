package ar.edu.utn.frc.tup.piii.models.cards.effects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type",
        visible = true,
        defaultImpl = UnknownEffect.class
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ApplyConditionEffect.class, name = "APPLY_CONDITION"),
        @JsonSubTypes.Type(value = HealEffect.class, name = "HEAL"),
        @JsonSubTypes.Type(value = AddDamageEffect.class, name = "ADD_DAMAGE"),
        @JsonSubTypes.Type(value = MultiplierDamageEffect.class, name = "MULTIPLIER_DAMAGE"),
        @JsonSubTypes.Type(value = PreventDamageEffect.class, name = "PREVENT_DAMAGE"),
        @JsonSubTypes.Type(value = CoinFlipEffect.class, name = "COIN_FLIP"),
        @JsonSubTypes.Type(value = DiscardEnergyEffect.class, name = "DISCARD_ENERGY"),
        @JsonSubTypes.Type(value = DamageToBenchEffect.class, name = "DAMAGE_TO_BENCH"),
        @JsonSubTypes.Type(value = SwitchPokemonEffect.class, name = "SWITCH_POKEMON"),
        @JsonSubTypes.Type(value = SearchDeckEffect.class, name = "SEARCH_DECK"),
        @JsonSubTypes.Type(value = RestrictEffect.class, name = "RESTRICT"),
        @JsonSubTypes.Type(value = DamageCountersEffect.class, name = "DAMAGE_COUNTERS"),
        @JsonSubTypes.Type(value = PassiveAbilityEffect.class, name = "PASSIVE_ABILITY"),
        @JsonSubTypes.Type(value = LookAtDeckEffect.class, name = "LOOK_AT_DECK"),
        @JsonSubTypes.Type(value = DrawUntilHandSizeEffect.class, name = "DRAW_UNTIL_HAND_SIZE"),
        @JsonSubTypes.Type(value = ShuffleHandEffect.class, name = "SHUFFLE_HAND"),
        // Nested ability effects (xy1 abilities block — Bloque 4 tier 1)
        @JsonSubTypes.Type(value = CoinFlipDamageEffect.class, name = "COIN_FLIP_DAMAGE"),
        @JsonSubTypes.Type(value = ReduceDamageEffect.class, name = "REDUCE_DAMAGE"),
        @JsonSubTypes.Type(value = RestrictItemsEffect.class, name = "RESTRICT_ITEMS"),
        @JsonSubTypes.Type(value = ImmuneToConditionsEffect.class, name = "IMMUNE_TO_CONDITIONS")
})
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public abstract class AttackEffect {
    // Abstract base class for Jackson polymorphism
}
