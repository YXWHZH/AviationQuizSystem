package cn.edu.aviationquiz.dao;

import cn.edu.aviationquiz.entity.AnswerRecord;
import cn.edu.aviationquiz.entity.AnswerSubmissionContext;

/** Data-access contract for live competition execution. */
public interface GameDao {
    <T> T inTransaction(Work<T> work);

    @FunctionalInterface
    interface Work<T> {
        T run(Transaction transaction) throws Exception;
    }

    interface Transaction {
        AnswerSubmissionContext findAnswerContext(String releaseId) throws Exception;

        boolean isPlayerAssignedToGroup(String groupId, String playerId) throws Exception;

        boolean hasSettlement(String releaseId, String playerId) throws Exception;

        void saveAnswer(AnswerRecord answer) throws Exception;
    }
}
