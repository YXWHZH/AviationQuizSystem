package cn.edu.aviationquiz.entity;

public final class BuzzerRound extends CompetitionRound {
    @Override
    public int calculateScore(boolean correct) {
        return correct ? 10 : -10;
    }
}
