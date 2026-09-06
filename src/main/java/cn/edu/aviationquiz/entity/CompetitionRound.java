package cn.edu.aviationquiz.entity;

public abstract class CompetitionRound {
    private final String name;
    private final int timeLimitSeconds;
    protected CompetitionRound(String name, int timeLimitSeconds) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("轮次名称不能为空");
        if (timeLimitSeconds <= 0) throw new IllegalArgumentException("答题时间必须大于 0");
        this.name = name;
        this.timeLimitSeconds = timeLimitSeconds;
    }
    public String getName() { return name; }
    public int getTimeLimitSeconds() { return timeLimitSeconds; }
    public abstract RoundType getType();
    public abstract int calculateScore(int baseScore, boolean correct, boolean timeout);
}
