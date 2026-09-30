package cn.edu.aviationquiz.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cn.edu.aviationquiz.entity.Models.QuestionInput;
import cn.edu.aviationquiz.entity.Models.Session;
import cn.edu.aviationquiz.exception.BusinessException;
import cn.edu.aviationquiz.service.QuestionBankUseCases;

import org.junit.jupiter.api.Test;

import java.util.List;

class QuestionBankControllerTest {
    private static final Session STAFF =
            new Session("token", "staff-1", "admin01", "李老师", true);
    private static final QuestionInput QUESTION =
            new QuestionInput(
                    "中国民航史第一题",
                    "民航史",
                    List.of("A1", "B1", "C1", "D1"),
                    "A",
                    true);

    @Test
    void delegatesAValidQuestionToTheUseCaseInterface() {
        FakeQuestionBank service = new FakeQuestionBank();
        QuestionBankController controller = new QuestionBankController(service);

        assertEquals("question-1", controller.saveQuestion(STAFF, null, QUESTION));
        assertEquals(QUESTION, service.question);
    }

    @Test
    void rejectsAMissingRoundBeforeAddingAQuestion() {
        FakeQuestionBank service = new FakeQuestionBank();
        QuestionBankController controller = new QuestionBankController(service);

        assertThrows(
                BusinessException.class,
                () -> controller.addQuestionToRound(STAFF, " ", "question-1", 1));
        assertNull(service.roundId);
    }

    @Test
    void delegatesRoundCreationToTheUseCaseInterface() {
        FakeQuestionBank service = new FakeQuestionBank();
        QuestionBankController controller = new QuestionBankController(service);

        controller.addRound(STAFF, "competition-1", "必答轮", "REQUIRED", 1, 30);

        assertEquals("competition-1", service.competitionId);
    }

    private static final class FakeQuestionBank implements QuestionBankUseCases {
        private QuestionInput question;
        private String competitionId;
        private String roundId;

        @Override
        public String saveQuestion(
                Session session, String existingId, QuestionInput input) {
            question = input;
            return "question-1";
        }

        @Override
        public void questionState(Session session, String questionId, boolean active) {}

        @Override
        public void deleteQuestion(Session session, String questionId) {}

        @Override
        public String addRound(
                Session session,
                String competitionId,
                String name,
                String type,
                int sequence,
                int seconds) {
            this.competitionId = competitionId;
            return "round-1";
        }

        @Override
        public void updateRound(
                Session session,
                String roundId,
                String name,
                String type,
                int sequence,
                int seconds) {}

        @Override
        public void deleteEmptyRound(Session session, String roundId) {}

        @Override
        public void addQuestionToRound(
                Session session, String roundId, String questionId, int sequence) {
            this.roundId = roundId;
        }

        @Override
        public void removeRoundQuestion(Session session, String roundQuestionId) {}
    }
}
