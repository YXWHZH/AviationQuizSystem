package cn.edu.aviationquiz.controller;

import static org.junit.jupiter.api.Assertions.*;

import cn.edu.aviationquiz.entity.Models.Session;
import cn.edu.aviationquiz.exception.BusinessException;
import cn.edu.aviationquiz.service.CompetitionRoomService;

import org.junit.jupiter.api.Test;

class CompetitionRoomControllerTest {
    private static final Session PLAYER =
            new Session("token", "player-1", "user01", "张三", false);

    @Test
    void validatesInputAndDelegatesToTheServiceInterface() {
        FakeRoomService service = new FakeRoomService();
        CompetitionRoomController controller = new CompetitionRoomController(service);

        assertEquals(10, controller.submitAnswer(PLAYER, "release-1", "A"));
        assertEquals("release-1", service.releaseId);
        assertEquals("A", service.option);
    }

    @Test
    void rejectsMissingViewInputBeforeCallingTheService() {
        FakeRoomService service = new FakeRoomService();
        CompetitionRoomController controller = new CompetitionRoomController(service);

        assertThrows(
                BusinessException.class,
                () -> controller.submitAnswer(PLAYER, "release-1", " "));
        assertNull(service.releaseId);
    }

    private static final class FakeRoomService implements CompetitionRoomService {
        private String releaseId;
        private String option;

        @Override
        public int submit(Session session, String releaseId, String option) {
            this.releaseId = releaseId;
            this.option = option;
            return 10;
        }
    }
}
