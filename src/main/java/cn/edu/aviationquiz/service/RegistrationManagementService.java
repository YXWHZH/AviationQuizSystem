package cn.edu.aviationquiz.service;

import cn.edu.aviationquiz.entity.ParticipationType;

/** Business rules for player participation and staff grouping. */
public interface RegistrationManagementService {
    void join(String playerId, String competitionId, ParticipationType type);

    void cancel(String playerId, String competitionId, ParticipationType type);

    String addGroup(String competitionId, String name, int sequence);

    void assign(String registrationId, String groupId);
}
