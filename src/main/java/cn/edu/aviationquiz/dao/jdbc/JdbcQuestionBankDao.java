package cn.edu.aviationquiz.dao.jdbc;

import cn.edu.aviationquiz.dao.QuestionBankDao;
import cn.edu.aviationquiz.dao.Store;
import cn.edu.aviationquiz.dao.Store.UnitOfWork;
import cn.edu.aviationquiz.entity.Models.QuestionInput;

/** SQLite implementation of question-bank and round-list persistence. */
public final class JdbcQuestionBankDao implements QuestionBankDao {
    private final Store store;

    public JdbcQuestionBankDao(Store store) {
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
        public String findCompetitionStatus(String competitionId) throws Exception {
            return db.one("SELECT status FROM competition WHERE id=?", competitionId)
                    .text("status");
        }

        @Override
        public String findRoundCompetitionId(String roundId) throws Exception {
            return db.one(
                            "SELECT competition_id FROM competition_round WHERE id=?",
                            roundId)
                    .text("competition_id");
        }

        @Override
        public String findRoundQuestionCompetitionId(String roundQuestionId)
                throws Exception {
            return db.one(
                            "SELECT r.competition_id FROM round_question rq JOIN competition_round r ON r.id=rq.round_id WHERE rq.id=?",
                            roundQuestionId)
                    .text("competition_id");
        }

        @Override
        public boolean isQuestionReferenced(String questionId) throws Exception {
            return db.exists(
                    "SELECT id FROM round_question WHERE question_id=?", questionId);
        }

        @Override
        public void insertQuestion(String questionId, QuestionInput input) throws Exception {
            db.execute(
                    "INSERT INTO question VALUES(?,?,?,?,?,?,?,?,?)",
                    questionId,
                    input.content(),
                    input.category(),
                    input.options().get(0),
                    input.options().get(1),
                    input.options().get(2),
                    input.options().get(3),
                    input.answer(),
                    input.active() ? 1 : 0);
        }

        @Override
        public void updateQuestion(String questionId, QuestionInput input) throws Exception {
            db.execute(
                    "UPDATE question SET content=?,category=?,option_a=?,option_b=?,option_c=?,option_d=?,correct_answer=?,active=? WHERE id=?",
                    input.content(),
                    input.category(),
                    input.options().get(0),
                    input.options().get(1),
                    input.options().get(2),
                    input.options().get(3),
                    input.answer(),
                    input.active() ? 1 : 0,
                    questionId);
        }

        @Override
        public void setQuestionActive(String questionId, boolean active) throws Exception {
            db.execute(
                    "UPDATE question SET active=? WHERE id=?",
                    active ? 1 : 0,
                    questionId);
        }

        @Override
        public void deleteQuestion(String questionId) throws Exception {
            db.execute("DELETE FROM question WHERE id=?", questionId);
        }

        @Override
        public void insertRound(
                String roundId,
                String competitionId,
                String name,
                String type,
                int sequence,
                int seconds)
                throws Exception {
            db.execute(
                    "INSERT INTO competition_round VALUES(?,?,?,?,?,?)",
                    roundId,
                    competitionId,
                    name,
                    type,
                    sequence,
                    seconds);
        }

        @Override
        public void updateRound(
                String roundId, String name, String type, int sequence, int seconds)
                throws Exception {
            db.execute(
                    "UPDATE competition_round SET name=?,round_type=?,sequence_no=?,time_limit=? WHERE id=?",
                    name,
                    type,
                    sequence,
                    seconds,
                    roundId);
        }

        @Override
        public boolean isRoundEmpty(String roundId) throws Exception {
            return !db.exists("SELECT id FROM round_question WHERE round_id=?", roundId);
        }

        @Override
        public void deleteRound(String roundId) throws Exception {
            db.execute("DELETE FROM competition_round WHERE id=?", roundId);
        }

        @Override
        public boolean isQuestionActive(String questionId) throws Exception {
            return db.exists(
                    "SELECT id FROM question WHERE id=? AND active=1", questionId);
        }

        @Override
        public boolean isQuestionAllowedForCompetition(
                String questionId, String competitionId) throws Exception {
            return db.exists(
                    "SELECT q.id FROM question q JOIN competition_category cc ON cc.category=q.category WHERE q.id=? AND cc.competition_id=?",
                    questionId,
                    competitionId);
        }

        @Override
        public boolean isQuestionUsedInCompetition(
                String questionId, String competitionId) throws Exception {
            return db.exists(
                    "SELECT rq.id FROM round_question rq JOIN competition_round r ON r.id=rq.round_id WHERE r.competition_id=? AND rq.question_id=?",
                    competitionId,
                    questionId);
        }

        @Override
        public void insertRoundQuestion(
                String roundQuestionId,
                String roundId,
                String questionId,
                int sequence)
                throws Exception {
            db.execute(
                    "INSERT INTO round_question VALUES(?,?,?,?)",
                    roundQuestionId,
                    roundId,
                    questionId,
                    sequence);
        }

        @Override
        public void deleteRoundQuestion(String roundQuestionId) throws Exception {
            db.execute("DELETE FROM round_question WHERE id=?", roundQuestionId);
        }
    }
}
