package cn.edu.aviationquiz.service;

import cn.edu.aviationquiz.entity.Models.CompetitionInput;
import cn.edu.aviationquiz.entity.Models.Session;

/** Competition setup operations exposed to the management controller. */
public interface CompetitionManagementUseCases {
    String saveCompetition(Session session, String existingId, CompetitionInput input);

    void registrationState(Session session, String competitionId, String targetState);
}
