package cn.edu.aviationquiz.entity;

/** DAO projection used by the result service to perform stable collection sorting. */
public record RankingSnapshot(
        String playerId,
        String name,
        String groupName,
        int totalScore,
        int correctCount,
        long elapsedMillis,
        int finalScore,
        int archivedRank,
        boolean promoted) {}
