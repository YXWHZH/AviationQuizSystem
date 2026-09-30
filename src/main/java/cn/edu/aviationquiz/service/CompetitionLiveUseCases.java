package cn.edu.aviationquiz.service;

import cn.edu.aviationquiz.entity.Models.ActiveReleaseView;
import cn.edu.aviationquiz.entity.Models.CompetitionProgressView;
import cn.edu.aviationquiz.entity.Models.PlayerSubmissionView;
import cn.edu.aviationquiz.entity.Models.PublishedQuestionView;
import cn.edu.aviationquiz.entity.Models.Session;

import java.util.List;

/** Live competition commands and queries exposed to the presentation layer. */
public interface CompetitionLiveUseCases {
    void startCompetition(Session session, String competitionId);

    CompetitionProgressView progress(Session session, String competitionId);

    int completedQuestions(Session session, String competitionId);

    void startRound(Session session, String competitionId);

    String publish(Session session, String competitionId);

    ActiveReleaseView activeRelease(Session session, String competitionId);

    void closeQuestion(Session session, String competitionId);

    void finishRound(Session session, String competitionId);

    PublishedQuestionView room(Session session, String competitionId);

    List<PlayerSubmissionView> monitor(Session session, String competitionId);
}
