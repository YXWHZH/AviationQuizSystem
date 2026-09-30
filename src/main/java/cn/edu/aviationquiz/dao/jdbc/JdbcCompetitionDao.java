package cn.edu.aviationquiz.dao.jdbc;

import cn.edu.aviationquiz.dao.CompetitionDao;
import cn.edu.aviationquiz.dao.Store;
import cn.edu.aviationquiz.dao.Store.Row;
import cn.edu.aviationquiz.dao.Store.UnitOfWork;
import cn.edu.aviationquiz.entity.CompetitionSetupContext;
import cn.edu.aviationquiz.entity.Models.CompetitionInput;
import cn.edu.aviationquiz.entity.Models.CompetitionView;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** SQLite implementation of competition setup persistence. */
public final class JdbcCompetitionDao implements CompetitionDao {
    private static final String COMPETITION_COLUMNS =
            "c.id,c.name,c.description,c.register_start,c.register_end,c.competition_time,"
                    + "c.status,c.advance_count,(SELECT GROUP_CONCAT(category,' / ') FROM"
                    + " competition_category cc WHERE cc.competition_id=c.id ORDER BY category)"
                    + " categories";
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
        public List<CompetitionView> findAll() throws Exception {
            return db.list(
                            "SELECT " + COMPETITION_COLUMNS
                                    + ",'' participation,'' group_name,'' my_status FROM competition c"
                                    + " ORDER BY c.competition_time DESC,c.id")
                    .stream()
                    .map(JdbcTransaction::competitionView)
                    .toList();
        }

        @Override
        public List<CompetitionView> findAllForPlayer(String playerId) throws Exception {
            return db.list(
                            "SELECT " + COMPETITION_COLUMNS + ",'' participation,'' group_name,"
                                + " CASE WHEN reg.status='有效' AND ga.id IS NOT NULL THEN '已分组'"
                                + " WHEN reg.status='有效' THEN '已报名' WHEN res.status='有效' THEN"
                                + " '已预约' WHEN reg.status='已取消' THEN '已取消报名' WHEN"
                                + " res.status='已取消' THEN '已取消预约' ELSE '未参与' END my_status"
                                + " FROM competition c LEFT JOIN registration reg ON"
                                + " reg.competition_id=c.id AND reg.player_id=? LEFT JOIN"
                                + " group_assignment ga ON ga.registration_id=reg.id LEFT JOIN"
                                + " reservation res ON res.competition_id=c.id AND res.player_id=?"
                                + " ORDER BY c.competition_time DESC,c.id",
                            playerId,
                            playerId)
                    .stream()
                    .map(JdbcTransaction::competitionView)
                    .toList();
        }

        @Override
        public List<CompetitionView> findParticipatedByPlayer(String playerId)
                throws Exception {
            return db.list(
                            "SELECT " + COMPETITION_COLUMNS + ",CASE WHEN r.status='有效' THEN"
                                + " '已报名' WHEN v.status='有效' THEN '已预约' WHEN"
                                + " r.status='已取消' THEN '已取消报名' ELSE '已取消预约' END"
                                + " participation,CASE WHEN r.status='有效' THEN CASE WHEN g.id IS"
                                + " NULL THEN '待分组' ELSE g.name END ELSE '—' END group_name,CASE"
                                + " WHEN r.status='有效' AND g.id IS NOT NULL THEN '已分组' WHEN"
                                + " r.status='有效' THEN '已报名' WHEN v.status='有效' THEN '已预约'"
                                + " WHEN r.status='已取消' THEN '已取消报名' ELSE '已取消预约' END"
                                + " my_status FROM competition c LEFT JOIN registration r ON"
                                + " r.competition_id=c.id AND r.player_id=? LEFT JOIN"
                                + " group_assignment a ON a.registration_id=r.id LEFT JOIN"
                                + " competition_group g ON g.id=a.group_id LEFT JOIN reservation v"
                                + " ON v.competition_id=c.id AND v.player_id=? WHERE r.id IS NOT"
                                + " NULL OR v.id IS NOT NULL ORDER BY c.competition_time DESC",
                            playerId,
                            playerId)
                    .stream()
                    .map(JdbcTransaction::competitionView)
                    .toList();
        }

        private static CompetitionView competitionView(Row row) {
            return new CompetitionView(
                    row.text("id"),
                    row.text("name"),
                    row.text("description"),
                    row.number("register_start"),
                    row.number("register_end"),
                    row.number("competition_time"),
                    row.text("status"),
                    (int) row.number("advance_count"),
                    row.text("categories"),
                    row.text("participation"),
                    row.text("group_name"),
                    row.text("my_status"));
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
