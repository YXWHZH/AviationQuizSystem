package cn.edu.aviationquiz.dao.jdbc;

import cn.edu.aviationquiz.dao.GameDao;
import cn.edu.aviationquiz.dao.Store;
import cn.edu.aviationquiz.dao.Store.Row;
import cn.edu.aviationquiz.dao.Store.UnitOfWork;
import cn.edu.aviationquiz.entity.AnswerRecord;
import cn.edu.aviationquiz.entity.AnswerSubmissionContext;

/** SQLite implementation of the live competition data-access contract. */
public final class JdbcGameDao implements GameDao {
    private static final String ANSWER_CONTEXT_SQL =
            "SELECT qr.id release_id,qr.started_at,qr.deadline,qr.closed_at,gr.group_id,"
                    + "r.competition_id,r.round_type,q.correct_answer,c.status competition_status "
                    + "FROM question_release qr "
                    + "JOIN group_round gr ON gr.id=qr.group_round_id "
                    + "JOIN competition_round r ON r.id=gr.round_id "
                    + "JOIN competition c ON c.id=r.competition_id "
                    + "JOIN round_question rq ON rq.id=qr.round_question_id "
                    + "JOIN question q ON q.id=rq.question_id WHERE qr.id=?";

    private final Store store;

    public JdbcGameDao(Store store) {
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
        public AnswerSubmissionContext findAnswerContext(String releaseId) throws Exception {
            Row row = db.one(ANSWER_CONTEXT_SQL, releaseId);
            return new AnswerSubmissionContext(
                    row.text("release_id"),
                    row.text("competition_id"),
                    row.text("competition_status"),
                    row.text("group_id"),
                    row.text("round_type"),
                    row.text("correct_answer"),
                    row.number("started_at"),
                    row.number("deadline"),
                    row.nil("closed_at") ? null : row.number("closed_at"));
        }

        @Override
        public boolean isPlayerAssignedToGroup(String groupId, String playerId) throws Exception {
            return db.exists(
                    "SELECT 1 FROM group_assignment a JOIN registration r ON"
                            + " r.id=a.registration_id WHERE a.group_id=? AND r.player_id=?"
                            + " AND r.status='有效'",
                    groupId,
                    playerId);
        }

        @Override
        public boolean hasSettlement(String releaseId, String playerId) throws Exception {
            return db.exists(
                    "SELECT 1 WHERE EXISTS(SELECT 1 FROM answer_record WHERE release_id=? AND"
                            + " player_id=?) OR EXISTS(SELECT 1 FROM timeout_record WHERE"
                            + " release_id=? AND player_id=?)",
                    releaseId,
                    playerId,
                    releaseId,
                    playerId);
        }

        @Override
        public void saveAnswer(AnswerRecord answer) throws Exception {
            db.execute(
                    "INSERT INTO answer_record"
                            + "(id,release_id,player_id,user_answer,correct,score_change,submitted_at)"
                            + " VALUES(?,?,?,?,?,?,?)",
                    answer.id(),
                    answer.releaseId(),
                    answer.playerId(),
                    answer.option(),
                    answer.correct() ? 1 : 0,
                    answer.scoreChange(),
                    answer.submittedAt());
        }
    }
}
