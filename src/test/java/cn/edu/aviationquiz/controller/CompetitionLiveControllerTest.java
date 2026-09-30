package cn.edu.aviationquiz.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cn.edu.aviationquiz.entity.Models.ActiveReleaseView;
import cn.edu.aviationquiz.entity.Models.CompetitionProgressView;
import cn.edu.aviationquiz.entity.Models.PlayerSubmissionView;
import cn.edu.aviationquiz.entity.Models.PublishedQuestionView;
import cn.edu.aviationquiz.entity.Models.Session;
import cn.edu.aviationquiz.exception.BusinessException;
import cn.edu.aviationquiz.service.CompetitionLiveUseCases;

import org.junit.jupiter.api.Test;

import java.util.List;

class CompetitionLiveControllerTest {
    private static final Session STAFF =
            new Session("token", "staff-1", "admin01", "李老师", true);

    @Test
    void delegatesAValidLiveCommandToTheUseCaseInterface() {
        FakeLiveUseCases service = new FakeLiveUseCases();
        CompetitionLiveController controller = new CompetitionLiveController(service);

        controller.startRound(STAFF, "competition-1");

        assertEquals("competition-1", service.competitionId);
    }

    @Test
    void exposesTypedLiveQueries() {
        CompetitionLiveController controller =
                new CompetitionLiveController(new FakeLiveUseCases());

        CompetitionProgressView progress = controller.progress(STAFF, "competition-1");

        assertEquals("第一组", progress.groupName());
        assertEquals("待提交", controller.monitor(STAFF, "competition-1").getFirst().status());
    }

    @Test
    void rejectsAMissingCompetitionBeforeCallingTheUseCase() {
        FakeLiveUseCases service = new FakeLiveUseCases();
        CompetitionLiveController controller = new CompetitionLiveController(service);

        assertThrows(BusinessException.class, () -> controller.publish(STAFF, " "));
        assertNull(service.competitionId);
    }

    private static final class FakeLiveUseCases implements CompetitionLiveUseCases {
        private String competitionId;

        @Override
        public void startCompetition(Session session, String competitionId) {
            this.competitionId = competitionId;
        }

        @Override
        public CompetitionProgressView progress(Session session, String competitionId) {
            this.competitionId = competitionId;
            return new CompetitionProgressView(
                    "group-round-1",
                    "group-1",
                    "round-1",
                    "第一组",
                    "必答轮",
                    30,
                    "REQUIRED",
                    "待开始");
        }

        @Override
        public int completedQuestions(Session session, String competitionId) {
            this.competitionId = competitionId;
            return 0;
        }

        @Override
        public void startRound(Session session, String competitionId) {
            this.competitionId = competitionId;
        }

        @Override
        public String publish(Session session, String competitionId) {
            this.competitionId = competitionId;
            return "release-1";
        }

        @Override
        public ActiveReleaseView activeRelease(Session session, String competitionId) {
            this.competitionId = competitionId;
            return null;
        }

        @Override
        public void closeQuestion(Session session, String competitionId) {
            this.competitionId = competitionId;
        }

        @Override
        public void finishRound(Session session, String competitionId) {
            this.competitionId = competitionId;
        }

        @Override
        public PublishedQuestionView room(Session session, String competitionId) {
            this.competitionId = competitionId;
            return null;
        }

        @Override
        public List<PlayerSubmissionView> monitor(Session session, String competitionId) {
            this.competitionId = competitionId;
            return List.of(new PlayerSubmissionView("张三", "待提交"));
        }
    }
}
