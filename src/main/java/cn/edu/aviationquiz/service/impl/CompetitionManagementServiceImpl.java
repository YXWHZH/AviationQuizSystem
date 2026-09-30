package cn.edu.aviationquiz.service.impl;

import cn.edu.aviationquiz.dao.CompetitionDao;
import cn.edu.aviationquiz.entity.CompetitionSetupContext;
import cn.edu.aviationquiz.entity.Models.CompetitionInput;
import cn.edu.aviationquiz.entity.Models.CompetitionView;
import cn.edu.aviationquiz.exception.BusinessException;
import cn.edu.aviationquiz.service.CompetitionManagementService;

import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Default competition setup rules, independent of JavaFX and JDBC. */
public final class CompetitionManagementServiceImpl
        implements CompetitionManagementService {
    private static final Set<String> CATEGORIES =
            Set.of("民航史", "飞行原理", "航空法规");
    private final CompetitionDao competitionDao;
    private final Clock clock;

    public CompetitionManagementServiceImpl(CompetitionDao competitionDao, Clock clock) {
        this.competitionDao = competitionDao;
        this.clock = clock;
    }

    @Override
    public List<CompetitionView> competitions() {
        return competitionDao.inTransaction(CompetitionDao.Transaction::findAll);
    }

    @Override
    public List<CompetitionView> competitionsForPlayer(String playerId) {
        return competitionDao.inTransaction(db -> db.findAllForPlayer(playerId));
    }

    @Override
    public List<CompetitionView> participatedCompetitions(String playerId) {
        return competitionDao.inTransaction(db -> db.findParticipatedByPlayer(playerId));
    }

    @Override
    public String save(String existingId, CompetitionInput input) {
        return competitionDao.inTransaction(
                db -> {
                    require(input != null, "请填写竞赛信息");
                    Set<String> categories =
                            input.categories() == null
                                    ? Set.of()
                                    : Set.copyOf(input.categories());
                    CompetitionInput validInput =
                            new CompetitionInput(
                                    bounded(input.name(), 1, 50, "竞赛名称"),
                                    bounded(input.description(), 0, 200, "简介"),
                                    input.registerStart(),
                                    input.registerEnd(),
                                    input.competitionTime(),
                                    input.quota(),
                                    categories);
                    validate(validInput);
                    String competitionId = existingId == null ? id() : existingId;
                    if (existingId == null) db.insertCompetition(competitionId, validInput);
                    else {
                        CompetitionSetupContext competition =
                                db.findCompetition(competitionId);
                        requireEditable(competition);
                        for (String used : db.findUsedQuestionCategories(competitionId))
                            require(
                                    categories.contains(used),
                                    "现有题单仍在使用“" + used + "”题目，请先调整题单");
                        db.updateCompetition(competitionId, validInput);
                    }
                    db.replaceCategories(competitionId, categories);
                    return competitionId;
                });
    }

    @Override
    public void changeRegistrationState(String competitionId, String targetState) {
        competitionDao.inTransaction(
                db -> {
                    CompetitionSetupContext competition =
                            db.findCompetition(competitionId);
                    String oldState = competition.status();
                    require(
                            (oldState.equals("未开放") && targetState.equals("报名中"))
                                    || (oldState.equals("报名中")
                                            && targetState.equals("报名截止")),
                            "只允许依次开放报名、截止报名");
                    long now = clock.millis();
                    if (targetState.equals("报名中"))
                        require(
                                now >= competition.registerStart()
                                        && now < competition.registerEnd(),
                                "当前不在报名时间范围");
                    if (targetState.equals("报名截止"))
                        require(now >= competition.registerEnd(), "尚未到报名截止时间");
                    db.updateStatus(competitionId, targetState);
                    return null;
                });
    }

    private static void validate(CompetitionInput input) {
        require(
                input.registerStart() < input.registerEnd()
                        && input.registerEnd() < input.competitionTime(),
                "时间顺序必须为报名开始 < 报名截止 < 比赛时间");
        require(input.quota() > 0, "晋级名额必须大于 0");
        require(
                !input.categories().isEmpty()
                        && CATEGORIES.containsAll(input.categories()),
                "请至少选择一个有效竞赛分类");
    }

    private static void requireEditable(CompetitionSetupContext competition) {
        require(
                !List.of("比赛中", "已结束").contains(competition.status()),
                "开赛后配置已锁定");
    }

    private static String bounded(String text, int min, int max, String label) {
        String value = text == null ? "" : text.trim();
        require(
                value.length() >= min && value.length() <= max,
                label + "长度须为 " + min + "～" + max);
        return value;
    }

    private static void require(boolean valid, String message) {
        if (!valid) throw new BusinessException(message);
    }

    private static String id() {
        return "C" + UUID.randomUUID().toString().replace("-", "");
    }
}
