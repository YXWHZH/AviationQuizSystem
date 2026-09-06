package cn.edu.aviationquiz.entity;

public final class RiskRound extends CompetitionRound {
    private final int multiplier;
    public RiskRound(String name, int seconds, int multiplier) {
        super(name, seconds);
        if (multiplier < 1 || multiplier > 3) throw new IllegalArgumentException("风险倍数只能为 1 至 3");
        this.multiplier = multiplier;
    }
    @Override public RoundType getType() { return RoundType.RISK; }
    @Override public int calculateScore(int baseScore, boolean correct, boolean timeout) {
        int value = baseScore * multiplier;
        return !timeout && correct ? value : -value;
    }
}
