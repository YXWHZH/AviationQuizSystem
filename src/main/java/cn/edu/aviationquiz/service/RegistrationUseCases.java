package cn.edu.aviationquiz.service;

import cn.edu.aviationquiz.entity.Models.Session;
import cn.edu.aviationquiz.entity.Models.GroupView;
import cn.edu.aviationquiz.entity.Models.ParticipantView;

import java.util.List;

/** Authenticated registration and grouping operations exposed to the controller. */
public interface RegistrationUseCases {
    List<ParticipantView> participants(
            Session session, String competitionId, boolean reserved);

    List<GroupView> groups(Session session, String competitionId);

    void join(Session session, String competitionId, boolean reservation);

    void cancelParticipation(Session session, String competitionId, boolean reservation);

    String addGroup(Session session, String competitionId, String name, int sequence);

    void deleteEmptyGroup(Session session, String groupId);

    void assign(Session session, String registrationId, String groupId);
}
