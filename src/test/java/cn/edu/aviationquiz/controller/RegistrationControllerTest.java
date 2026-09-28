package cn.edu.aviationquiz.controller;

import static org.junit.jupiter.api.Assertions.*;

import cn.edu.aviationquiz.entity.Models.Session;
import cn.edu.aviationquiz.exception.BusinessException;
import cn.edu.aviationquiz.service.RegistrationUseCases;

import org.junit.jupiter.api.Test;

class RegistrationControllerTest {
    private static final Session PLAYER =
            new Session("token", "player-1", "user01", "张三", false);

    @Test
    void delegatesValidRegistrationInputToTheServiceInterface() {
        FakeRegistrationService service = new FakeRegistrationService();
        RegistrationController controller = new RegistrationController(service);

        controller.join(PLAYER, "competition-1", false);

        assertEquals("competition-1", service.competitionId);
        assertFalse(service.reservation);
    }

    @Test
    void rejectsAnEmptyGroupNameBeforeCallingTheService() {
        FakeRegistrationService service = new FakeRegistrationService();
        RegistrationController controller = new RegistrationController(service);

        assertThrows(
                BusinessException.class,
                () -> controller.addGroup(PLAYER, "competition-1", " ", 1));
        assertNull(service.groupName);
    }

    private static final class FakeRegistrationService implements RegistrationUseCases {
        private String competitionId;
        private boolean reservation;
        private String groupName;

        @Override
        public void join(Session session, String competitionId, boolean reservation) {
            this.competitionId = competitionId;
            this.reservation = reservation;
        }

        @Override
        public void cancelParticipation(
                Session session, String competitionId, boolean reservation) {}

        @Override
        public String addGroup(
                Session session, String competitionId, String name, int sequence) {
            groupName = name;
            return "group-1";
        }

        @Override
        public void assign(Session session, String registrationId, String groupId) {}
    }
}
