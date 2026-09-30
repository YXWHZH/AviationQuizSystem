package cn.edu.aviationquiz.service.impl;

import cn.edu.aviationquiz.dao.CompetitionLiveDao;
import cn.edu.aviationquiz.entity.LiveCompetitionRecords.NamedItem;
import cn.edu.aviationquiz.entity.LiveCompetitionRecords.OpenRelease;
import cn.edu.aviationquiz.entity.LiveCompetitionRecords.PlayerQuestion;
import cn.edu.aviationquiz.entity.Models.ActiveReleaseView;
import cn.edu.aviationquiz.entity.Models.CompetitionProgressView;
import cn.edu.aviationquiz.entity.Models.PlayerSubmissionView;
import cn.edu.aviationquiz.entity.Models.PublishedQuestionView;
import cn.edu.aviationquiz.exception.BusinessException;
import cn.edu.aviationquiz.service.CompetitionLiveService;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/** Default live competition workflow, independent of JavaFX and JDBC. */
public final class CompetitionLiveServiceImpl implements CompetitionLiveService {
    private final CompetitionLiveDao dao;
    private final Clock clock;

    public CompetitionLiveServiceImpl(CompetitionLiveDao dao, Clock clock) {
        this.dao = dao;
        this.clock = clock;
    }

    @Override
    public void startCompetition(String competitionId) {
        dao.inTransaction(
                db -> {
                    require(db.findCompetitionStatus(competitionId).equals("报名截止"), "请先截止报名");
                    require(!db.hasRunningCompetition(), "已有一场竞赛正在运行");
                    require(db.hasValidPlayer(competitionId), "没有正式参赛选手");
                    require(!db.hasUnassignedPlayer(competitionId), "还有选手未分组");
                    List<NamedItem> groups = db.findGroups(competitionId);
                    List<NamedItem> rounds = db.findRounds(competitionId);
                    require(!groups.isEmpty() && !rounds.isEmpty(), "请配置小组和轮次");
                    for (NamedItem group : groups)
                        require(db.groupHasPlayer(group.id()), "存在空小组：" + group.name());
                    for (NamedItem round : rounds)
                        require(db.roundHasQuestion(round.id()), "轮次尚未配置题目：" + round.name());
                    for (NamedItem group : groups)
                        for (NamedItem round : rounds)
                            db.createGroupRound(id("GR"), group.id(), round.id());
                    db.updateCompetitionStatus(competitionId, "比赛中");
                    return null;
                });
    }

    @Override
    public CompetitionProgressView progress(String competitionId) {
        return dao.inTransaction(db -> db.findCurrentProgress(competitionId));
    }

    @Override
    public int completedQuestions(String competitionId) {
        return dao.inTransaction(
                db -> {
                    CompetitionProgressView current = db.findCurrentProgress(competitionId);
                    return current == null
                            ? 0
                            : db.countCompletedQuestions(current.groupRoundId());
                });
    }

    @Override
    public void startRound(String competitionId) {
        dao.inTransaction(
                db -> {
                    requireRunning(db, competitionId);
                    CompetitionProgressView current = db.findCurrentProgress(competitionId);
                    require(current != null && current.status().equals("待开始"), "没有待开始轮次");
                    db.updateGroupRoundStatus(current.groupRoundId(), "进行中");
                    return null;
                });
    }

    @Override
    public String publish(String competitionId) {
        recover();
        return dao.inTransaction(
                db -> {
                    requireRunning(db, competitionId);
                    CompetitionProgressView current = db.findCurrentProgress(competitionId);
                    require(current != null && current.status().equals("进行中"), "请先开始当前轮次");
                    require(!db.hasOpenRelease(), "当前题目尚未结束");
                    String roundQuestionId =
                            db.findNextRoundQuestion(
                                    current.roundId(), current.groupRoundId());
                    require(roundQuestionId != null, "本轮全部题目已发布，请结束轮次");
                    String releaseId = id("PUB");
                    long startedAt = clock.millis();
                    long deadline =
                            Math.addExact(
                                    startedAt,
                                    Math.multiplyExact(current.timeLimitSeconds(), 1000L));
                    db.createRelease(
                            releaseId,
                            current.groupRoundId(),
                            roundQuestionId,
                            startedAt,
                            deadline);
                    return releaseId;
                });
    }

    @Override
    public boolean recover() {
        return dao.inTransaction(
                db -> {
                    var expired = db.findExpiredReleases(clock.millis());
                    for (var release : expired) {
                        for (String playerId : db.findMemberIds(release.groupId()))
                            if (!db.hasAnswer(release.releaseId(), playerId))
                                db.createTimeout(
                                        id("T"),
                                        release.releaseId(),
                                        playerId,
                                        clock.millis());
                        db.closeReleaseAtDeadline(release.releaseId());
                    }
                    return !expired.isEmpty();
                });
    }

    @Override
    public ActiveReleaseView activeRelease(String competitionId) {
        return dao.inTransaction(db -> db.findActiveRelease(competitionId));
    }

    @Override
    public void closeQuestion(String competitionId) {
        recover();
        dao.inTransaction(
                db -> {
                    requireRunning(db, competitionId);
                    OpenRelease release = db.findOpenRelease(competitionId);
                    require(release != null, "当前没有正在作答的题目");
                    require(
                            db.countAnswers(release.releaseId())
                                    == db.findMemberIds(release.groupId()).size(),
                            "还有选手未提交，请等待倒计时结束");
                    db.closeRelease(release.releaseId(), clock.millis());
                    return null;
                });
    }

    @Override
    public void finishRound(String competitionId) {
        recover();
        dao.inTransaction(
                db -> {
                    requireRunning(db, competitionId);
                    CompetitionProgressView current = db.findCurrentProgress(competitionId);
                    require(current != null && current.status().equals("进行中"), "没有进行中的轮次");
                    require(
                            !db.hasIncompleteRoundQuestion(
                                    current.roundId(), current.groupRoundId()),
                            "本轮还有未完成题目");
                    db.updateGroupRoundStatus(current.groupRoundId(), "已完成");
                    return null;
                });
    }

    @Override
    public PublishedQuestionView room(String playerId, String competitionId) {
        recover();
        return dao.inTransaction(
                db -> {
                    require(db.isProfileComplete(playerId), "请先完善个人资料");
                    PlayerQuestion question = db.findPlayerQuestion(competitionId, playerId);
                    if (question == null) return null;
                    String status =
                            question.answer() != null
                                    ? (Boolean.TRUE.equals(question.correct()) ? "回答正确" : "回答错误")
                                    : question.closedAt() == null ? "可作答" : "已超时，本题 0 分";
                    return new PublishedQuestionView(
                            question.releaseId(),
                            question.roundName(),
                            question.content(),
                            question.options(),
                            question.deadline(),
                            status,
                            question.answer() == null ? "" : question.answer(),
                            question.score());
                });
    }

    @Override
    public List<PlayerSubmissionView> monitor(String competitionId) {
        return dao.inTransaction(
                db -> {
                    CompetitionProgressView current = db.findCurrentProgress(competitionId);
                    return current == null
                            ? List.of()
                            : db.findSubmissions(
                                    current.groupRoundId(), current.groupId());
                });
    }

    private static void requireRunning(
            CompetitionLiveDao.Transaction db, String competitionId) throws Exception {
        require(db.findCompetitionStatus(competitionId).equals("比赛中"), "竞赛不在比赛中");
    }

    private static void require(boolean valid, String message) {
        if (!valid) throw new BusinessException(message);
    }

    private static String id(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "");
    }
}
