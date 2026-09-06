package cn.edu.aviationquiz.service;

import cn.edu.aviationquiz.entity.Team;
import java.util.Comparator;
import java.util.List;

public final class RankingService {
    private static final Comparator<Team> ORDER = Comparator.comparingInt(Team::getScore).reversed()
            .thenComparing(Comparator.comparingInt(Team::getCorrectAnswers).reversed())
            .thenComparingLong(Team::getElapsedSeconds)
            .thenComparingLong(Team::getId);
    public List<Team> rank(List<Team> teams) { return teams.stream().sorted(ORDER).toList(); }
    public List<Team> promoted(List<Team> teams, int quota) {
        if (quota <= 0) throw new IllegalArgumentException("晋级名额必须大于 0");
        return rank(teams).stream().limit(quota).toList();
    }
}
