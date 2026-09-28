package cn.edu.aviationquiz.dao;

import cn.edu.aviationquiz.entity.CompetitionRegistrationContext;
import cn.edu.aviationquiz.entity.ParticipationRecord;
import cn.edu.aviationquiz.entity.ParticipationType;
import cn.edu.aviationquiz.entity.RegistrationRecord;

import java.util.Optional;

/** Data-access contract for reservations, registrations, groups and assignments. */
public interface RegistrationDao {
    <T> T inTransaction(Work<T> work);

    @FunctionalInterface
    interface Work<T> {
        T run(Transaction transaction) throws Exception;
    }

    interface Transaction {
        CompetitionRegistrationContext findCompetition(String competitionId) throws Exception;

        boolean isPlayerProfileComplete(String playerId) throws Exception;

        Optional<ParticipationRecord> findParticipation(
                ParticipationType type, String playerId, String competitionId) throws Exception;

        void insertParticipation(
                ParticipationType type,
                String id,
                String playerId,
                String competitionId,
                long createdAt)
                throws Exception;

        void restoreParticipation(ParticipationType type, String id, long createdAt)
                throws Exception;

        void cancelParticipation(ParticipationType type, String id) throws Exception;

        void cancelActiveReservation(String playerId, String competitionId) throws Exception;

        boolean isRegistrationAssigned(String registrationId) throws Exception;

        void insertGroup(
                String id, String competitionId, String name, int sequence) throws Exception;

        RegistrationRecord findActiveRegistration(String registrationId) throws Exception;

        boolean groupBelongsToCompetition(String groupId, String competitionId) throws Exception;

        void insertAssignment(
                String id, String registrationId, String groupId, long createdAt) throws Exception;
    }
}
