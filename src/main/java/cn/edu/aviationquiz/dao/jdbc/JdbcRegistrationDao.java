package cn.edu.aviationquiz.dao.jdbc;

import cn.edu.aviationquiz.dao.RegistrationDao;
import cn.edu.aviationquiz.dao.Store;
import cn.edu.aviationquiz.dao.Store.Row;
import cn.edu.aviationquiz.dao.Store.UnitOfWork;
import cn.edu.aviationquiz.entity.CompetitionRegistrationContext;
import cn.edu.aviationquiz.entity.ParticipationRecord;
import cn.edu.aviationquiz.entity.ParticipationType;
import cn.edu.aviationquiz.entity.RegistrationRecord;

import java.util.Optional;

/** SQLite implementation of registration and grouping persistence. */
public final class JdbcRegistrationDao implements RegistrationDao {
    private final Store store;

    public JdbcRegistrationDao(Store store) {
        this.store = store;
    }

    @Override
    public <T> T inTransaction(Work<T> work) {
        return store.transaction(db -> work.run(new JdbcTransaction(db)));
    }

    private static final class JdbcTransaction implements Transaction {
        private final UnitOfWork db;

        private JdbcTransaction(UnitOfWork db) {
            this.db = db;
        }

        @Override
        public CompetitionRegistrationContext findCompetition(String competitionId)
                throws Exception {
            Row row =
                    db.one(
                            "SELECT id,status,register_start,register_end FROM competition WHERE id=?",
                            competitionId);
            return new CompetitionRegistrationContext(
                    row.text("id"),
                    row.text("status"),
                    row.number("register_start"),
                    row.number("register_end"));
        }

        @Override
        public boolean isPlayerProfileComplete(String playerId) throws Exception {
            return db.one("SELECT profile_complete FROM player WHERE id=?", playerId)
                            .number("profile_complete")
                    == 1;
        }

        @Override
        public Optional<ParticipationRecord> findParticipation(
                ParticipationType type, String playerId, String competitionId) throws Exception {
            var rows =
                    db.list(
                            "SELECT id,status FROM " + table(type)
                                    + " WHERE player_id=? AND competition_id=?",
                            playerId,
                            competitionId);
            return rows.isEmpty()
                    ? Optional.empty()
                    : Optional.of(
                            new ParticipationRecord(
                                    rows.getFirst().text("id"), rows.getFirst().text("status")));
        }

        @Override
        public void insertParticipation(
                ParticipationType type,
                String id,
                String playerId,
                String competitionId,
                long createdAt)
                throws Exception {
            db.execute(
                    "INSERT INTO " + table(type)
                            + "(id,player_id,competition_id,created_at,status) VALUES(?,?,?,?,'有效')",
                    id,
                    playerId,
                    competitionId,
                    createdAt);
        }

        @Override
        public void restoreParticipation(ParticipationType type, String id, long createdAt)
                throws Exception {
            db.execute(
                    "UPDATE " + table(type) + " SET status='有效',created_at=? WHERE id=?",
                    createdAt,
                    id);
        }

        @Override
        public void cancelParticipation(ParticipationType type, String id) throws Exception {
            db.execute("UPDATE " + table(type) + " SET status='已取消' WHERE id=?", id);
        }

        @Override
        public void cancelActiveReservation(String playerId, String competitionId)
                throws Exception {
            db.execute(
                    "UPDATE reservation SET status='已取消' WHERE player_id=? AND"
                            + " competition_id=? AND status='有效'",
                    playerId,
                    competitionId);
        }

        @Override
        public boolean isRegistrationAssigned(String registrationId) throws Exception {
            return db.exists(
                    "SELECT id FROM group_assignment WHERE registration_id=?", registrationId);
        }

        @Override
        public void insertGroup(String id, String competitionId, String name, int sequence)
                throws Exception {
            db.execute(
                    "INSERT INTO competition_group(id,competition_id,name,sequence_no)"
                            + " VALUES(?,?,?,?)",
                    id,
                    competitionId,
                    name,
                    sequence);
        }

        @Override
        public RegistrationRecord findActiveRegistration(String registrationId) throws Exception {
            Row row =
                    db.one(
                            "SELECT id,competition_id FROM registration WHERE id=? AND status='有效'",
                            registrationId);
            return new RegistrationRecord(row.text("id"), row.text("competition_id"));
        }

        @Override
        public boolean groupBelongsToCompetition(String groupId, String competitionId)
                throws Exception {
            return db.exists(
                    "SELECT id FROM competition_group WHERE id=? AND competition_id=?",
                    groupId,
                    competitionId);
        }

        @Override
        public void insertAssignment(
                String id, String registrationId, String groupId, long createdAt) throws Exception {
            db.execute(
                    "INSERT INTO group_assignment(id,registration_id,group_id,created_at)"
                            + " VALUES(?,?,?,?)",
                    id,
                    registrationId,
                    groupId,
                    createdAt);
        }

        private static String table(ParticipationType type) {
            return type == ParticipationType.RESERVATION ? "reservation" : "registration";
        }
    }
}
