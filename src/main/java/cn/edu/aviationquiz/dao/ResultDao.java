package cn.edu.aviationquiz.dao;

import cn.edu.aviationquiz.entity.CompetitionResultContext;
import cn.edu.aviationquiz.entity.RankingSnapshot;
import cn.edu.aviationquiz.entity.Models.HistoryView;

import java.util.List;

/** Data-access contract for ranking calculation and final-result archival. */
public interface ResultDao {
    <T> T inTransaction(Work<T> work);

    @FunctionalInterface
    interface Work<T> {
        T run(Transaction transaction) throws Exception;
    }

    interface Transaction {
        CompetitionResultContext findCompetition(String competitionId) throws Exception;

        List<HistoryView> listStaffHistory() throws Exception;

        List<HistoryView> listPlayerHistory(String playerId) throws Exception;

        boolean hasUnfinishedRounds(String competitionId) throws Exception;

        List<RankingSnapshot> listRankingSnapshots(String competitionId) throws Exception;

        void insertResult(
                String id,
                String competitionId,
                String playerId,
                int finalScore,
                int rank,
                boolean promoted)
                throws Exception;

        void markCompetitionArchived(String competitionId) throws Exception;
    }
}
