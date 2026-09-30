package cn.edu.aviationquiz.service;

import static org.junit.jupiter.api.Assertions.*;

import cn.edu.aviationquiz.dao.ResultDao;
import cn.edu.aviationquiz.entity.CompetitionResultContext;
import cn.edu.aviationquiz.entity.RankingSnapshot;
import cn.edu.aviationquiz.entity.Models.HistoryView;
import cn.edu.aviationquiz.exception.BusinessException;
import cn.edu.aviationquiz.service.impl.ResultServiceImpl;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class ResultServiceTest {
    @Test
    void sortsByScoreCorrectCountElapsedTimeAndPlayerId() {
        FakeResultDao dao = new FakeResultDao();
        dao.rows =
                List.of(
                        row("P03", 20, 2, 900),
                        row("P02", 20, 2, 700),
                        row("P01", 20, 2, 700),
                        row("P04", 20, 1, 100));
        ResultService service = new ResultServiceImpl(dao);

        assertEquals(
                List.of("P01", "P02", "P03", "P04"),
                service.ranking("competition-1").stream().map(r -> r.playerId()).toList());
    }

    @Test
    void previewRequiresEveryRoundToBeComplete() {
        FakeResultDao dao = new FakeResultDao();
        dao.unfinished = true;
        ResultService service = new ResultServiceImpl(dao);

        BusinessException error =
                assertThrows(BusinessException.class, () -> service.preview("competition-1"));

        assertEquals("所有小组完成全部轮次后才能判定晋级", error.getMessage());
    }

    @Test
    void archivesCalculatedResultsAndCompetitionInOneDaoTransaction() {
        FakeResultDao dao = new FakeResultDao();
        dao.rows = List.of(row("P01", 30, 3, 600), row("P02", 10, 1, 300));
        ResultService service = new ResultServiceImpl(dao);

        service.archive("competition-1");

        assertEquals(2, dao.saved.size());
        assertTrue(dao.saved.getFirst().promoted());
        assertFalse(dao.saved.getLast().promoted());
        assertTrue(dao.archived);
        assertEquals(1, dao.transactions);
    }

    @Test
    void exportsOnlyArchivedCompetitionResults() {
        FakeResultDao dao = new FakeResultDao();
        ResultService service = new ResultServiceImpl(dao);

        assertThrows(BusinessException.class, () -> service.exportCsv("competition-1"));

        dao.context = new CompetitionResultContext("competition-1", "已结束", 1);
        dao.rows = List.of(new RankingSnapshot("=P01", "张三", "A组", 0, 2, 600, 30, 1, true));
        String csv = service.exportCsv("competition-1");
        assertTrue(csv.startsWith("\uFEFF名次"));
        assertTrue(csv.contains("\"'=P01\""));
    }

    private static RankingSnapshot row(String id, int score, int correct, long elapsed) {
        return new RankingSnapshot(id, id, "A组", score, correct, elapsed, 0, 0, false);
    }

    private static final class FakeResultDao implements ResultDao, ResultDao.Transaction {
        private CompetitionResultContext context =
                new CompetitionResultContext("competition-1", "比赛中", 1);
        private List<RankingSnapshot> rows = List.of();
        private final List<SavedResult> saved = new ArrayList<>();
        private boolean unfinished;
        private boolean archived;
        private int transactions;

        @Override
        public <T> T inTransaction(Work<T> work) {
            transactions++;
            try {
                return work.run(this);
            } catch (RuntimeException runtime) {
                throw runtime;
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        }

        @Override
        public CompetitionResultContext findCompetition(String competitionId) {
            return context;
        }

        @Override
        public List<HistoryView> listStaffHistory() {
            return List.of();
        }

        @Override
        public List<HistoryView> listPlayerHistory(String playerId) {
            return List.of();
        }

        @Override
        public boolean hasUnfinishedRounds(String competitionId) {
            return unfinished;
        }

        @Override
        public List<RankingSnapshot> listRankingSnapshots(String competitionId) {
            return rows;
        }

        @Override
        public void insertResult(
                String id,
                String competitionId,
                String playerId,
                int finalScore,
                int rank,
                boolean promoted) {
            saved.add(new SavedResult(playerId, finalScore, rank, promoted));
        }

        @Override
        public void markCompetitionArchived(String competitionId) {
            archived = true;
            context = new CompetitionResultContext(competitionId, "已结束", 1);
        }
    }

    private record SavedResult(String playerId, int score, int rank, boolean promoted) {}
}
