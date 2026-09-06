package cn.edu.aviationquiz.entity;

public final class RequiredRound extends CompetitionRound {
    public RequiredRound(String name, int seconds) { super(name, seconds); }
    @Override public RoundType getType() { return RoundType.REQUIRED; }
    @Override public int calculateScore(int baseScore, boolean correct, boolean timeout) {
        return !timeout && correct ? baseScore : 0;
    }
}
