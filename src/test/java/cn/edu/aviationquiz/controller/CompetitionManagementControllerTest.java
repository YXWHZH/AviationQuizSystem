package cn.edu.aviationquiz.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cn.edu.aviationquiz.entity.Models.CompetitionInput;
import cn.edu.aviationquiz.entity.Models.CompetitionView;
import cn.edu.aviationquiz.entity.Models.Session;
import cn.edu.aviationquiz.exception.BusinessException;
import cn.edu.aviationquiz.service.CompetitionManagementUseCases;

import org.junit.jupiter.api.Test;

import java.util.List;

class CompetitionManagementControllerTest {
    private static final Session STAFF =
            new Session("token", "staff-1", "admin01", "李老师", true);
    private static final CompetitionInput INPUT =
            new CompetitionInput("航空知识赛", "简介", 1_000, 2_000, 3_000, 2);

    @Test
    void delegatesCompetitionSavingToTheUseCaseInterface() {
        FakeCompetitionManagement service = new FakeCompetitionManagement();
        CompetitionManagementController controller =
                new CompetitionManagementController(service);

        assertEquals("competition-1", controller.save(STAFF, null, INPUT));
        assertEquals(INPUT, service.input);
    }

    @Test
    void rejectsAMissingCompetitionBeforeChangingState() {
        FakeCompetitionManagement service = new FakeCompetitionManagement();
        CompetitionManagementController controller =
                new CompetitionManagementController(service);

        assertThrows(
                BusinessException.class,
                () -> controller.changeRegistrationState(STAFF, " ", "报名中"));
        assertNull(service.competitionId);
    }

    @Test
    void delegatesTypedCompetitionQueries() {
        CompetitionManagementController controller =
                new CompetitionManagementController(new FakeCompetitionManagement());

        assertEquals("competition-1", controller.competitions().getFirst().id());
        assertEquals(
                "competition-1",
                controller.competitionsForPlayer(STAFF).getFirst().id());
    }

    private static final class FakeCompetitionManagement
            implements CompetitionManagementUseCases {
        private CompetitionInput input;
        private String competitionId;

        @Override
        public List<CompetitionView> competitions() {
            return List.of(competition());
        }

        @Override
        public List<CompetitionView> competitionsForPlayer(Session session) {
            return List.of(competition());
        }

        @Override
        public List<CompetitionView> participatedCompetitions(Session session) {
            return List.of(competition());
        }

        private static CompetitionView competition() {
            return new CompetitionView(
                    "competition-1",
                    "航空知识赛",
                    "简介",
                    1_000,
                    2_000,
                    3_000,
                    "未开放",
                    2,
                    "民航史",
                    "",
                    "",
                    "未参与");
        }

        @Override
        public String saveCompetition(
                Session session, String existingId, CompetitionInput input) {
            this.input = input;
            return "competition-1";
        }

        @Override
        public void registrationState(
                Session session, String competitionId, String targetState) {
            this.competitionId = competitionId;
        }
    }
}
