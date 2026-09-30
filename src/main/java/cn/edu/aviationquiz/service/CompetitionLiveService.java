package cn.edu.aviationquiz.service;

import cn.edu.aviationquiz.entity.Models.ActiveReleaseView;
import cn.edu.aviationquiz.entity.Models.CompetitionProgressView;
import cn.edu.aviationquiz.entity.Models.PlayerSubmissionView;
import cn.edu.aviationquiz.entity.Models.PublishedQuestionView;

import java.util.List;

/** Business rules for starting, conducting and recovering a live competition. */
public interface CompetitionLiveService {
    void startCompetition(String competitionId);

    CompetitionProgressView progress(String competitionId);

    int completedQuestions(String competitionId);

    void startRound(String competitionId);

    String publish(String competitionId);

    boolean recover();

    ActiveReleaseView activeRelease(String competitionId);

    void closeQuestion(String competitionId);

    void finishRound(String competitionId);

    PublishedQuestionView room(String playerId, String competitionId);

    List<PlayerSubmissionView> monitor(String competitionId);
}
