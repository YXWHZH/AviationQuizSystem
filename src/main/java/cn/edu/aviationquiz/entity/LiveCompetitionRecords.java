package cn.edu.aviationquiz.entity;

import java.util.List;

/** Persistence-neutral records used by live competition business rules. */
public final class LiveCompetitionRecords {
    private LiveCompetitionRecords() {}

    public record NamedItem(String id, String name) {}

    public record ExpiredRelease(String releaseId, String groupId) {}

    public record OpenRelease(String releaseId, String groupId) {}

    public record PlayerQuestion(
            String releaseId,
            String roundName,
            String content,
            List<String> options,
            long deadline,
            Long closedAt,
            String answer,
            Boolean correct,
            Integer score) {}
}
