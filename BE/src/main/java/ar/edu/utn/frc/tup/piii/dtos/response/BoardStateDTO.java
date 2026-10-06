package ar.edu.utn.frc.tup.piii.dtos.response;

import ar.edu.utn.frc.tup.piii.models.cards.GameStatus;
import ar.edu.utn.frc.tup.piii.models.cards.TurnPhase;

/** Response DTO projecting the full board state filtered for the requesting player (opponent hand as count only). */
public class BoardStateDTO {
    private String gameId;
    private Long currentPlayerId;
    private TurnPhase phase;
    private int turnNumber;
    private PlayerFieldDTO myField;
    private OpponentFieldDTO opponentField;
    private boolean isMyTurn;

    /** Match lifecycle (SETUP/ACTIVE/FINISHED) so the FE can route screens. */
    private GameStatus status;
    /** Authoritative Sudden Death round. 0 means the match is not in Sudden Death. */
    private int suddenDeathRound;
    /** Winner and precise reason — only meaningful when status == FINISHED. */
    private Long winnerId;
    private String finishedReason;
    /** Non-null when the engine is paused waiting for a player's choice (e.g. post-KO promotion). */
    private PendingSelectionDTO pendingSelection;

    /** READY_CHECK only: whether the requesting player / their opponent pressed "Listo". */
    private boolean myReady;
    private boolean opponentReady;
    /** READY_CHECK only: coin-flip winner (the only one allowed to choose who starts); null until both ready. */
    private Long coinFlipWinnerId;

    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }
    public Long getCurrentPlayerId() { return currentPlayerId; }
    public void setCurrentPlayerId(Long currentPlayerId) { this.currentPlayerId = currentPlayerId; }
    public TurnPhase getPhase() { return phase; }
    public void setPhase(TurnPhase phase) { this.phase = phase; }
    public int getTurnNumber() { return turnNumber; }
    public void setTurnNumber(int turnNumber) { this.turnNumber = turnNumber; }
    public PlayerFieldDTO getMyField() { return myField; }
    public void setMyField(PlayerFieldDTO myField) { this.myField = myField; }
    public OpponentFieldDTO getOpponentField() { return opponentField; }
    public void setOpponentField(OpponentFieldDTO opponentField) { this.opponentField = opponentField; }
    public boolean isMyTurn() { return isMyTurn; }
    public void setMyTurn(boolean myTurn) { isMyTurn = myTurn; }
    public GameStatus getStatus() { return status; }
    public void setStatus(GameStatus status) { this.status = status; }
    public int getSuddenDeathRound() { return suddenDeathRound; }
    public void setSuddenDeathRound(int suddenDeathRound) { this.suddenDeathRound = suddenDeathRound; }
    public Long getWinnerId() { return winnerId; }
    public void setWinnerId(Long winnerId) { this.winnerId = winnerId; }
    public String getFinishedReason() { return finishedReason; }
    public void setFinishedReason(String finishedReason) { this.finishedReason = finishedReason; }
    public PendingSelectionDTO getPendingSelection() { return pendingSelection; }
    public void setPendingSelection(PendingSelectionDTO pendingSelection) { this.pendingSelection = pendingSelection; }
    public boolean isMyReady() { return myReady; }
    public void setMyReady(boolean myReady) { this.myReady = myReady; }
    public boolean isOpponentReady() { return opponentReady; }
    public void setOpponentReady(boolean opponentReady) { this.opponentReady = opponentReady; }
    public Long getCoinFlipWinnerId() { return coinFlipWinnerId; }
    public void setCoinFlipWinnerId(Long coinFlipWinnerId) { this.coinFlipWinnerId = coinFlipWinnerId; }
}
