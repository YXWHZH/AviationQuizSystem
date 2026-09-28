package cn.edu.aviationquiz.entity;

/** One accepted answer and its immutable event-time scoring result. */
public record AnswerRecord(
        String id,
        String releaseId,
        String playerId,
        String option,
        boolean correct,
        int scoreChange,
        long submittedAt) {}
