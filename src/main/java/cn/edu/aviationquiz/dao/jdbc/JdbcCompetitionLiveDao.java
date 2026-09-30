package cn.edu.aviationquiz.dao.jdbc;

import cn.edu.aviationquiz.dao.CompetitionLiveDao;
import cn.edu.aviationquiz.dao.Store;
import cn.edu.aviationquiz.dao.Store.Row;
import cn.edu.aviationquiz.dao.Store.UnitOfWork;
import cn.edu.aviationquiz.entity.LiveCompetitionRecords.ExpiredRelease;
import cn.edu.aviationquiz.entity.LiveCompetitionRecords.NamedItem;
import cn.edu.aviationquiz.entity.LiveCompetitionRecords.OpenRelease;
import cn.edu.aviationquiz.entity.LiveCompetitionRecords.PlayerQuestion;
import cn.edu.aviationquiz.entity.Models.ActiveReleaseView;
import cn.edu.aviationquiz.entity.Models.CompetitionProgressView;
import cn.edu.aviationquiz.entity.Models.PlayerSubmissionView;

import java.util.List;

/** SQLite implementation of live competition persistence. */
public final class JdbcCompetitionLiveDao implements CompetitionLiveDao {
    private static final String RELEASE_SQL =
            "SELECT qr.*,gr.group_id,gr.round_id,r.competition_id,r.round_type,r.name"
                + " round_name,q.content,q.option_a,q.option_b,q.option_c,q.option_d,q.correct_answer"
                + " FROM question_release qr JOIN group_round gr ON gr.id=qr.group_round_id JOIN"
                + " competition_round r ON r.id=gr.round_id JOIN round_question rq ON"
                + " rq.id=qr.round_question_id JOIN question q ON q.id=rq.question_id ";

    private final Store store;

