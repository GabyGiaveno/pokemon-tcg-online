package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.request.CreateDeckRequest;
import ar.edu.utn.frc.tup.piii.entities.Player;
import ar.edu.utn.frc.tup.piii.repositories.DeckRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Provisions default starter decks for newly registered players.
 * <p>
 * Holds 6 immutable XY1 starter templates (one per type). When invoked,
 * queries the player's existing default keys and creates only the missing
 * starter decks via {@link DeckService#createDeckForPlayer}.
 * </p>
 * Idempotent: re-running after all starters exist is a no-op.
 */
@Service
public class DefaultDeckProvisioningService {

    /**
     * Deck ratio: ~22 Pokémon / 26 Trainers / 12 Energy = 60 cards.
     * All cards are XY1 set. Basic Energy is exempt from the 4-copy limit.
     * Trainers are generic and shared across all decks for beginner usability.
     */
    private static final List<CreateDeckRequest.CardEntry> GENERIC_TRAINERS = List.of(
            new CreateDeckRequest.CardEntry("xy1-122", 4),  // Professor Sycamore
            new CreateDeckRequest.CardEntry("xy1-127", 3),  // Shauna
            new CreateDeckRequest.CardEntry("xy1-118", 4),  // Great Ball
            new CreateDeckRequest.CardEntry("xy1-116", 2),  // Evosoda
            new CreateDeckRequest.CardEntry("xy1-123", 2),  // Professor's Letter
            new CreateDeckRequest.CardEntry("xy1-121", 2),  // Muscle Band
            new CreateDeckRequest.CardEntry("xy1-119", 2),  // Hard Charm
            new CreateDeckRequest.CardEntry("xy1-128", 2),  // Super Potion
            new CreateDeckRequest.CardEntry("xy1-115", 2),  // Cassius
            new CreateDeckRequest.CardEntry("xy1-120", 1),  // Max Revive
            new CreateDeckRequest.CardEntry("xy1-124", 1),  // Red Card
            new CreateDeckRequest.CardEntry("xy1-129", 1)   // Team Flare Grunt
    );

    private static final List<DefaultDeckTemplate> TEMPLATES = List.of(
            // TUNED for consistency: focused Fennekin->Braixen->Delphox (4-3-2) line,
            // Slugma->Magcargo backup attacker (Heat Blast 80), plus colorless Tauros and
            // a cheap Pansear opener. 18 Pokemon / 26 Trainers / 16 Fire Energy = 60.
            new DefaultDeckTemplate("starter-fire", "Mazo Fuego Inicial",
                    concat(
                            List.of(
                                    new CreateDeckRequest.CardEntry("xy1-24", 4),  // Fennekin (Will-O-Wisp [F]=20)
                                    new CreateDeckRequest.CardEntry("xy1-25", 3),  // Braixen
                                    new CreateDeckRequest.CardEntry("xy1-26", 2),  // Delphox (Blaze Ball [CCC]=50+)
                                    new CreateDeckRequest.CardEntry("xy1-20", 3),  // Slugma
                                    new CreateDeckRequest.CardEntry("xy1-21", 2),  // Magcargo (Heat Blast [FFC]=80)
                                    new CreateDeckRequest.CardEntry("xy1-100", 2), // Tauros (colorless basic attacker)
                                    new CreateDeckRequest.CardEntry("xy1-22", 2)   // Pansear (cheap opener)
                            ),
                            GENERIC_TRAINERS,
                            List.of(
                                    new CreateDeckRequest.CardEntry("xy1-133", 16) // Fire Energy
                            )
                    )),
            // TUNED for consistency: focused Froakie->Frogadier->Greninja (4-3-3) line
            // (Greninja Mist Slash [W]=50 is the star attacker), Staryu->Starmie backup
            // (Core Splash + Recover heal), Lapras basic wall/opener.
            // 18 Pokemon / 26 Trainers / 16 Water Energy = 60.
            new DefaultDeckTemplate("starter-water", "Mazo Agua Inicial",
                    concat(
                            List.of(
                                    new CreateDeckRequest.CardEntry("xy1-39", 4),  // Froakie (Bounce [W]=10)
                                    new CreateDeckRequest.CardEntry("xy1-40", 3),  // Frogadier
                                    new CreateDeckRequest.CardEntry("xy1-41", 3),  // Greninja (Mist Slash [W]=50)
                                    new CreateDeckRequest.CardEntry("xy1-33", 3),  // Staryu
                                    new CreateDeckRequest.CardEntry("xy1-34", 2),  // Starmie (Core Splash + Recover)
                                    new CreateDeckRequest.CardEntry("xy1-35", 3)   // Lapras (basic wall/opener)
                            ),
                            GENERIC_TRAINERS,
                            List.of(
                                    new CreateDeckRequest.CardEntry("xy1-134", 16) // Water Energy
                            )
                    )),
            new DefaultDeckTemplate("starter-grass", "Mazo Planta Inicial",
                    concat(
                            List.of(
                                    new CreateDeckRequest.CardEntry("xy1-3", 4),   // Weedle
                                    new CreateDeckRequest.CardEntry("xy1-4", 2),   // Kakuna
                                    new CreateDeckRequest.CardEntry("xy1-5", 2),   // Beedrill
                                    new CreateDeckRequest.CardEntry("xy1-12", 4),  // Chespin
                                    new CreateDeckRequest.CardEntry("xy1-13", 2),  // Quilladin
                                    new CreateDeckRequest.CardEntry("xy1-14", 2),  // Chesnaught
                                    new CreateDeckRequest.CardEntry("xy1-18", 2),  // Skiddo
                                    new CreateDeckRequest.CardEntry("xy1-19", 2),  // Gogoat
                                    new CreateDeckRequest.CardEntry("xy1-1", 2)    // Venusaur-EX
                            ),
                            GENERIC_TRAINERS,
                            List.of(
                                    new CreateDeckRequest.CardEntry("xy1-132", 12) // Grass Energy
                            )
                    )),
            new DefaultDeckTemplate("starter-lightning", "Mazo Rayo Inicial",
                    concat(
                            List.of(
                                    new CreateDeckRequest.CardEntry("xy1-42", 4),  // Pikachu
                                    new CreateDeckRequest.CardEntry("xy1-43", 2),  // Raichu
                                    new CreateDeckRequest.CardEntry("xy1-44", 4),  // Voltorb
                                    new CreateDeckRequest.CardEntry("xy1-45", 2),  // Electrode
                                    new CreateDeckRequest.CardEntry("xy1-46", 4),  // Emolga-EX
                                    new CreateDeckRequest.CardEntry("xy1-101", 4), // Dunsparce
                                    new CreateDeckRequest.CardEntry("xy1-98", 2)   // Doduo
                            ),
                            GENERIC_TRAINERS,
                            List.of(
                                    new CreateDeckRequest.CardEntry("xy1-135", 12) // Lightning Energy
                            )
                    )),
            new DefaultDeckTemplate("starter-fighting", "Mazo Lucha Inicial",
                    concat(
                            List.of(
                                    new CreateDeckRequest.CardEntry("xy1-58", 4),  // Diglett
                                    new CreateDeckRequest.CardEntry("xy1-59", 2),  // Dugtrio
                                    new CreateDeckRequest.CardEntry("xy1-60", 4),  // Rhyhorn
                                    new CreateDeckRequest.CardEntry("xy1-61", 2),  // Rhydon
                                    new CreateDeckRequest.CardEntry("xy1-62", 2),  // Rhyperior
                                    new CreateDeckRequest.CardEntry("xy1-65", 4),  // Timburr
                                    new CreateDeckRequest.CardEntry("xy1-66", 2),  // Gurdurr
                                    new CreateDeckRequest.CardEntry("xy1-67", 2)   // Conkeldurr
                            ),
                            GENERIC_TRAINERS,
                            List.of(
                                    new CreateDeckRequest.CardEntry("xy1-137", 12) // Fighting Energy
                            )
                    )),
            new DefaultDeckTemplate("starter-psychic", "Mazo Psíquico Inicial",
                    concat(
                            List.of(
                                    new CreateDeckRequest.CardEntry("xy1-49", 4),  // Spoink
                                    new CreateDeckRequest.CardEntry("xy1-50", 2),  // Grumpig
                                    new CreateDeckRequest.CardEntry("xy1-51", 4),  // Venipede
                                    new CreateDeckRequest.CardEntry("xy1-52", 2),  // Whirlipede
                                    new CreateDeckRequest.CardEntry("xy1-53", 2),  // Scolipede
                                    new CreateDeckRequest.CardEntry("xy1-54", 4),  // Phantump
                                    new CreateDeckRequest.CardEntry("xy1-55", 2),  // Trevenant
                                    new CreateDeckRequest.CardEntry("xy1-56", 2)   // Pumpkaboo
                            ),
                            GENERIC_TRAINERS,
                            List.of(
                                    new CreateDeckRequest.CardEntry("xy1-136", 12) // Psychic Energy
                            )
                    ))
    );

    private final DeckRepository deckRepository;
    private final DeckService deckService;

    public DefaultDeckProvisioningService(DeckRepository deckRepository, DeckService deckService) {
        this.deckRepository = deckRepository;
        this.deckService = deckService;
    }

    /**
     * Provisions any default starter decks that the player does not already have.
     * Querying existing keys first ensures idempotency.
     *
     * @param player the newly registered player (already persisted)
     */
    public void provisionDefaults(Player player) {
        Set<String> existingKeys = deckRepository.findDefaultKeysByPlayerId(player.getId());

        for (DefaultDeckTemplate template : TEMPLATES) {
            if (existingKeys.contains(template.key)) {
                continue;
            }

            CreateDeckRequest request = new CreateDeckRequest();
            request.setName(template.name);
            request.setCards(template.cardEntries);

            deckService.createDeckForPlayer(player, request, template.key);
        }
    }

    /**
     * Concatenates multiple lists into a single mutable list.
     * Used by the TEMPLATES builder to merge Pokémon + Trainers + Energy.
     */
    @SafeVarargs
    private static List<CreateDeckRequest.CardEntry> concat(
            List<CreateDeckRequest.CardEntry>... lists) {
        List<CreateDeckRequest.CardEntry> result = new ArrayList<>();
        for (List<CreateDeckRequest.CardEntry> list : lists) {
            result.addAll(list);
        }
        return result;
    }

    /**
     * Internal record holding a single starter deck template definition.
     */
    private static final class DefaultDeckTemplate {
        private final String key;
        private final String name;
        private final List<CreateDeckRequest.CardEntry> cardEntries;

        DefaultDeckTemplate(String key, String name, List<CreateDeckRequest.CardEntry> cardEntries) {
            this.key = key;
            this.name = name;
            this.cardEntries = cardEntries;
        }
    }
}
