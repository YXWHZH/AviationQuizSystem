package cn.edu.aviationquiz.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Team {
    private final long id;
    private final String name;
    private final List<Player> members = new ArrayList<>();
    private int score;
    private int correctAnswers;
    private long elapsedSeconds;

    public Team(long id, String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("队伍名称不能为空");
        this.id = id;
        this.name = name.trim();
    }
    public long getId() { return id; }
    public String getName() { return name; }
    public List<Player> getMembers() { return Collections.unmodifiableList(members); }
    public int getScore() { return score; }
    public int getCorrectAnswers() { return correctAnswers; }
    public long getElapsedSeconds() { return elapsedSeconds; }
    public void addMember(Player player) {
        if (player == null) throw new IllegalArgumentException("选手不能为空");
        if (members.stream().anyMatch(p -> p.studentNumber().equals(player.studentNumber())))
            throw new IllegalArgumentException("该学号已在队伍中");
        members.add(player);
    }
    public void applyScore(int change, boolean correct, long seconds) {
        score += change;
        if (correct) correctAnswers++;
        elapsedSeconds += Math.max(seconds, 0);
    }
}
