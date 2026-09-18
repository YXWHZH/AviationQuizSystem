package cn.edu.aviationquiz.entity;

public final class RequiredRound extends CompetitionRound {
    @Override
    public int calculateScore(boolean correct) {
        return correct ? 10 : 0;
    }
}
