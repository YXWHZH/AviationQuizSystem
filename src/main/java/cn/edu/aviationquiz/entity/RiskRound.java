package cn.edu.aviationquiz.entity;

public final class RiskRound extends CompetitionRound {
    @Override
    public int calculateScore(boolean correct) {
        return correct ? 20 : -20;
    }
}
