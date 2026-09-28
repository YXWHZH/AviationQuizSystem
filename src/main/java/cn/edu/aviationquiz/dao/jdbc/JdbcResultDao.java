package cn.edu.aviationquiz.dao.jdbc;

import cn.edu.aviationquiz.dao.ResultDao;
import cn.edu.aviationquiz.dao.Store;
import cn.edu.aviationquiz.dao.Store.Row;
import cn.edu.aviationquiz.dao.Store.UnitOfWork;
import cn.edu.aviationquiz.entity.CompetitionResultContext;
import cn.edu.aviationquiz.entity.RankingSnapshot;

import java.util.ArrayList;
import java.util.List;

/** SQLite implementation of ranking and archival persistence. */
public final class JdbcResultDao implements ResultDao {
    private static final String RANKING_SQL =
            """
SELECT p.id,p.name,COALESCE(g.name,'待分组') group_name,
COALESCE(scores.total,0) total,COALESCE(scores.correct_count,0) correct_count,COALESCE(scores.elapsed,0) elapsed,
result.final_score,result.ranking,result.promoted FROM registration reg JOIN player p ON p.id=reg.player_id
LEFT JOIN group_assignment ga ON ga.registration_id=reg.id LEFT JOIN competition_group g ON g.id=ga.group_id
LEFT JOIN (SELECT a.player_id,SUM(a.score_change) total,SUM(a.correct) correct_count,SUM(a.submitted_at-qr.started_at) elapsed
  FROM answer_record a JOIN question_release qr ON qr.id=a.release_id JOIN group_round gr ON gr.id=qr.group_round_id
  JOIN competition_round r ON r.id=gr.round_id WHERE r.competition_id=? GROUP BY a.player_id) scores ON scores.player_id=p.id
LEFT JOIN result ON result.competition_id=reg.competition_id AND result.player_id=p.id
WHERE reg.competition_id=? AND reg.status='有效'
""";

    private final Store store;

    public JdbcResultDao(Store store) {
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
        public CompetitionResultContext findCompetition(String competitionId) throws Exception {
            Row row =
                    db.one(
                            "SELECT id,status,advance_count FROM competition WHERE id=?",
                            competitionId);
            return new CompetitionResultContext(
                    row.text("id"), row.text("status"), (int) row.number("advance_count"));
        }

        @Override
        public boolean hasUnfinishedRounds(String competitionId) throws Exception {
            return db.exists(
                    "SELECT 1 FROM group_round gr JOIN competition_group g ON g.id=gr.group_id"
                            + " WHERE g.competition_id=? AND gr.status<>'已完成'",
                    competitionId);
        }

        @Override
        public List<RankingSnapshot> listRankingSnapshots(String competitionId)
                throws Exception {
            List<RankingSnapshot> result = new ArrayList<>();
            for (Row row : db.list(RANKING_SQL, competitionId, competitionId))
                result.add(
                        new RankingSnapshot(
                                row.text("id"),
                                row.text("name"),
                                row.text("group_name"),
                                (int) row.number("total"),
                                (int) row.number("correct_count"),
                                row.number("elapsed"),
                                (int) row.number("final_score"),
                                (int) row.number("ranking"),
                                row.number("promoted") == 1));
            return List.copyOf(result);
        }

        @Override
        public void insertResult(
                String id,
                String competitionId,
                String playerId,
                int finalScore,
                int rank,
                boolean promoted)
                throws Exception {
            db.execute(
                    "INSERT INTO result(id,competition_id,player_id,final_score,ranking,promoted)"
                            + " VALUES(?,?,?,?,?,?)",
                    id,
                    competitionId,
                    playerId,
                    finalScore,
                    rank,
                    promoted ? 1 : 0);
        }

        @Override
        public void markCompetitionArchived(String competitionId) throws Exception {
            db.execute("UPDATE competition SET status='已结束' WHERE id=?", competitionId);
        }
    }
}
