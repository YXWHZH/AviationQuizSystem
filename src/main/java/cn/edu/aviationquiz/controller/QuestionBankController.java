package cn.edu.aviationquiz.controller;

import cn.edu.aviationquiz.entity.Models.QuestionInput;
import cn.edu.aviationquiz.entity.Models.QuestionView;
import cn.edu.aviationquiz.entity.Models.RoundQuestionView;
import cn.edu.aviationquiz.entity.Models.RoundView;
import cn.edu.aviationquiz.entity.Models.Session;
import cn.edu.aviationquiz.exception.BusinessException;
import cn.edu.aviationquiz.service.QuestionBankUseCases;

import java.util.List;

/** Handles question-bank and round-list input before invoking use cases. */
public final class QuestionBankController {
    private final QuestionBankUseCases service;

    public QuestionBankController(QuestionBankUseCases service) {
        this.service = service;
    }

    public List<QuestionView> questions(Session session) {
        return service.questions(session);
    }

    public List<QuestionView> questionsForCompetition(
            Session session, String competitionId) {
        requireId(competitionId, "请先选择竞赛");
        return service.questionsForCompetition(session, competitionId);
    }

    public List<RoundView> rounds(Session session, String competitionId) {
        requireId(competitionId, "请先选择竞赛");
        return service.rounds(session, competitionId);
    }

    public List<RoundQuestionView> roundQuestions(Session session, String roundId) {
        requireId(roundId, "请先选择轮次");
        return service.roundQuestions(session, roundId);
    }

    public String saveQuestion(
            Session session, String existingId, QuestionInput input) {
        if (input == null) throw new BusinessException("请填写题目信息");
        return service.saveQuestion(session, existingId, input);
    }

    public void setQuestionActive(Session session, String questionId, boolean active) {
        requireId(questionId, "请先选择题目");
        service.questionState(session, questionId, active);
    }

    public void deleteQuestion(Session session, String questionId) {
        requireId(questionId, "请先选择题目");
        service.deleteQuestion(session, questionId);
    }

    public String addRound(
            Session session,
            String competitionId,
            String name,
            String type,
            int sequence,
            int seconds) {
        requireId(competitionId, "请先选择竞赛");
        return service.addRound(session, competitionId, name, type, sequence, seconds);
    }

    public void updateRound(
            Session session,
            String roundId,
            String name,
            String type,
            int sequence,
            int seconds) {
        requireId(roundId, "请先选择轮次");
        service.updateRound(session, roundId, name, type, sequence, seconds);
    }

    public void deleteEmptyRound(Session session, String roundId) {
        requireId(roundId, "请先选择轮次");
        service.deleteEmptyRound(session, roundId);
    }

    public void addQuestionToRound(
            Session session, String roundId, String questionId, int sequence) {
        requireId(roundId, "请先选择轮次");
        requireId(questionId, "请选择题目");
        service.addQuestionToRound(session, roundId, questionId, sequence);
    }

    public void removeRoundQuestion(Session session, String roundQuestionId) {
        requireId(roundQuestionId, "请先选择题单中的题目");
        service.removeRoundQuestion(session, roundQuestionId);
    }

    private static void requireId(String value, String message) {
        if (value == null || value.isBlank()) throw new BusinessException(message);
    }
}
