package cn.edu.aviationquiz.controller;

import cn.edu.aviationquiz.entity.Models.Session;
import cn.edu.aviationquiz.exception.BusinessException;
import cn.edu.aviationquiz.service.CompetitionRoomService;

/** Handles competition-room input and delegates business rules to the service interface. */
public final class CompetitionRoomController {
    private final CompetitionRoomService service;

    public CompetitionRoomController(CompetitionRoomService service) {
        this.service = service;
    }

    public int submitAnswer(Session session, String releaseId, String option) {
        if (releaseId == null || releaseId.isBlank())
            throw new BusinessException("当前没有可提交的题目");
        if (option == null || option.isBlank()) throw new BusinessException("请选择一个选项");
        return service.submit(session, releaseId, option);
    }
}
