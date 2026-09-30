package cn.edu.aviationquiz.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cn.edu.aviationquiz.dao.QuestionBankDao;
import cn.edu.aviationquiz.entity.Models.QuestionInput;
import cn.edu.aviationquiz.entity.Models.QuestionView;
import cn.edu.aviationquiz.entity.Models.RoundQuestionView;
import cn.edu.aviationquiz.entity.Models.RoundView;
import cn.edu.aviationquiz.exception.BusinessException;
import cn.edu.aviationquiz.service.impl.QuestionBankServiceImpl;
import cn.edu.aviationquiz.service.impl.StandardRoundFactory;

import org.junit.jupiter.api.Test;

import java.util.List;

class QuestionBankServiceTest {
    private static final QuestionInput QUESTION =
            new QuestionInput(
                    " 中国民航史第一题 ",
                    "民航史",
                    List.of(" A1 ", "B1", "C1", "D1"),
                    "A",
                    true);

    @Test
    void validatesNormalizesAndSavesAQuestionThroughTheDaoContract() {
        FakeQuestionBankDao dao = new FakeQuestionBankDao();
        QuestionBankService service = service(dao);

        String questionId = service.saveQuestion(null, QUESTION);

        assertNotNull(questionId);
        assertEquals("中国民航史第一题", dao.insertedQuestion.content());
        assertEquals("A1", dao.insertedQuestion.options().getFirst());
    }

    @Test
    void returnsTypedQuestionViewsFromTheDaoContract() {
        FakeQuestionBankDao dao = new FakeQuestionBankDao();
        dao.questions =
                List.of(
                        new QuestionView(
                                "Q1",
                                "题目",
                                "民航史",
                                List.of("A", "B", "C", "D"),
                                "A",
                                true));
        QuestionBankService service = service(dao);

        assertEquals("Q1", service.questions().getFirst().id());
    }

    @Test
    void preventsEditingAQuestionAlreadyUsedByARound() {
        FakeQuestionBankDao dao = new FakeQuestionBankDao();
        dao.questionReferenced = true;
        QuestionBankService service = service(dao);

        assertThrows(
                BusinessException.class,
                () -> service.saveQuestion("Q1", QUESTION));
        assertNull(dao.updatedQuestion);
    }

    @Test
    void createsAValidatedPolymorphicRound() {
        FakeQuestionBankDao dao = new FakeQuestionBankDao();
        QuestionBankService service = service(dao);

        String roundId = service.addRound("C1", " 必答轮 ", "REQUIRED", 1, 30);

        assertNotNull(roundId);
        assertEquals("必答轮", dao.insertedRoundName);
        assertEquals("REQUIRED", dao.insertedRoundType);
    }

    @Test
    void preventsUsingTheSameQuestionTwiceInOneCompetition() {
        FakeQuestionBankDao dao = new FakeQuestionBankDao();
        dao.questionUsed = true;
        QuestionBankService service = service(dao);

        assertThrows(
                BusinessException.class,
                () -> service.addQuestionToRound("R1", "Q1", 1));
        assertNull(dao.insertedRoundQuestionId);
    }

    private static QuestionBankService service(FakeQuestionBankDao dao) {
        return new QuestionBankServiceImpl(dao, new StandardRoundFactory());
    }

    private static final class FakeQuestionBankDao
            implements QuestionBankDao, QuestionBankDao.Transaction {
        private String competitionStatus = "未开放";
        private boolean questionReferenced;
        private boolean roundEmpty = true;
        private boolean questionActive = true;
        private boolean questionAllowed = true;
        private boolean questionUsed;
        private QuestionInput insertedQuestion;
        private QuestionInput updatedQuestion;
        private String insertedRoundName;
        private String insertedRoundType;
        private String insertedRoundQuestionId;
        private List<QuestionView> questions = List.of();

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
        public List<QuestionView> listQuestions() {
            return questions;
        }

        @Override
        public List<QuestionView> listQuestionsForCompetition(String competitionId) {
            return questions;
        }

        @Override
        public List<RoundView> listRounds(String competitionId) {
            return List.of();
        }

        @Override
        public List<RoundQuestionView> listRoundQuestions(String roundId) {
            return List.of();
        }

        @Override
        public String findCompetitionStatus(String competitionId) {
            return competitionStatus;
        }

        @Override
        public String findRoundCompetitionId(String roundId) {
            return "C1";
        }

        @Override
        public String findRoundQuestionCompetitionId(String roundQuestionId) {
            return "C1";
        }

        @Override
        public boolean isQuestionReferenced(String questionId) {
            return questionReferenced;
        }

        @Override
        public void insertQuestion(String questionId, QuestionInput input) {
            insertedQuestion = input;
        }

        @Override
        public void updateQuestion(String questionId, QuestionInput input) {
            updatedQuestion = input;
        }

        @Override
        public void setQuestionActive(String questionId, boolean active) {}

        @Override
        public void deleteQuestion(String questionId) {}

        @Override
        public void insertRound(
                String roundId,
                String competitionId,
                String name,
                String type,
                int sequence,
                int seconds) {
            insertedRoundName = name;
            insertedRoundType = type;
        }

        @Override
        public void updateRound(
                String roundId, String name, String type, int sequence, int seconds) {}

        @Override
        public boolean isRoundEmpty(String roundId) {
            return roundEmpty;
        }

        @Override
        public void deleteRound(String roundId) {}

        @Override
        public boolean isQuestionActive(String questionId) {
            return questionActive;
        }

        @Override
        public boolean isQuestionAllowedForCompetition(
                String questionId, String competitionId) {
            return questionAllowed;
        }

        @Override
        public boolean isQuestionUsedInCompetition(
                String questionId, String competitionId) {
            return questionUsed;
        }

        @Override
        public void insertRoundQuestion(
                String roundQuestionId,
                String roundId,
                String questionId,
                int sequence) {
            insertedRoundQuestionId = roundQuestionId;
        }

        @Override
        public void deleteRoundQuestion(String roundQuestionId) {}
    }
}
