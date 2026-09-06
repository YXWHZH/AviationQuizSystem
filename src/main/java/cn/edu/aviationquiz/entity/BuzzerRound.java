package cn.edu.aviationquiz.entity;

public final class BuzzerRound extends CompetitionRound {
    public BuzzerRound(String name, int seconds) { super(name, seconds); }
    @Override public RoundType getType() { return RoundType.BUZZER; }
    @Override public int calculateScore(int baseScore, boolean correct, boolean timeout) {
        return !timeout && correct ? baseScore : -baseScore;
    }
}
