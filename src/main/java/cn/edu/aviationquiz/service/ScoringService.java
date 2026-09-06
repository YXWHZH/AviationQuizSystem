package cn.edu.aviationquiz.service;

import cn.edu.aviationquiz.entity.CompetitionRound;
import cn.edu.aviationquiz.entity.Question;
import cn.edu.aviationquiz.entity.Team;

public final class ScoringService {
    public int score(Team team, CompetitionRound round, Question question, String answer, long elapsedSeconds) {
        if (team == null || round == null || question == null) throw new IllegalArgumentException("判分参数不完整");
        boolean timeout = elapsedSeconds > round.getTimeLimitSeconds();
        boolean correct = !timeout && question.isCorrect(answer);
        int change = round.calculateScore(question.defaultScore(), correct, timeout);
        team.applyScore(change, correct, elapsedSeconds);
        return change;
    }
}
