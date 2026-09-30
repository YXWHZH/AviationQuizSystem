package cn.edu.aviationquiz.controller;

import static org.junit.jupiter.api.Assertions.*;

import cn.edu.aviationquiz.entity.Models.RankingEntry;
import cn.edu.aviationquiz.entity.Models.HistoryView;
import cn.edu.aviationquiz.entity.Models.Session;
import cn.edu.aviationquiz.exception.BusinessException;
import cn.edu.aviationquiz.service.ResultUseCases;

import org.junit.jupiter.api.Test;

import java.util.List;

class ResultControllerTest {
    private static final Session STAFF =
            new Session("token", "staff-1", "admin01", "李老师", true);

    @Test
    void delegatesRankingToTheServiceInterface() {
        FakeResultService service = new FakeResultService();
        ResultController controller = new ResultController(service);

        assertEquals(1, controller.ranking(STAFF, "competition-1").size());
        assertEquals("competition-1", service.competitionId);
    }

    @Test
    void rejectsMissingCompetitionBeforeCallingTheService() {
        FakeResultService service = new FakeResultService();
        ResultController controller = new ResultController(service);

        assertThrows(BusinessException.class, () -> controller.archive(STAFF, " "));
        assertNull(service.competitionId);
    }

    @Test
    void delegatesTypedHistoryQuery() {
        ResultController controller = new ResultController(new FakeResultService());

        assertEquals("competition-1", controller.history(STAFF).getFirst().competitionId());
    }

    private static final class FakeResultService implements ResultUseCases {
        private String competitionId;

        @Override
        public List<HistoryView> history(Session session) {
            return List.of(
                    new HistoryView(
                            "competition-1", "航空知识赛", 1_000, 10, 1, "晋级"));
        }

        @Override
        public List<RankingEntry> ranking(Session session, String competitionId) {
            this.competitionId = competitionId;
            return List.of(new RankingEntry("P1", "张三", "A组", 10, 1, 100, 1, "待判定"));
        }

        @Override
        public List<RankingEntry> preview(Session session, String competitionId) {
            this.competitionId = competitionId;
            return List.of();
        }

        @Override
        public void archive(Session session, String competitionId) {
            this.competitionId = competitionId;
        }

        @Override
        public String exportCsv(Session session, String competitionId) {
            this.competitionId = competitionId;
            return "";
        }
    }
}
