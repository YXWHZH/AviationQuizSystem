package cn.edu.aviationquiz.dao;

import cn.edu.aviationquiz.entity.CompetitionSetupContext;
import cn.edu.aviationquiz.entity.Models.CompetitionInput;
import cn.edu.aviationquiz.entity.Models.CompetitionView;

import java.util.List;
import java.util.Set;

/** Data-access contract for competition setup and registration-state transitions. */
public interface CompetitionDao {
    <T> T inTransaction(Work<T> work);

    @FunctionalInterface
    interface Work<T> {
        T run(Transaction transaction) throws Exception;
    }

    interface Transaction {
        CompetitionSetupContext findCompetition(String competitionId) throws Exception;

        List<CompetitionView> findAll() throws Exception;

        List<CompetitionView> findAllForPlayer(String playerId) throws Exception;

        List<CompetitionView> findParticipatedByPlayer(String playerId) throws Exception;

        Set<String> findUsedQuestionCategories(String competitionId) throws Exception;

        void insertCompetition(String competitionId, CompetitionInput input) throws Exception;

        void updateCompetition(String competitionId, CompetitionInput input) throws Exception;

        void replaceCategories(String competitionId, Set<String> categories) throws Exception;

        void updateStatus(String competitionId, String status) throws Exception;
    }
}
