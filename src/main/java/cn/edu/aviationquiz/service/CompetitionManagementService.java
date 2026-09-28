package cn.edu.aviationquiz.service;

import cn.edu.aviationquiz.entity.Models.CompetitionInput;

/** Business rules for competition setup and registration-state transitions. */
public interface CompetitionManagementService {
    String save(String existingId, CompetitionInput input);

    void changeRegistrationState(String competitionId, String targetState);
}
