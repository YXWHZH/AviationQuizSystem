package cn.edu.aviationquiz.dao.jdbc;

import cn.edu.aviationquiz.dao.AccountDao;
import cn.edu.aviationquiz.dao.Store;
import cn.edu.aviationquiz.dao.Store.Row;
import cn.edu.aviationquiz.dao.Store.UnitOfWork;
import cn.edu.aviationquiz.entity.AccountRecord;
import cn.edu.aviationquiz.entity.Models.PlayerProfileInput;
import cn.edu.aviationquiz.entity.Models.PlayerProfileView;

import java.util.Optional;

/** SQLite implementation of account and player-profile persistence. */
public final class JdbcAccountDao implements AccountDao {
    private final Store store;

    public JdbcAccountDao(Store store) {
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
        public boolean hasStaff() throws Exception {
            return db.exists("SELECT id FROM staff");
        }

        @Override
        public boolean usernameExists(boolean staff, String username) throws Exception {
            return db.exists(
                    "SELECT id FROM " + table(staff) + " WHERE username=?", username);
        }

        @Override
        public boolean playerIdentityExists(
                String school, String studentNumber, String excludedPlayerId) throws Exception {
            if (excludedPlayerId == null)
                return db.exists(
                        "SELECT id FROM player WHERE school=? AND student_number=?",
                        school,
                        studentNumber);
            return db.exists(
                    "SELECT id FROM player WHERE school=? AND student_number=? AND id<>?",
                    school,
                    studentNumber,
                    excludedPlayerId);
        }

        @Override
        public void insertStaff(String id, String username, String passwordHash, String name)
                throws Exception {
            db.execute("INSERT INTO staff VALUES(?,?,?,?)", id, username, passwordHash, name);
        }

        @Override
        public void insertPlayer(
                String id,
                String username,
                String passwordHash,
                PlayerProfileInput profile)
                throws Exception {
            db.execute(
                    "INSERT INTO player VALUES(?,?,?,?,?,?,?,?,?,1)",
                    id,
                    username,
                    passwordHash,
                    profile.name(),
                    profile.phone(),
                    profile.school(),
                    profile.college(),
                    profile.major(),
                    profile.studentNumber());
        }

        @Override
        public Optional<AccountRecord> findAccount(boolean staff, String username)
                throws Exception {
            var rows =
                    db.list(
                            "SELECT id,username,password_hash,name FROM "
                                    + table(staff)
                                    + " WHERE username=?",
                            username);
            if (rows.isEmpty()) return Optional.empty();
            Row row = rows.getFirst();
            return Optional.of(
                    new AccountRecord(
                            row.text("id"),
                            row.text("username"),
                            row.text("password_hash"),
                            row.text("name"),
                            staff));
        }

        @Override
        public boolean isPlayerProfileComplete(String playerId) throws Exception {
            return db.one("SELECT profile_complete FROM player WHERE id=?", playerId)
                            .number("profile_complete")
                    == 1;
        }

        @Override
        public PlayerProfileView findPlayerProfile(String playerId) throws Exception {
            Row row =
                    db.one(
                            "SELECT username,name,phone,school,college,major,student_number,profile_complete FROM player WHERE id=?",
                            playerId);
            return new PlayerProfileView(
                    row.text("username"),
                    row.text("name"),
                    row.text("phone"),
                    row.text("school"),
                    row.text("college"),
                    row.text("major"),
                    row.text("student_number"),
                    row.number("profile_complete") == 1);
        }

        @Override
        public void updatePlayerProfile(String playerId, PlayerProfileInput profile)
                throws Exception {
            db.execute(
                    "UPDATE player SET school=?,college=?,major=?,student_number=?,name=?,phone=?,profile_complete=1 WHERE id=?",
                    profile.school(),
                    profile.college(),
                    profile.major(),
                    profile.studentNumber(),
                    profile.name(),
                    profile.phone(),
                    playerId);
        }

        private static String table(boolean staff) {
            return staff ? "staff" : "player";
        }
    }
}
