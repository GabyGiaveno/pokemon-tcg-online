package ar.edu.utn.frc.tup.piii.engine.mappers;

import ar.edu.utn.frc.tup.piii.models.cards.GameStatus;
import ar.edu.utn.frc.tup.piii.models.cards.SpecialCondition;
import ar.edu.utn.frc.tup.piii.models.game.*;
import ar.edu.utn.frc.tup.piii.models.game.state.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EngineStateMapper {

    // =========================================================================
    // DTO → Domain (state → engine)
    // =========================================================================

    public static BoardState toDomainState(GameBoardState dbState) {
        if (dbState == null) return null;

        return BoardState.builder()
                .matchState(dbState.getMatchState() != null ? dbState.getMatchState() : GameStatus.SETUP)
                .currentPhase(dbState.getPhase())
                .currentPlayerId(dbState.getCurrentPlayerId())
                .turnNumber(dbState.getTurnNumber() != null ? dbState.getTurnNumber() : 1)
                .firstPlayerHasActed(dbState.isFirstPlayerHasActed())
                .finishedReason(dbState.getFinishedReason())
                .setupCompletedPlayers(dbState.getSetupCompletedPlayers() != null
                        ? new ArrayList<>(dbState.getSetupCompletedPlayers()) : new ArrayList<>())
                .winnerId(dbState.getWinnerPlayerId())
                .activeStadiumCardId(dbState.getActiveStadiumCardId())
                .pendingSelection(dbState.getPendingSelection())
                .player1Field(toPlayerField(dbState.getPlayer1State()))
                .player2Field(toPlayerField(dbState.getPlayer2State()))
                .build();
    }

    // =========================================================================
    // Domain → DTO (engine → state)
    // =========================================================================

    public static void updateDbState(BoardState domainState, GameBoardState dbState) {
        if (domainState == null || dbState == null) return;

        dbState.setMatchState(domainState.getMatchState());
        dbState.setPhase(domainState.getCurrentPhase());
        dbState.setCurrentPlayerId(domainState.getCurrentPlayerId());
        dbState.setTurnNumber(domainState.getTurnNumber());
        dbState.setActiveStadiumCardId(domainState.getActiveStadiumCardId());
        dbState.setWinnerPlayerId(domainState.getWinnerId());
        dbState.setPendingSelection(domainState.getPendingSelection());
        dbState.setFirstPlayerHasActed(domainState.isFirstPlayerHasActed());
        dbState.setSetupCompletedPlayers(domainState.getSetupCompletedPlayers() != null
                ? new ArrayList<>(domainState.getSetupCompletedPlayers()) : new ArrayList<>());

        // Persist the PRECISE finished reason from the domain (5.3); generic marker only as fallback
        if (domainState.getMatchState() == GameStatus.FINISHED) {
            dbState.setFinishedReason(domainState.getFinishedReason() != null
                    ? domainState.getFinishedReason()
                    : "ENGINE_FINISHED");
        } else {
            dbState.setFinishedReason(null);
        }

        updatePlayerBoardState(domainState.getPlayer1Field(), dbState.getPlayer1State());
        updatePlayerBoardState(domainState.getPlayer2Field(), dbState.getPlayer2State());
    }

    // =========================================================================
    // PlayerField ↔ PlayerBoardState
    // =========================================================================

    private static PlayerField toPlayerField(PlayerBoardState state) {
        if (state == null) return null;

        TurnFlags turnFlags = new TurnFlags();
        turnFlags.setEnergyAttachedThisTurn(state.isHasAttachedEnergyThisTurn());
        turnFlags.setSupporterPlayedThisTurn(state.isHasPlayedSupporterThisTurn());
        turnFlags.setRetreatedThisTurn(state.isRetreatedThisTurn());
        turnFlags.setAttackedThisTurn(state.isAttackedThisTurn());
        turnFlags.setAbilitiesUsedThisTurn(state.getAbilitiesUsedThisTurn() != null
                ? new HashSet<>(state.getAbilitiesUsedThisTurn()) : new HashSet<>());

        return PlayerField.builder()
                .playerId(state.getPlayerId())
                .activePokemon(toActivePokemon(state.getActivePokemon()))
                .bench(toBenchPokemonList(state.getBench()))
                .hand(toInstanceIdList(state.getHand()))
                .deck(toInstanceIdList(state.getDeck()))
                .prizeCards(toInstanceIdList(state.getPrizeCards()))
                .discardPile(toInstanceIdList(state.getDiscardPile()))
                .turnFlags(turnFlags)
                .playerTurnCount(state.getPlayerTurnCount() != null ? state.getPlayerTurnCount() : 0)
                .instanceCardIds(buildInstanceCardMap(state))
                .build();
    }

    private static void updatePlayerBoardState(PlayerField domainField, PlayerBoardState dbState) {
        if (domainField == null || dbState == null) return;

        // ── Turn flags ──────────────────────────────────────────────────
        dbState.setHasAttachedEnergyThisTurn(domainField.getTurnFlags().isEnergyAttachedThisTurn());
        dbState.setHasPlayedSupporterThisTurn(domainField.getTurnFlags().isSupporterPlayedThisTurn());
        dbState.setRetreatedThisTurn(domainField.getTurnFlags().isRetreatedThisTurn());
        dbState.setAttackedThisTurn(domainField.getTurnFlags().isAttackedThisTurn());
        dbState.setAbilitiesUsedThisTurn(domainField.getTurnFlags().getAbilitiesUsedThisTurn() != null
                ? new HashSet<>(domainField.getTurnFlags().getAbilitiesUsedThisTurn()) : new HashSet<>());

        // ── Turn counters ───────────────────────────────────────────────
        dbState.setPlayerTurnCount(domainField.getPlayerTurnCount());

        // ── Active Pokémon ──────────────────────────────────────────────
        if (domainField.getActivePokemon() != null) {
            if (dbState.getActivePokemon() == null) {
                dbState.setActivePokemon(new PokemonInPlayState());
                dbState.getActivePokemon().setInstanceId(UUID.randomUUID().toString());
            }
            var domainActive = domainField.getActivePokemon();
            var dbActive = dbState.getActivePokemon();
            // Always sync cardId — evolution replaces the card identity on the same slot.
            dbActive.setCardId(domainActive.getCardId());
            dbActive.setCurrentHp(domainActive.getCurrentHp());
            dbActive.setMaxHp(domainActive.getMaxHp());
            dbActive.setCondition(domainActive.getCondition() != null
                    ? domainActive.getCondition().name()
                    : SpecialCondition.NONE.name());
            dbActive.setBurned(domainActive.isBurned());
            dbActive.setPoisoned(domainActive.isPoisoned());
            dbActive.setEnteredThisTurn(domainActive.isEnteredThisTurn());
            dbActive.setDamageProtected(domainActive.isDamageProtected());
            dbActive.setRestrictions(domainActive.getRestrictions() != null
                    ? new java.util.ArrayList<>(domainActive.getRestrictions())
                    : new java.util.ArrayList<>());

            // Write-back attached energies
            dbActive.setAttachedEnergies(toAttachedCardInstanceStateList(domainActive.getAttachedEnergies()));

            // Tool (max 1 per Pokémon)
            if (domainActive.getTool() != null) {
                List<CardInstanceState> tools = new ArrayList<>();
                tools.add(CardInstanceState.builder()
                        .instanceId(UUID.randomUUID().toString())
                        .cardId(domainActive.getTool().getCardId())
                        .build());
                dbActive.setAttachedTools(tools);
            } else {
                dbActive.setAttachedTools(new ArrayList<>());
            }
        } else {
            dbState.setActivePokemon(null);
        }

        // ── Bench ───────────────────────────────────────────────────────
        if (domainField.getBench() != null) {
            List<PokemonInPlayState> newBench = new ArrayList<>();
            for (int i = 0; i < domainField.getBench().size(); i++) {
                BenchPokemon dMon = domainField.getBench().get(i);
                PokemonInPlayState dbMon = null;
                if (dbState.getBench() != null && i < dbState.getBench().size()) {
                    PokemonInPlayState existing = dbState.getBench().get(i);
                    if (existing.getCardId().equals(dMon.getCardId())) {
                        dbMon = existing;
                    }
                }
                if (dbMon == null) {
                    dbMon = new PokemonInPlayState();
                    dbMon.setInstanceId(UUID.randomUUID().toString());
                    dbMon.setCardId(dMon.getCardId());
                }
                dbMon.setCurrentHp(dMon.getCurrentHp());
                dbMon.setMaxHp(dMon.getMaxHp());
                dbMon.setEnteredThisTurn(dMon.isEnteredThisTurn());

                // Write-back attached energies
                dbMon.setAttachedEnergies(toAttachedCardInstanceStateList(dMon.getAttachedEnergies()));

                // Tool (max 1 per Pokémon)
                if (dMon.getTool() != null) {
                    List<CardInstanceState> tools = new ArrayList<>();
                    tools.add(CardInstanceState.builder()
                            .instanceId(UUID.randomUUID().toString())
                            .cardId(dMon.getTool().getCardId())
                            .build());
                    dbMon.setAttachedTools(tools);
                } else {
                    dbMon.setAttachedTools(new ArrayList<>());
                }
                newBench.add(dbMon);
            }
            dbState.setBench(newBench);
        }

        // ── Card lists (preserve cardId across zone moves) ──────────────
        syncPlayerCardLists(domainField, dbState);
    }

    // =========================================================================
    // Card list synchronization
    // =========================================================================

    /**
     * Rebuilds all player card lists from domain data, preserving existing
     * {@link CardInstanceState} (with their cardId) across zone moves.
     *
     * <p>When the engine moves a card from deck → hand (e.g. DRAW), the domain
     * reflects the change via instanceId lists. This method rebuilds the DB
     * lists so the same instanceId keeps its cardId in the new zone.
     */
    private static void syncPlayerCardLists(PlayerField domainField, PlayerBoardState dbState) {
        // Index all existing cards by instanceId (from all zones, including attached cards).
        // Attached energies/tools must be included so they can be found when they move to discard.
        Map<String, CardInstanceState> existing = new HashMap<>();
        for (List<CardInstanceState> zone : Arrays.asList(
                dbState.getHand(), dbState.getDeck(),
                dbState.getPrizeCards(), dbState.getDiscardPile())) {
            if (zone != null) {
                for (CardInstanceState cis : zone) {
                    if (cis != null && cis.getInstanceId() != null) {
                        existing.put(cis.getInstanceId(), cis);
                    }
                }
            }
        }
        // Index attached cards from active Pokémon
        if (dbState.getActivePokemon() != null) {
            indexAttachedCards(dbState.getActivePokemon().getAttachedEnergies(), existing);
            indexAttachedCards(dbState.getActivePokemon().getAttachedTools(), existing);
        }
        // Index attached cards from bench Pokémon
        if (dbState.getBench() != null) {
            for (var benchMon : dbState.getBench()) {
                if (benchMon != null) {
                    indexAttachedCards(benchMon.getAttachedEnergies(), existing);
                    indexAttachedCards(benchMon.getAttachedTools(), existing);
                }
            }
        }

        Map<String, String> instanceCardIds = domainField.getInstanceCardIds();
        rebuildZone(domainField.getHand(), dbState.getHand(), existing, instanceCardIds, null);
        rebuildZone(domainField.getDeck(), dbState.getDeck(), existing, instanceCardIds, null);
        rebuildZone(domainField.getPrizeCards(), dbState.getPrizeCards(), existing, instanceCardIds, null);
        rebuildZone(domainField.getDiscardPile(), dbState.getDiscardPile(), existing, instanceCardIds, CardZone.DISCARD);
    }

    /**
     * Rebuilds a single card list from domain IDs, looking up existing
     * CardInstanceState by instanceId to preserve cardId/name across moves.
     */
    private static void rebuildZone(List<String> domainIds, List<CardInstanceState> dbList,
                                     Map<String, CardInstanceState> existing,
                                     Map<String, String> instanceCardIds,
                                     CardZone zone) {
        if (dbList == null || domainIds == null) return;

        dbList.clear();
        for (String instanceId : domainIds) {
            CardInstanceState match = existing.get(instanceId);
            if (match != null) {
                // Preserve existing metadata (cardId, name) — zone may have changed
                if (zone != null) match.setZone(zone);
                dbList.add(match);
            } else {
                // Not present as a CardInstanceState in any persisted zone. This happens when a
                // Pokémon (and its attachments) is knocked out: its Active/Bench slot is cleared
                // before this sync runs, so it is no longer indexed in `existing`, yet its
                // instanceId now lives in the discard pile. Recover the real cardId from the
                // load-time instanceId→cardId map so the card keeps its identity instead of
                // becoming an orphan with cardId == instanceId. The display name is re-enriched
                // from the card cache at the DTO layer.
                String cardId = instanceCardIds != null ? instanceCardIds.get(instanceId) : null;
                CardInstanceState instance = CardInstanceState.builder()
                        .instanceId(instanceId)
                        .cardId(cardId != null ? cardId : instanceId)
                        .build();
                if (zone != null) instance.setZone(zone);
                dbList.add(instance);
            }
        }
    }

    // =========================================================================
    // Domain model ↔ State model type converters
    // =========================================================================

    private static ActivePokemon toActivePokemon(PokemonInPlayState state) {
        if (state == null) return null;
        ActivePokemon active = new ActivePokemon();
        active.setInstanceId(state.getInstanceId());
        active.setCardId(state.getCardId());
        active.setCurrentHp(state.getCurrentHp());
        active.setMaxHp(state.getMaxHp());
        active.setAttachedEnergies(state.getAttachedEnergies() != null
                ? state.getAttachedEnergies().stream()
                        .map(c -> new AttachedCard(c.getInstanceId(), c.getCardId(), null))
                        .collect(Collectors.toList())
                : new ArrayList<>());
        active.setCondition(state.getCondition() != null
                ? SpecialCondition.valueOf(state.getCondition())
                : SpecialCondition.NONE);
        active.setBurned(state.isBurned());
        active.setPoisoned(state.isPoisoned());
        active.setEnteredThisTurn(state.isEnteredThisTurn());
        active.setDamageProtected(state.isDamageProtected());
        active.setRestrictions(state.getRestrictions() != null
                ? new java.util.HashSet<>(state.getRestrictions())
                : new java.util.HashSet<>());
        if (state.getAttachedTools() != null && !state.getAttachedTools().isEmpty()) {
            CardInstanceState t = state.getAttachedTools().get(0);
            active.setTool(new AttachedCard(t.getInstanceId(), t.getCardId(), null));
        }
        return active;
    }

    private static List<BenchPokemon> toBenchPokemonList(List<PokemonInPlayState> states) {
        if (states == null) return new ArrayList<>();
        return states.stream().map(state -> {
            BenchPokemon bench = new BenchPokemon();
            bench.setInstanceId(state.getInstanceId());
            bench.setCardId(state.getCardId());
            bench.setCurrentHp(state.getCurrentHp());
            bench.setMaxHp(state.getMaxHp());
            bench.setAttachedEnergies(state.getAttachedEnergies() != null
                    ? state.getAttachedEnergies().stream()
                            .map(c -> new AttachedCard(c.getInstanceId(), c.getCardId(), null))
                            .collect(Collectors.toList())
                    : new ArrayList<>());
            bench.setEnteredThisTurn(state.isEnteredThisTurn());
            if (state.getAttachedTools() != null && !state.getAttachedTools().isEmpty()) {
                CardInstanceState t = state.getAttachedTools().get(0);
                bench.setTool(new AttachedCard(t.getInstanceId(), t.getCardId(), null));
            }
            return bench;
        }).collect(Collectors.toList());
    }

    // =========================================================================
    // Utility helpers
    // =========================================================================

    /** Converts domain AttachedCard list to CardInstanceState list for persistence. */
    private static List<CardInstanceState> toAttachedCardInstanceStateList(List<AttachedCard> attachedCards) {
        if (attachedCards == null) return new ArrayList<>();
        return attachedCards.stream()
                .map(ac -> CardInstanceState.builder()
                        .instanceId(UUID.randomUUID().toString())
                        .cardId(ac.getCardId())
                        .build())
                .collect(Collectors.toList());
    }

    /** Extracts instance IDs from a CardInstanceState list for the domain. */
    private static List<String> toInstanceIdList(List<CardInstanceState> stateList) {
        if (stateList == null) return new ArrayList<>();
        return stateList.stream()
                .map(CardInstanceState::getInstanceId)
                .collect(Collectors.toList());
    }

    /** Builds instanceId → cardId map from all zones of a player's board state. */
    private static Map<String, String> buildInstanceCardMap(PlayerBoardState state) {
        Map<String, String> map = new HashMap<>();
        Stream.of(state.getHand(), state.getDeck(), state.getPrizeCards(), state.getDiscardPile())
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(c -> c != null && c.getInstanceId() != null && c.getCardId() != null)
                .forEach(c -> map.put(c.getInstanceId(), c.getCardId()));
        if (state.getActivePokemon() != null) {
            addPokemonCards(state.getActivePokemon(), map);
        }
        if (state.getBench() != null) {
            state.getBench().stream().filter(Objects::nonNull)
                    .forEach(p -> addPokemonCards(p, map));
        }
        return map;
    }

    private static void addPokemonCards(PokemonInPlayState p, Map<String, String> map) {
        if (p.getInstanceId() != null && p.getCardId() != null) {
            map.put(p.getInstanceId(), p.getCardId());
        }
        Stream.of(p.getAttachedEnergies(), p.getAttachedTools())
                .filter(Objects::nonNull).flatMap(List::stream)
                .filter(c -> c != null && c.getInstanceId() != null && c.getCardId() != null)
                .forEach(c -> map.put(c.getInstanceId(), c.getCardId()));
    }

    /** Adds a list of attached cards to the instanceId lookup map. */
    private static void indexAttachedCards(List<CardInstanceState> cards,
                                           Map<String, CardInstanceState> existing) {
        if (cards == null) return;
        for (CardInstanceState cis : cards) {
            if (cis != null && cis.getInstanceId() != null) {
                existing.put(cis.getInstanceId(), cis);
            }
        }
    }
}
