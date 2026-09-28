package cn.edu.aviationquiz.controller;

import cn.edu.aviationquiz.entity.Models.RankingEntry;
import cn.edu.aviationquiz.entity.Models.Session;
import cn.edu.aviationquiz.exception.BusinessException;
import cn.edu.aviationquiz.service.ResultUseCases;

import java.util.List;

/** Handles result-page input and delegates ranking and archival to the service interface. */
public final class ResultController {
    private final ResultUseCases service;

    public ResultController(ResultUseCases service) {
        this.service = service;
    }

    public List<RankingEntry> ranking(Session session, String competitionId) {
        requireCompetition(competitionId);
        return service.ranking(session, competitionId);
    }

    public List<RankingEntry> preview(Session session, String competitionId) {
        requireCompetition(competitionId);
        return service.preview(session, competitionId);
    }

    public void archive(Session session, String competitionId) {
        requireCompetition(competitionId);
        service.archive(session, competitionId);
    }

    public String exportCsv(Session session, String competitionId) {
        requireCompetition(competitionId);
        return service.exportCsv(session, competitionId);
    }

    private static void requireCompetition(String competitionId) {
        if (competitionId == null || competitionId.isBlank())
            throw new BusinessException("请先选择竞赛");
    }
}
