package cn.edu.aviationquiz.entity;

/** Competition state and time window needed by registration rules. */
public record CompetitionRegistrationContext(
        String id, String status, long registerStart, long registerEnd) {}
