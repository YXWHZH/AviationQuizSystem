package cn.edu.aviationquiz.service;

import cn.edu.aviationquiz.entity.Models.CompetitionInput;
import cn.edu.aviationquiz.entity.Models.CompetitionView;

import java.util.List;

/** Business rules for competition setup and registration-state transitions. */
public interface CompetitionManagementService {
    List<CompetitionView> competitions();

    List<CompetitionView> competitionsForPlayer(String playerId);

    List<CompetitionView> participatedCompetitions(String playerId);

    String save(String existingId, CompetitionInput input);

    void changeRegistrationState(String competitionId, String targetState);
}
