package cn.edu.aviationquiz.service;

import static org.junit.jupiter.api.Assertions.*;

import cn.edu.aviationquiz.dao.GameDao;
import cn.edu.aviationquiz.entity.AnswerRecord;
import cn.edu.aviationquiz.entity.AnswerSubmissionContext;
import cn.edu.aviationquiz.exception.BusinessException;
import cn.edu.aviationquiz.service.impl.CompetitionExecutionServiceImpl;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

class CompetitionExecutionServiceTest {
    private static final Clock CLOCK =
            Clock.fixed(Instant.ofEpochMilli(1_000), ZoneOffset.UTC);

    @Test
    void scoresAndSavesThroughTheDaoContract() {
        FakeGameDao dao = new FakeGameDao();
        dao.context = context("RISK");
        CompetitionExecutionService service = new CompetitionExecutionServiceImpl(dao, CLOCK);

        assertEquals(20, service.submitAnswer("player-1", "release-1", "A"));
        assertNotNull(dao.saved);
        assertEquals("player-1", dao.saved.playerId());
        assertEquals(20, dao.saved.scoreChange());
        assertTrue(dao.saved.correct());
    }

    @Test
    void rejectsAnAlreadySettledAnswerBeforeSaving() {
        FakeGameDao dao = new FakeGameDao();
        dao.context = context("REQUIRED");
        dao.settled = true;
        CompetitionExecutionService service = new CompetitionExecutionServiceImpl(dao, CLOCK);

        BusinessException error =
                assertThrows(
                        BusinessException.class,
                        () -> service.submitAnswer("player-1", "release-1", "A"));

        assertEquals("本题已结算，请勿重复提交", error.getMessage());
        assertNull(dao.saved);
    }

    private static AnswerSubmissionContext context(String roundType) {
        return new AnswerSubmissionContext(
                "release-1",
                "competition-1",
                "比赛中",
                "group-1",
                roundType,
                "A",
                500,
                1_500,
                null);
    }

    private static final class FakeGameDao implements GameDao, GameDao.Transaction {
        private AnswerSubmissionContext context;
        private boolean settled;
        private AnswerRecord saved;

        @Override
        public <T> T inTransaction(Work<T> work) {
            try {
                return work.run(this);
            } catch (RuntimeException runtime) {
                throw runtime;
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        }

        @Override
        public AnswerSubmissionContext findAnswerContext(String releaseId) {
            return context;
        }

        @Override
        public boolean isPlayerAssignedToGroup(String groupId, String playerId) {
            return true;
        }

        @Override
        public boolean hasSettlement(String releaseId, String playerId) {
            return settled;
        }

        @Override
        public void saveAnswer(AnswerRecord answer) {
            saved = answer;
        }
    }
}
