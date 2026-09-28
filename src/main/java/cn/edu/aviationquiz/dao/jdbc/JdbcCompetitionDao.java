package cn.edu.aviationquiz.dao.jdbc;

import cn.edu.aviationquiz.dao.CompetitionDao;
import cn.edu.aviationquiz.dao.Store;
import cn.edu.aviationquiz.dao.Store.Row;
import cn.edu.aviationquiz.dao.Store.UnitOfWork;
import cn.edu.aviationquiz.entity.CompetitionSetupContext;
import cn.edu.aviationquiz.entity.Models.CompetitionInput;

import java.util.LinkedHashSet;
import java.util.Set;

/** SQLite implementation of competition setup persistence. */
public final class JdbcCompetitionDao implements CompetitionDao {
    private final Store store;

    public JdbcCompetitionDao(Store store) {
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
        public CompetitionSetupContext findCompetition(String competitionId) throws Exception {
            Row row =
                    db.one(
                            "SELECT id,status,register_start,register_end FROM competition WHERE id=?",
                            competitionId);
            return new CompetitionSetupContext(
                    row.text("id"),
                    row.text("status"),
                    row.number("register_start"),
                    row.number("register_end"));
        }

        @Override
        public Set<String> findUsedQuestionCategories(String competitionId) throws Exception {
            var rows =
                    db.list(
                            "SELECT DISTINCT q.category FROM round_question rq JOIN question q ON q.id=rq.question_id JOIN competition_round r ON r.id=rq.round_id WHERE r.competition_id=?",
                            competitionId);
            Set<String> categories = new LinkedHashSet<>();
            for (Row row : rows) categories.add(row.text("category"));
            return Set.copyOf(categories);
        }

        @Override
        public void insertCompetition(String competitionId, CompetitionInput input)
                throws Exception {
            db.execute(
                    "INSERT INTO competition VALUES(?,?,?,?,?,?,?,?)",
                    competitionId,
                    input.name(),
                    input.description(),
                    input.registerStart(),
                    input.registerEnd(),
                    input.competitionTime(),
                    "未开放",
                    input.quota());
        }

        @Override
        public void updateCompetition(String competitionId, CompetitionInput input)
                throws Exception {
            db.execute(
                    "UPDATE competition SET name=?,description=?,register_start=?,register_end=?,competition_time=?,advance_count=? WHERE id=?",
                    input.name(),
                    input.description(),
                    input.registerStart(),
                    input.registerEnd(),
                    input.competitionTime(),
                    input.quota(),
                    competitionId);
        }

        @Override
        public void replaceCategories(String competitionId, Set<String> categories)
                throws Exception {
            db.execute("DELETE FROM competition_category WHERE competition_id=?", competitionId);
            for (String category : categories)
                db.execute(
                        "INSERT INTO competition_category VALUES(?,?)",
                        competitionId,
                        category);
        }

        @Override
        public void updateStatus(String competitionId, String status) throws Exception {
            db.execute(
                    "UPDATE competition SET status=? WHERE id=?", status, competitionId);
        }
    }
}