    public JdbcCompetitionLiveDao(Store store) {
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
        public boolean hasRunningCompetition() throws Exception {
            return db.exists("SELECT id FROM competition WHERE status='比赛中'");
        }

        @Override
        public boolean hasValidPlayer(String competitionId) throws Exception {
            return db.exists(
                    "SELECT id FROM registration WHERE competition_id=? AND status='有效'",
                    competitionId);
        }

        @Override
        public boolean hasUnassignedPlayer(String competitionId) throws Exception {
            return db.exists(
                    "SELECT r.id FROM registration r LEFT JOIN group_assignment a"
                        + " ON a.registration_id=r.id WHERE r.competition_id=? AND"
                        + " r.status='有效' AND a.id IS NULL",
                    competitionId);
        }

        @Override
        public List<NamedItem> findGroups(String competitionId) throws Exception {
            return db.list(
                            "SELECT id,name FROM competition_group WHERE competition_id=?",
                            competitionId)
                    .stream()
                    .map(row -> new NamedItem(row.text("id"), row.text("name")))
                    .toList();
        }

        @Override
        public List<NamedItem> findRounds(String competitionId) throws Exception {
            return db.list(
                            "SELECT id,name FROM competition_round WHERE competition_id=?",
                            competitionId)
                    .stream()
                    .map(row -> new NamedItem(row.text("id"), row.text("name")))
                    .toList();
        }

        @Override
        public boolean groupHasPlayer(String groupId) throws Exception {
            return db.exists("SELECT id FROM group_assignment WHERE group_id=?", groupId);
        }

        @Override
        public boolean roundHasQuestion(String roundId) throws Exception {
            return db.exists("SELECT id FROM round_question WHERE round_id=?", roundId);
        }

        @Override
        public void createGroupRound(String id, String groupId, String roundId)
                throws Exception {
            db.execute(
                    "INSERT INTO group_round VALUES(?,?,?,'待开始')", id, groupId, roundId);
        }

        @Override
        public void updateCompetitionStatus(String competitionId, String status)
                throws Exception {
            db.execute("UPDATE competition SET status=? WHERE id=?", status, competitionId);
        }

        @Override
        public CompetitionProgressView findCurrentProgress(String competitionId)
                throws Exception {
            var rows =
                    db.list(
                            "SELECT gr.*,g.name group_name,r.name round_name,r.time_limit,r.round_type"
                                + " FROM group_round gr JOIN competition_group g ON g.id=gr.group_id"
                                + " JOIN competition_round r ON r.id=gr.round_id WHERE"
                                + " g.competition_id=? AND gr.status<>'已完成' ORDER BY"
                                + " g.sequence_no,r.sequence_no LIMIT 1",
                            competitionId);
            if (rows.isEmpty()) return null;
            Row row = rows.getFirst();
            return new CompetitionProgressView(
                    row.text("id"),
                    row.text("group_id"),
                    row.text("round_id"),
                    row.text("group_name"),
                    row.text("round_name"),
                    (int) row.number("time_limit"),
                    row.text("round_type"),
                    row.text("status"));
        }

        @Override
        public int countCompletedQuestions(String groupRoundId) throws Exception {
            return (int)
                    db.one(
                                    "SELECT COUNT(*) n FROM question_release WHERE"
                                            + " group_round_id=? AND closed_at IS NOT NULL",
                                    groupRoundId)
                            .number("n");
        }

        @Override
        public void updateGroupRoundStatus(String groupRoundId, String status)
                throws Exception {
            db.execute("UPDATE group_round SET status=? WHERE id=?", status, groupRoundId);
        }

        @Override
        public boolean hasOpenRelease() throws Exception {
            return db.exists("SELECT id FROM question_release WHERE closed_at IS NULL");
        }

        @Override
        public String findNextRoundQuestion(String roundId, String groupRoundId)
                throws Exception {
            var rows =
                    db.list(
                            "SELECT rq.id FROM round_question rq WHERE rq.round_id=? AND NOT"
                                + " EXISTS(SELECT 1 FROM question_release qr WHERE"
                                + " qr.round_question_id=rq.id AND qr.group_round_id=?)"
                                + " ORDER BY rq.sequence_no LIMIT 1",
                            roundId,
                            groupRoundId);
            return rows.isEmpty() ? null : rows.getFirst().text("id");
        }

        @Override
        public void createRelease(
                String releaseId,
                String groupRoundId,
                String roundQuestionId,
                long startedAt,
                long deadline)
                throws Exception {
            db.execute(
                    "INSERT INTO question_release VALUES(?,?,?,?,?,NULL)",
                    releaseId,
                    groupRoundId,
                    roundQuestionId,
                    startedAt,
                    deadline);
        }

        @Override
        public List<ExpiredRelease> findExpiredReleases(long now) throws Exception {
            return db.list(
                            RELEASE_SQL + "WHERE qr.closed_at IS NULL AND qr.deadline<=?",
                            now)
                    .stream()
                    .map(
                            row ->
                                    new ExpiredRelease(
                                            row.text("id"), row.text("group_id")))
                    .toList();
        }

        @Override
        public List<String> findMemberIds(String groupId) throws Exception {
            return db.list(
                            "SELECT r.player_id FROM group_assignment a JOIN registration r ON"
                                    + " r.id=a.registration_id WHERE a.group_id=? AND r.status='有效'",
                            groupId)
                    .stream()
                    .map(row -> row.text("player_id"))
                    .toList();
        }

        @Override
        public boolean hasAnswer(String releaseId, String playerId) throws Exception {
            return db.exists(
                    "SELECT id FROM answer_record WHERE release_id=? AND player_id=?",
                    releaseId,
                    playerId);
        }

        @Override
        public void createTimeout(
                String timeoutId, String releaseId, String playerId, long createdAt)
                throws Exception {
            db.execute(
                    "INSERT OR IGNORE INTO timeout_record VALUES(?,?,?,?)",
                    timeoutId,
                    releaseId,
                    playerId,
                    createdAt);
        }

        @Override
        public void closeReleaseAtDeadline(String releaseId) throws Exception {
            db.execute(
                    "UPDATE question_release SET closed_at=deadline WHERE id=?", releaseId);
        }

        @Override
        public ActiveReleaseView findActiveRelease(String competitionId) throws Exception {
            var rows =
                    db.list(
                            RELEASE_SQL
                                    + "WHERE r.competition_id=? AND qr.closed_at IS NULL",
                            competitionId);
            if (rows.isEmpty()) return null;
            Row row = rows.getFirst();
            return new ActiveReleaseView(
                    row.text("id"), row.text("content"), row.number("deadline"));
        }

        @Override
        public OpenRelease findOpenRelease(String competitionId) throws Exception {
            var rows =
                    db.list(
                            RELEASE_SQL
                                    + "WHERE r.competition_id=? AND qr.closed_at IS NULL",
                            competitionId);
            return rows.isEmpty()
                    ? null
                    : new OpenRelease(
                            rows.getFirst().text("id"),
                            rows.getFirst().text("group_id"));
        }

        @Override
        public int countAnswers(String releaseId) throws Exception {
            return (int)
                    db.one(
                                    "SELECT COUNT(*) n FROM answer_record WHERE release_id=?",
                                    releaseId)
                            .number("n");
        }

        @Override
        public void closeRelease(String releaseId, long closedAt) throws Exception {
            db.execute(
                    "UPDATE question_release SET closed_at=? WHERE id=?",
                    closedAt,
                    releaseId);
        }

        @Override
        public boolean hasIncompleteRoundQuestion(String roundId, String groupRoundId)
                throws Exception {
            return db.exists(
                    "SELECT rq.id FROM round_question rq WHERE rq.round_id=? AND"
                        + " NOT EXISTS(SELECT 1 FROM question_release qr WHERE"
                        + " qr.group_round_id=? AND qr.round_question_id=rq.id AND"
                        + " qr.closed_at IS NOT NULL)",
                    roundId,
                    groupRoundId);
        }

        @Override
        public boolean isProfileComplete(String playerId) throws Exception {
            return db.one("SELECT profile_complete FROM player WHERE id=?", playerId)
                            .number("profile_complete")
                    == 1;
        }

        @Override
        public PlayerQuestion findPlayerQuestion(String competitionId, String playerId)
                throws Exception {
            var rows =
                    db.list(
                            RELEASE_SQL
                                + "JOIN group_assignment ga ON ga.group_id=gr.group_id"
                                + " JOIN registration reg ON reg.id=ga.registration_id"
                                + " WHERE r.competition_id=? AND reg.player_id=? ORDER"
                                + " BY qr.started_at DESC,qr.rowid DESC LIMIT 1",
                            competitionId,
                            playerId);
            if (rows.isEmpty()) return null;
            Row release = rows.getFirst();
            var answers =
                    db.list(
                            "SELECT * FROM answer_record WHERE release_id=? AND player_id=?",
                            release.text("id"),
                            playerId);
            Row answer = answers.isEmpty() ? null : answers.getFirst();
            return new PlayerQuestion(
                    release.text("id"),
                    release.text("round_name"),
                    release.text("content"),
                    List.of(
                            release.text("option_a"),
                            release.text("option_b"),
                            release.text("option_c"),
                            release.text("option_d")),
                    release.number("deadline"),
                    release.nil("closed_at") ? null : release.number("closed_at"),
                    answer == null ? null : answer.text("user_answer"),
                    answer == null ? null : answer.number("correct") == 1,
                    answer == null ? null : (int) answer.number("score_change"));
        }

        @Override
        public List<PlayerSubmissionView> findSubmissions(
                String groupRoundId, String groupId) throws Exception {
            return db.list(
                            "SELECT p.name,CASE WHEN a.id IS NOT NULL THEN '已提交' WHEN t.id IS NOT"
                                + " NULL THEN '超时' ELSE '待提交' END answer_status FROM"
                                + " group_assignment ga JOIN registration reg ON"
                                + " reg.id=ga.registration_id JOIN player p ON p.id=reg.player_id"
                                + " LEFT JOIN answer_record a ON a.player_id=p.id AND"
                                + " a.release_id=(SELECT id FROM question_release WHERE"
                                + " group_round_id=? ORDER BY rowid DESC LIMIT 1) LEFT JOIN"
                                + " timeout_record t ON t.player_id=p.id AND t.release_id=(SELECT"
                                + " id FROM question_release WHERE group_round_id=? ORDER BY rowid"
                                + " DESC LIMIT 1) WHERE ga.group_id=?",
                            groupRoundId,
                            groupRoundId,
                            groupId)
                    .stream()
                    .map(
                            row ->
                                    new PlayerSubmissionView(
                                            row.text("name"), row.text("answer_status")))
                    .toList();
        }
    }
}
