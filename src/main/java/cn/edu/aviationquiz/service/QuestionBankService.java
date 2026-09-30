package cn.edu.aviationquiz.service;

import cn.edu.aviationquiz.entity.Models.QuestionInput;

/** Business rules for questions, competition rounds and round question lists. */
public interface QuestionBankService {
    String saveQuestion(String existingId, QuestionInput input);

    void setQuestionActive(String questionId, boolean active);

    void deleteQuestion(String questionId);

    String addRound(
            String competitionId, String name, String type, int sequence, int seconds);

    void updateRound(
            String roundId, String name, String type, int sequence, int seconds);

    void deleteEmptyRound(String roundId);

    void addQuestionToRound(String roundId, String questionId, int sequence);

    void removeRoundQuestion(String roundQuestionId);
}
