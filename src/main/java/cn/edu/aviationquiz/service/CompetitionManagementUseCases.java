package cn.edu.aviationquiz.service;

import cn.edu.aviationquiz.entity.Models.CompetitionInput;
import cn.edu.aviationquiz.entity.Models.CompetitionView;
import cn.edu.aviationquiz.entity.Models.Session;

import java.util.List;

/** Competition setup operations exposed to the management controller. */
public interface CompetitionManagementUseCases {
    List<CompetitionView> competitions();

    List<CompetitionView> competitionsForPlayer(Session session);

    List<CompetitionView> participatedCompetitions(Session session);

    String saveCompetition(Session session, String existingId, CompetitionInput input);

    void registrationState(Session session, String competitionId, String targetState);
}
