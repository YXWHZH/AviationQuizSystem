package cn.edu.aviationquiz.entity;

import java.util.List;
import java.util.Set;

/** Immutable inputs and views, independent of JavaFX and JDBC. */
public final class Models {
    private Models() {}

    public record Session(String token, String id, String username, String name, boolean staff) {}

    public record PlayerProfileInput(
            String school, String college, String major, String studentNumber, String name, String phone) {}

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
