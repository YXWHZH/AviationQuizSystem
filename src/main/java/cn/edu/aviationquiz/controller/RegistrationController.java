package cn.edu.aviationquiz.controller;

import cn.edu.aviationquiz.entity.Models.Session;
import cn.edu.aviationquiz.entity.Models.GroupView;
import cn.edu.aviationquiz.entity.Models.ParticipantView;
import cn.edu.aviationquiz.exception.BusinessException;
import cn.edu.aviationquiz.service.RegistrationUseCases;

import java.util.List;

/** Handles registration and grouping input before calling the service interface. */
public final class RegistrationController {
    private final RegistrationUseCases service;

    public RegistrationController(RegistrationUseCases service) {
        this.service = service;
    }

    public List<ParticipantView> participants(
            Session session, String competitionId, boolean reserved) {
        requireId(competitionId, "请先选择竞赛");
        return service.participants(session, competitionId, reserved);
    }

    public List<GroupView> groups(Session session, String competitionId) {
        requireId(competitionId, "请先选择竞赛");
        return service.groups(session, competitionId);
    }

    public void join(Session session, String competitionId, boolean reservation) {
        requireId(competitionId, "请先选择竞赛");
        service.join(session, competitionId, reservation);
    }

    public void cancel(Session session, String competitionId, boolean reservation) {
        requireId(competitionId, "请先选择竞赛");
        service.cancelParticipation(session, competitionId, reservation);
    }

    public String addGroup(
            Session session, String competitionId, String name, int sequence) {
        requireId(competitionId, "请先选择竞赛");
        if (name == null || name.isBlank()) throw new BusinessException("组名不能为空");
        return service.addGroup(session, competitionId, name, sequence);
    }

    public void deleteEmptyGroup(Session session, String groupId) {
        requireId(groupId, "请选择小组");
        service.deleteEmptyGroup(session, groupId);
    }

    public void assign(
            Session session, String registrationId, String groupId) {
        requireId(registrationId, "请先选择报名选手");
        requireId(groupId, "请选择小组");
        service.assign(session, registrationId, groupId);
    }

    private static void requireId(String value, String message) {
        if (value == null || value.isBlank()) throw new BusinessException(message);
    }
}
