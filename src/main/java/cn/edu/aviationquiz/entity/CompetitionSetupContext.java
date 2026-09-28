package cn.edu.aviationquiz.entity;

/** Persisted competition state required by setup and registration-state rules. */
public record CompetitionSetupContext(
        String id, String status, long registerStart, long registerEnd) {}
