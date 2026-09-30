package cn.edu.aviationquiz.entity;

import java.util.List;
import java.util.Set;

/** Immutable inputs and views, independent of JavaFX and JDBC. */
public final class Models {
    private Models() {}

    public record Session(String token, String id, String username, String name, boolean staff) {}

    public record PlayerProfileInput(
            String school, String college, String major, String studentNumber, String name, String phone) {}

    public record PlayerProfileView(
            String username,
            String name,
            String phone,
            String school,
            String college,
            String major,
            String studentNumber,
            boolean complete) {}

    public record CompetitionInput(
            String name,
            String description,
            long registerStart,
            long registerEnd,
            long competitionTime,
            int quota,
            Set<String> categories) {
        public CompetitionInput(String name, String description, long registerStart, long registerEnd,
                long competitionTime, int quota) {
            this(name, description, registerStart, registerEnd, competitionTime, quota,
                    Set.of("民航史", "飞行原理", "航空法规"));
        }
    }

    public record QuestionInput(
            String content, String category, List<String> options, String answer, boolean active) {}

    public record QuestionView(
            String id,
            String content,
            String category,
            List<String> options,
            String answer,
            boolean active) {
        @Override
        public String toString() {
            return content;
        }
    }

    public record RoundView(
            String id, String name, String type, int sequence, int timeLimitSeconds) {}

    public record RoundQuestionView(String id, int sequence, String content) {}

    public record CompetitionProgressView(
            String groupRoundId,
            String groupId,
            String roundId,
            String groupName,
            String roundName,
            int timeLimitSeconds,
            String roundType,
            String status) {}

    public record ActiveReleaseView(String releaseId, String content, long deadline) {}

    public record PlayerSubmissionView(String name, String status) {}

    public record RankingEntry(
            String playerId,
            String name,
            String group,
            int score,
            int correctCount,
            long elapsedMillis,
            int rank,
            String promotion) {}

    public record PublishedQuestionView(
            String releaseId,
            String roundName,
            String content,
            List<String> options,
            long deadline,
            String status,
            String answer,
            Integer score) {}
}
