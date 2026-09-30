package cn.edu.aviationquiz.service;

import cn.edu.aviationquiz.entity.ParticipationType;
import cn.edu.aviationquiz.entity.Models.GroupView;
import cn.edu.aviationquiz.entity.Models.ParticipantView;

import java.util.List;

/** Business rules for player participation and staff grouping. */
public interface RegistrationManagementService {
    List<ParticipantView> participants(String competitionId, boolean reserved);

    List<GroupView> groups(String competitionId);

    void join(String playerId, String competitionId, ParticipationType type);

    void cancel(String playerId, String competitionId, ParticipationType type);

    String addGroup(String competitionId, String name, int sequence);

    void deleteEmptyGroup(String groupId);

    void assign(String registrationId, String groupId);
}
