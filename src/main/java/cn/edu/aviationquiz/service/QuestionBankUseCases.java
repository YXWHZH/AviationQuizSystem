package cn.edu.aviationquiz.service;

import cn.edu.aviationquiz.entity.Models.QuestionInput;
import cn.edu.aviationquiz.entity.Models.QuestionView;
import cn.edu.aviationquiz.entity.Models.RoundQuestionView;
import cn.edu.aviationquiz.entity.Models.RoundView;
import cn.edu.aviationquiz.entity.Models.Session;

import java.util.List;

/** Question-bank and round-list mutations exposed to the controller. */
public interface QuestionBankUseCases {
    List<QuestionView> questions(Session session);

    List<QuestionView> questionsForCompetition(Session session, String competitionId);

    List<RoundView> rounds(Session session, String competitionId);

    List<RoundQuestionView> roundQuestions(Session session, String roundId);

    String saveQuestion(Session session, String existingId, QuestionInput input);

    void questionState(Session session, String questionId, boolean active);

    void deleteQuestion(Session session, String questionId);

    String addRound(
            Session session,
            String competitionId,
            String name,
            String type,
            int sequence,
            int seconds);

    void updateRound(
            Session session,
            String roundId,
            String name,
            String type,
            int sequence,
            int seconds);

    void deleteEmptyRound(Session session, String roundId);

    void addQuestionToRound(
            Session session, String roundId, String questionId, int sequence);

    void removeRoundQuestion(Session session, String roundQuestionId);
}
