package cn.edu.aviationquiz.entity;

/** Immutable data needed to validate and score one answer submission. */
public record AnswerSubmissionContext(
        String releaseId,
        String competitionId,
        String competitionStatus,
        String groupId,
        String roundType,
        String correctAnswer,
        long startedAt,
        long deadline,
        Long closedAt) {
    public boolean closed() {
        return closedAt != null;
    }
}
