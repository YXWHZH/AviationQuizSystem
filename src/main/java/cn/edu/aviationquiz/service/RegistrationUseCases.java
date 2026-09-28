package cn.edu.aviationquiz.service;

import cn.edu.aviationquiz.entity.Models.Session;

/** Authenticated registration and grouping operations exposed to the controller. */
public interface RegistrationUseCases {
    void join(Session session, String competitionId, boolean reservation);

    void cancelParticipation(Session session, String competitionId, boolean reservation);

    String addGroup(Session session, String competitionId, String name, int sequence);

    void assign(Session session, String registrationId, String groupId);
}
