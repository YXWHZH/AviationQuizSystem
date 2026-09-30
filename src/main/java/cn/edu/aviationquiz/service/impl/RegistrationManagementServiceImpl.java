package cn.edu.aviationquiz.service.impl;

import cn.edu.aviationquiz.dao.RegistrationDao;
import cn.edu.aviationquiz.entity.CompetitionRegistrationContext;
import cn.edu.aviationquiz.entity.ParticipationType;
import cn.edu.aviationquiz.entity.Models.GroupView;
import cn.edu.aviationquiz.entity.Models.ParticipantView;
import cn.edu.aviationquiz.exception.BusinessException;
import cn.edu.aviationquiz.service.RegistrationManagementService;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/** Default registration and grouping rules, independent of JavaFX and JDBC. */
public final class RegistrationManagementServiceImpl
        implements RegistrationManagementService {
    private final RegistrationDao registrationDao;
    private final Clock clock;

    public RegistrationManagementServiceImpl(RegistrationDao registrationDao, Clock clock) {
        this.registrationDao = registrationDao;
        this.clock = clock;
    }

    @Override
    public List<ParticipantView> participants(String competitionId, boolean reserved) {
        return registrationDao.inTransaction(
                db -> db.findParticipants(competitionId, reserved));
    }

    @Override
    public List<GroupView> groups(String competitionId) {
        return registrationDao.inTransaction(db -> db.findGroups(competitionId));
    }

    @Override
    public void join(String playerId, String competitionId, ParticipationType type) {
        registrationDao.inTransaction(
                db -> {
                    require(db.isPlayerProfileComplete(playerId), "请先完善学校、学院、专业和学号资料");
                    CompetitionRegistrationContext competition =
                            db.findCompetition(competitionId);
                    validateJoinWindow(competition, type);
                    long now = clock.millis();
                    var existing = db.findParticipation(type, playerId, competitionId);
                    if (existing.isEmpty())
                        db.insertParticipation(
                                type,
                                id(type == ParticipationType.RESERVATION ? "V" : "R"),
                                playerId,
                                competitionId,
                                now);
                    else {
                        require(
                                existing.get().status().equals("已取消"),
                                type == ParticipationType.RESERVATION
                                        ? "您已经预约，无需重复提交"
                                        : "您已经报名，无需重复提交");
                        db.restoreParticipation(type, existing.get().id(), now);
                    }
                    if (type == ParticipationType.REGISTRATION)
                        db.cancelActiveReservation(playerId, competitionId);
                    return null;
                });
    }

    @Override
    public void cancel(String playerId, String competitionId, ParticipationType type) {
        registrationDao.inTransaction(
                db -> {
                    CompetitionRegistrationContext competition =
                            db.findCompetition(competitionId);
                    var record =
                            db.findParticipation(type, playerId, competitionId)
                                    .orElseThrow(
                                            () ->
                                                    new BusinessException(
                                                            type == ParticipationType.RESERVATION
                                                                    ? "当前没有有效预约"
                                                                    : "当前没有有效报名"));
                    require(
                            record.status().equals("有效"),
                            type == ParticipationType.RESERVATION
                                    ? "当前没有有效预约"
                                    : "当前没有有效报名");
                    long now = clock.millis();
                    if (type == ParticipationType.RESERVATION)
                        require(
                                competition.status().equals("未开放"),
                                "只有未开放竞赛允许取消预约");
                    else {
                        require(
                                competition.status().equals("报名中")
                                        && now >= competition.registerStart()
                                        && now < competition.registerEnd(),
                                "只能在报名时间内取消报名");
                        require(
                                !db.isRegistrationAssigned(record.id()), "已分组后不能取消报名");
                    }
                    db.cancelParticipation(type, record.id());
                    return null;
                });
    }

    @Override
    public String addGroup(String competitionId, String name, int sequence) {
        return registrationDao.inTransaction(
                db -> {
                    CompetitionRegistrationContext competition =
                            db.findCompetition(competitionId);
                    requireEditable(competition);
                    require(competition.status().equals("报名截止"), "报名截止后才能分组");
                    require(sequence > 0, "出场顺序必须大于 0");
                    String groupId = id("G");
                    db.insertGroup(groupId, competitionId, bounded(name, 1, 30, "组名"), sequence);
                    return groupId;
                });
    }

    @Override
    public void deleteEmptyGroup(String groupId) {
        registrationDao.inTransaction(
                db -> {
                    GroupView group = db.findGroup(groupId);
                    requireEditable(db.findCompetition(group.competitionId()));
                    require(!db.groupHasAssignment(groupId), "只能删除尚未分配选手的空小组");
                    db.deleteGroup(groupId);
                    return null;
                });
    }

    @Override
    public void assign(String registrationId, String groupId) {
        registrationDao.inTransaction(
                db -> {
                    var registration = db.findActiveRegistration(registrationId);
                    CompetitionRegistrationContext competition =
                            db.findCompetition(registration.competitionId());
                    requireEditable(competition);
                    require(competition.status().equals("报名截止"), "报名截止后才能分组");
                    require(
                            db.groupBelongsToCompetition(
                                    groupId, registration.competitionId()),
                            "小组与报名不属于同场竞赛");
                    require(!db.isRegistrationAssigned(registrationId), "该选手已经分组");
                    db.insertAssignment(id("GA"), registrationId, groupId, clock.millis());
                    return null;
                });
    }

    private void validateJoinWindow(
            CompetitionRegistrationContext competition, ParticipationType type) {
        if (type == ParticipationType.RESERVATION) {
            require(competition.status().equals("未开放"), "只有未开放竞赛允许预约");
            return;
        }
        long now = clock.millis();
        require(
                competition.status().equals("报名中")
                        && now >= competition.registerStart()
                        && now < competition.registerEnd(),
                "报名尚未开放或已经截止");
    }

    private static void requireEditable(CompetitionRegistrationContext competition) {
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

    private static String id(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "");
    }
}
