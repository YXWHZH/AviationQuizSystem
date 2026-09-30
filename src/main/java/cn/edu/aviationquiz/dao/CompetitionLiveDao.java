package cn.edu.aviationquiz.dao;

import cn.edu.aviationquiz.entity.LiveCompetitionRecords.ExpiredRelease;
import cn.edu.aviationquiz.entity.LiveCompetitionRecords.NamedItem;
import cn.edu.aviationquiz.entity.LiveCompetitionRecords.OpenRelease;
import cn.edu.aviationquiz.entity.LiveCompetitionRecords.PlayerQuestion;
import cn.edu.aviationquiz.entity.Models.ActiveReleaseView;
import cn.edu.aviationquiz.entity.Models.CompetitionProgressView;
import cn.edu.aviationquiz.entity.Models.PlayerSubmissionView;

import java.util.List;

/** Data-access contract for starting and running a live competition. */
public interface CompetitionLiveDao {
    <T> T inTransaction(Work<T> work);

    @FunctionalInterface
    interface Work<T> {
        T run(Transaction transaction) throws Exception;
    }

    interface Transaction {
        String findCompetitionStatus(String competitionId) throws Exception;

        boolean hasRunningCompetition() throws Exception;

        boolean hasValidPlayer(String competitionId) throws Exception;

        boolean hasUnassignedPlayer(String competitionId) throws Exception;

        List<NamedItem> findGroups(String competitionId) throws Exception;

        List<NamedItem> findRounds(String competitionId) throws Exception;

        boolean groupHasPlayer(String groupId) throws Exception;

        boolean roundHasQuestion(String roundId) throws Exception;

        void createGroupRound(String id, String groupId, String roundId) throws Exception;

        void updateCompetitionStatus(String competitionId, String status) throws Exception;

        CompetitionProgressView findCurrentProgress(String competitionId) throws Exception;

        int countCompletedQuestions(String groupRoundId) throws Exception;

        void updateGroupRoundStatus(String groupRoundId, String status) throws Exception;

        boolean hasOpenRelease() throws Exception;

        String findNextRoundQuestion(String roundId, String groupRoundId) throws Exception;

        void createRelease(
                String releaseId,
                String groupRoundId,
                String roundQuestionId,
                long startedAt,
                long deadline)
                throws Exception;

        List<ExpiredRelease> findExpiredReleases(long now) throws Exception;

        List<String> findMemberIds(String groupId) throws Exception;

        boolean hasAnswer(String releaseId, String playerId) throws Exception;

        void createTimeout(String timeoutId, String releaseId, String playerId, long createdAt)
                throws Exception;

        void closeReleaseAtDeadline(String releaseId) throws Exception;

        ActiveReleaseView findActiveRelease(String competitionId) throws Exception;

        OpenRelease findOpenRelease(String competitionId) throws Exception;

        int countAnswers(String releaseId) throws Exception;

        void closeRelease(String releaseId, long closedAt) throws Exception;

        boolean hasIncompleteRoundQuestion(String roundId, String groupRoundId) throws Exception;

        boolean isProfileComplete(String playerId) throws Exception;

        PlayerQuestion findPlayerQuestion(String competitionId, String playerId) throws Exception;

        List<PlayerSubmissionView> findSubmissions(
                String groupRoundId, String groupId) throws Exception;
    }
}
