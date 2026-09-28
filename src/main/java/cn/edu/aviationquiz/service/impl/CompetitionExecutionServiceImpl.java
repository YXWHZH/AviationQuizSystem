package cn.edu.aviationquiz.service.impl;

import cn.edu.aviationquiz.dao.GameDao;
import cn.edu.aviationquiz.entity.AnswerRecord;
import cn.edu.aviationquiz.entity.CompetitionRound;
import cn.edu.aviationquiz.exception.BusinessException;
import cn.edu.aviationquiz.service.CompetitionExecutionService;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/** Default competition execution rules, independent of JavaFX and JDBC. */
public final class CompetitionExecutionServiceImpl implements CompetitionExecutionService {
    private final GameDao gameDao;
    private final Clock clock;

    public CompetitionExecutionServiceImpl(GameDao gameDao, Clock clock) {
        this.gameDao = gameDao;
        this.clock = clock;
    }

    @Override
    public int submitAnswer(String playerId, String releaseId, String option) {
        long receivedAt = clock.millis();
        return gameDao.inTransaction(
                db -> {
                    var context = db.findAnswerContext(releaseId);
                    require(context.competitionStatus().equals("比赛中"), "竞赛不在比赛中");
                    require(!context.closed() && receivedAt < context.deadline(), "题目已结束或已超时");
                    require(receivedAt >= context.startedAt(), "系统时间异常");
                    require(
                            db.isPlayerAssignedToGroup(context.groupId(), playerId),
                            "您不属于当前答题小组");
                    require(!db.hasSettlement(releaseId, playerId), "本题已结算，请勿重复提交");
                    require(
                            option != null && List.of("A", "B", "C", "D").contains(option),
                            "请选择一个选项");
                    boolean correct = option.equals(context.correctAnswer());
                    int score =
                            CompetitionRound.of(context.roundType()).calculateScore(correct);
                    db.saveAnswer(
                            new AnswerRecord(
                                    id(),
                                    releaseId,
                                    playerId,
                                    option,
                                    correct,
                                    score,
                                    receivedAt));
                    return score;
                });
    }

    private static void require(boolean valid, String message) {
        if (!valid) throw new BusinessException(message);
    }

    private static String id() {
        return "A" + UUID.randomUUID().toString().replace("-", "");
    }
}
