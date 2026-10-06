package ar.edu.utn.frc.tup.piii.services.game;

import ar.edu.utn.frc.tup.piii.dtos.request.ActionRequest;
import ar.edu.utn.frc.tup.piii.entities.GameAction;
import ar.edu.utn.frc.tup.piii.entities.GameSession;
import ar.edu.utn.frc.tup.piii.models.cards.ActionType;
import ar.edu.utn.frc.tup.piii.repositories.GameActionRepository;
import ar.edu.utn.frc.tup.piii.services.GameActionLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GameActionLogServiceTest {

    @Mock
    private GameActionRepository gameActionRepository;

    private GameActionLogService service;

    private GameSession session;

    @BeforeEach
    void setUp() {
        service = new GameActionLogService(gameActionRepository, new ObjectMapper());
        session = GameSession.builder().id(UUID.randomUUID()).build();
    }

    @Test
    void logSuccess_persistsSuccessResultWithEvents() throws Exception {
        ActionRequest request = ActionRequest.builder().type(ActionType.DRAW_CARD).build();
        List<String> events = List.of("EVENT_1");

        service.logSuccess(session, request, 1L, 3, events);

        ArgumentCaptor<GameAction> captor = ArgumentCaptor.forClass(GameAction.class);
        verify(gameActionRepository).save(captor.capture());

        GameAction saved = captor.getValue();
        assertThat(saved.getPlayerId()).isEqualTo(1L);
        assertThat(saved.getTurnNumber()).isEqualTo(3);
        assertThat(saved.getActionType()).isEqualTo(ActionType.DRAW_CARD);
        assertThat(saved.getResult()).contains("\"status\":\"SUCCESS\"");
        assertThat(saved.getResult()).contains("\"events\"");
        assertThat(saved.getTimestamp()).isNotNull();
    }

    @Test
    void logSuccess_withNullEvents_stillWritesSuccessStatus() {
        ActionRequest request = ActionRequest.builder().type(ActionType.END_TURN).build();

        service.logSuccess(session, request, 2L, 1, null);

        ArgumentCaptor<GameAction> captor = ArgumentCaptor.forClass(GameAction.class);
        verify(gameActionRepository).save(captor.capture());

        assertThat(captor.getValue().getResult()).contains("\"status\":\"SUCCESS\"");
    }

    @Test
    void logFailure_persistsFailureResultWithMessage() {
        service.logFailure(session, ActionType.USE_ATTACK, 1L, 2, "Not your turn");

        ArgumentCaptor<GameAction> captor = ArgumentCaptor.forClass(GameAction.class);
        verify(gameActionRepository).save(captor.capture());

        GameAction saved = captor.getValue();
        assertThat(saved.getActionType()).isEqualTo(ActionType.USE_ATTACK);
        assertThat(saved.getResult()).contains("\"status\":\"FAILURE\"");
        assertThat(saved.getResult()).contains("Not your turn");
    }

    @Test
    void logFailure_withNullMessage_writesEmptyString() {
        service.logFailure(session, ActionType.PLAY_ITEM, 1L, 1, null);

        ArgumentCaptor<GameAction> captor = ArgumentCaptor.forClass(GameAction.class);
        verify(gameActionRepository).save(captor.capture());

        assertThat(captor.getValue().getResult()).contains("\"status\":\"FAILURE\"");
    }
}
