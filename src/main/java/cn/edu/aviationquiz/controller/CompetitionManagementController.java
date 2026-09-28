package cn.edu.aviationquiz.controller;

import cn.edu.aviationquiz.entity.Models.CompetitionInput;
import cn.edu.aviationquiz.entity.Models.Session;
import cn.edu.aviationquiz.exception.BusinessException;
import cn.edu.aviationquiz.service.CompetitionManagementUseCases;

/** Handles competition-setup input before calling the management use-case interface. */
public final class CompetitionManagementController {
    private final CompetitionManagementUseCases service;

    public CompetitionManagementController(CompetitionManagementUseCases service) {
        this.service = service;
    }

    public String save(
            Session session, String existingId, CompetitionInput input) {
        if (input == null) throw new BusinessException("请填写竞赛信息");
        return service.saveCompetition(session, existingId, input);
    }

    public void changeRegistrationState(
            Session session, String competitionId, String targetState) {
        if (competitionId == null || competitionId.isBlank())
            throw new BusinessException("请先选择竞赛");
        if (targetState == null || targetState.isBlank())
            throw new BusinessException("目标状态不能为空");
        service.registrationState(session, competitionId, targetState);
    }
}
