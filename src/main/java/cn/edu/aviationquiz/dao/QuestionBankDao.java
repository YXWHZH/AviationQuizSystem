package cn.edu.aviationquiz.dao;

import cn.edu.aviationquiz.entity.Models.QuestionInput;

/** Data-access contract for questions, competition rounds and round question lists. */
public interface QuestionBankDao {
    <T> T inTransaction(Work<T> work);

    @FunctionalInterface
    interface Work<T> {
        T run(Transaction transaction) throws Exception;
    }

    interface Transaction {
        String findCompetitionStatus(String competitionId) throws Exception;

        String findRoundCompetitionId(String roundId) throws Exception;

        String findRoundQuestionCompetitionId(String roundQuestionId) throws Exception;

        boolean isQuestionReferenced(String questionId) throws Exception;

        void insertQuestion(String questionId, QuestionInput input) throws Exception;

        void updateQuestion(String questionId, QuestionInput input) throws Exception;

        void setQuestionActive(String questionId, boolean active) throws Exception;

        void deleteQuestion(String questionId) throws Exception;

        void insertRound(
                String roundId,
                String competitionId,
                String name,
                String type,
                int sequence,
                int seconds)
                throws Exception;

        void updateRound(
                String roundId, String name, String type, int sequence, int seconds)
                throws Exception;

        boolean isRoundEmpty(String roundId) throws Exception;

        void deleteRound(String roundId) throws Exception;

        boolean isQuestionActive(String questionId) throws Exception;

        boolean isQuestionAllowedForCompetition(String questionId, String competitionId)
                throws Exception;

        boolean isQuestionUsedInCompetition(String questionId, String competitionId)
                throws Exception;

        void insertRoundQuestion(
                String roundQuestionId, String roundId, String questionId, int sequence)
                throws Exception;

        void deleteRoundQuestion(String roundQuestionId) throws Exception;
    }
}
