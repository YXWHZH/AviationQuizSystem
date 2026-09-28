package cn.edu.aviationquiz.entity;

/** Competition state needed to calculate and archive final results. */
public record CompetitionResultContext(String id, String status, int advanceCount) {}
