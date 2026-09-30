package cn.edu.aviationquiz.controller;

import cn.edu.aviationquiz.entity.Models.ActiveReleaseView;
import cn.edu.aviationquiz.entity.Models.CompetitionProgressView;
import cn.edu.aviationquiz.entity.Models.PlayerSubmissionView;
import cn.edu.aviationquiz.entity.Models.PublishedQuestionView;
import cn.edu.aviationquiz.entity.Models.Session;
import cn.edu.aviationquiz.exception.BusinessException;
import cn.edu.aviationquiz.service.CompetitionLiveUseCases;

import java.util.List;

/** Validates live-control input and delegates the workflow to its use-case boundary. */
public final class CompetitionLiveController {
    private final CompetitionLiveUseCases service;

    public CompetitionLiveController(CompetitionLiveUseCases service) {
        this.service = service;
    }

    public void startCompetition(Session session, String competitionId) {
        requireCompetition(competitionId);
        service.startCompetition(session, competitionId);
    }

    public CompetitionProgressView progress(Session session, String competitionId) {
        requireCompetition(competitionId);
        return service.progress(session, competitionId);
    }

    public int completedQuestions(Session session, String competitionId) {
        requireCompetition(competitionId);
        return service.completedQuestions(session, competitionId);
    }

    public void startRound(Session session, String competitionId) {
        requireCompetition(competitionId);
        service.startRound(session, competitionId);
    }

    public String publish(Session session, String competitionId) {
        requireCompetition(competitionId);
        return service.publish(session, competitionId);
    }

    public ActiveReleaseView activeRelease(Session session, String competitionId) {
        requireCompetition(competitionId);
        return service.activeRelease(session, competitionId);
    }

    public void closeQuestion(Session session, String competitionId) {
        requireCompetition(competitionId);
        service.closeQuestion(session, competitionId);
    }

    public void finishRound(Session session, String competitionId) {
        requireCompetition(competitionId);
        service.finishRound(session, competitionId);
    }

    public PublishedQuestionView room(Session session, String competitionId) {
        requireCompetition(competitionId);
        return service.room(session, competitionId);
    }

    public List<PlayerSubmissionView> monitor(Session session, String competitionId) {
        requireCompetition(competitionId);
        return service.monitor(session, competitionId);
    }

    private static void requireCompetition(String competitionId) {
        if (competitionId == null || competitionId.isBlank()) {
            throw new BusinessException("请先选择竞赛");
        }
    }
}
