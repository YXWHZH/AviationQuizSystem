package cn.edu.aviationquiz.service;

import cn.edu.aviationquiz.dao.Store;
import cn.edu.aviationquiz.dao.Store.Row;
import cn.edu.aviationquiz.dao.Store.UnitOfWork;
import cn.edu.aviationquiz.entity.AccountRecord;
import cn.edu.aviationquiz.entity.Models.*;
import cn.edu.aviationquiz.entity.ParticipationType;
import cn.edu.aviationquiz.exception.BusinessException;

import java.time.Clock;
import java.util.*;

/** Application use cases. Each mutation checks authority and owns one transaction. */
public final class QuizService
        implements AccountUseCases,
                CompetitionManagementUseCases,
                CompetitionLiveUseCases,
                CompetitionRoomService,
                QuestionBankUseCases,
                RegistrationUseCases,
                ResultUseCases {
    private static final String COMPETITION_SELECT =
            "SELECT c.*,(SELECT GROUP_CONCAT(category,' / ') FROM competition_category cc WHERE cc.competition_id=c.id ORDER BY category) categories FROM competition c ";
    private final Store store;
    private final Clock clock;
    private final AccountManagementService accountService;
    private final CompetitionManagementService competitionManagementService;
    private final CompetitionExecutionService executionService;
    private final QuestionBankService questionBankService;
    private final RegistrationManagementService registrationService;
    private final ResultService resultService;
    private final Map<String, Session> sessions = new HashMap<>();

    public QuizService(
            Store store,
            Clock clock,
            AccountManagementService accountService,
            CompetitionManagementService competitionManagementService,
            CompetitionExecutionService executionService,
            QuestionBankService questionBankService,
            RegistrationManagementService registrationService,
            ResultService resultService) {
        this.store = store;
        this.clock = clock;
        this.accountService = accountService;
        this.competitionManagementService = competitionManagementService;
        this.executionService = executionService;
        this.questionBankService = questionBankService;
        this.registrationService = registrationService;
        this.resultService = resultService;
        recover();
    }

    private static String id(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "");
    }

    private static void require(boolean valid, String message) {
        if (!valid) throw new BusinessException(message);
    }

    private long now() {
        return clock.millis();
    }

    private void auth(Session s, boolean staff) {
        require(
                s != null && s.equals(sessions.get(s.token())) && s.staff() == staff,
                "请使用对应身份重新登录");
    }

    private void signed(Session s) {
        require(s != null && s.equals(sessions.get(s.token())), "请重新登录");
    }

    private static String bounded(String text, int min, int max, String label) {
        String value = text == null ? "" : text.trim();
        require(value.length() >= min && value.length() <= max, label + "长度须为 " + min + "～" + max);
        return value;
    }

    @Override
    public synchronized boolean needsSetup() {
        return accountService.needsStaffSetup();
    }

    @Override
    public synchronized void setupStaff(String username, String password, String name) {
        accountService.setupStaff(username, password, name);
    }

    @Override
    public synchronized void register(String username, String password, PlayerProfileInput profile) {
        accountService.registerPlayer(username, password, profile);
    }

    @Override
    public synchronized boolean profileComplete(Session s) {
        auth(s, false);
        return accountService.profileComplete(s.id());
    }

    @Override
    public synchronized PlayerProfileView playerProfile(Session s) {
        auth(s, false);
        return accountService.playerProfile(s.id());
    }

    @Override
    public synchronized void updatePlayerProfile(Session s, PlayerProfileInput profile) {
        auth(s, false);
        accountService.updatePlayerProfile(s.id(), profile);
    }

    private void requireComplete(UnitOfWork db, Session s) throws Exception {
        require(db.one("SELECT profile_complete FROM player WHERE id=?", s.id()).number("profile_complete") == 1, "请先完善个人资料");
    }

    @Override
    public synchronized Session login(boolean staff, String username, String password) {
        AccountRecord account = accountService.authenticate(staff, username, password);
        for (Session session : sessions.values())
            if (session.staff() == account.staff() && session.id().equals(account.id()))
                return session;
        Session session =
                new Session(
                        id("SESSION"),
                        account.id(),
                        account.username(),
                        account.name(),
                        account.staff());
        sessions.put(session.token(), session);
        return session;
    }

    @Override
    public synchronized void logout(Session s) {
        if (s != null) sessions.remove(s.token());
    }

    public synchronized List<Row> competitions() {
        return store.transaction(
                db -> db.list(COMPETITION_SELECT + "ORDER BY c.competition_time DESC,c.id"));
    }

    public synchronized List<Row> competitionsForPlayer(Session s) {
        auth(s, false);
        return store.transaction(
                db ->
                        db.list(
                                """
SELECT c.*,(SELECT GROUP_CONCAT(category,' / ') FROM competition_category cc WHERE cc.competition_id=c.id ORDER BY category) categories,
 CASE
  WHEN reg.status='有效' AND ga.id IS NOT NULL THEN '已分组'
  WHEN reg.status='有效' THEN '已报名'
  WHEN res.status='有效' THEN '已预约'
  WHEN reg.status='已取消' THEN '已取消报名'
  WHEN res.status='已取消' THEN '已取消预约'
  ELSE '未参与'
 END my_status
FROM competition c
LEFT JOIN registration reg ON reg.competition_id=c.id AND reg.player_id=?
LEFT JOIN group_assignment ga ON ga.registration_id=reg.id
LEFT JOIN reservation res ON res.competition_id=c.id AND res.player_id=?
ORDER BY c.competition_time DESC,c.id
""",
                                s.id(),
                                s.id()));
    }

    @Override
    public synchronized String saveCompetition(Session s, String existing, CompetitionInput input) {
        auth(s, true);
        return competitionManagementService.save(existing, input);
    }

    private Row competition(UnitOfWork db, String cid) throws Exception {
        return db.one("SELECT * FROM competition WHERE id=?", cid);
    }

    private void editable(UnitOfWork db, String cid) throws Exception {
        require(!List.of("比赛中", "已结束").contains(competition(db, cid).text("status")), "开赛后配置已锁定");
    }

    @Override
    public synchronized void registrationState(Session s, String cid, String state) {
        auth(s, true);
        competitionManagementService.changeRegistrationState(cid, state);
    }

    @Override
    public synchronized void join(Session s, String cid, boolean reservation) {
        auth(s, false);
        registrationService.join(
                s.id(),
                cid,
                reservation ? ParticipationType.RESERVATION : ParticipationType.REGISTRATION);
    }

    @Override
    public synchronized void cancelParticipation(Session s, String cid, boolean reservation) {
        auth(s, false);
        registrationService.cancel(
                s.id(),
                cid,
                reservation ? ParticipationType.RESERVATION : ParticipationType.REGISTRATION);
    }

    public synchronized List<Row> mine(Session s) {
        auth(s, false);
        return store.transaction(
                db ->
                        db.list(
                                """
SELECT c.*,(SELECT GROUP_CONCAT(category,' / ') FROM competition_category cc WHERE cc.competition_id=c.id ORDER BY category) categories,
CASE WHEN r.status='有效' THEN '已报名' WHEN v.status='有效' THEN '已预约' WHEN r.status='已取消' THEN '已取消报名' ELSE '已取消预约' END participation,
CASE WHEN r.status='有效' THEN CASE WHEN g.id IS NULL THEN '待分组' ELSE g.name END ELSE '—' END group_name,
CASE WHEN r.status='有效' AND g.id IS NOT NULL THEN '已分组' WHEN r.status='有效' THEN '已报名' WHEN v.status='有效' THEN '已预约' WHEN r.status='已取消' THEN '已取消报名' ELSE '已取消预约' END my_status
FROM competition c LEFT JOIN registration r ON r.competition_id=c.id AND r.player_id=?
LEFT JOIN group_assignment a ON a.registration_id=r.id LEFT JOIN competition_group g ON g.id=a.group_id
LEFT JOIN reservation v ON v.competition_id=c.id AND v.player_id=?
WHERE r.id IS NOT NULL OR v.id IS NOT NULL ORDER BY c.competition_time DESC
""",
                                s.id(),
                                s.id()));
    }

    public synchronized List<Row> people(Session s, String cid, boolean reserved) {
        auth(s, true);
        return store.transaction(
                db ->
                        reserved
                                ? db.list(
                                        "SELECT v.id,p.name,p.username,p.school,p.college,p.major,p.student_number,p.phone,v.created_at,'' group_name FROM"
                                                + " reservation v JOIN player p ON p.id=v.player_id"
                                                + " WHERE v.competition_id=? AND v.status='有效'",
                                        cid)
                                : db.list(
                                        "SELECT"
                                            + " r.id,p.name,p.username,p.school,p.college,p.major,p.student_number,p.phone,r.created_at,COALESCE(g.name,'待分组')"
                                            + " group_name FROM registration r JOIN player p ON"
                                            + " p.id=r.player_id LEFT JOIN group_assignment a ON"
                                            + " a.registration_id=r.id LEFT JOIN competition_group"
                                            + " g ON g.id=a.group_id WHERE r.competition_id=? AND"
                                            + " r.status='有效' ORDER BY r.created_at,r.id",
                                        cid));
    }

    public synchronized List<Row> groups(Session s, String cid) {
        signed(s);
        return store.transaction(
                db ->
                        db.list(
                                "SELECT * FROM competition_group WHERE competition_id=? ORDER BY"
                                        + " sequence_no",
                                cid));
    }

    @Override
    public synchronized String addGroup(Session s, String cid, String name, int sequence) {
        auth(s, true);
        return registrationService.addGroup(cid, name, sequence);
    }

    @Override
    public synchronized void assign(Session s, String registration, String group) {
        auth(s, true);
        registrationService.assign(registration, group);
    }

    public synchronized void deleteEmptyGroup(Session s, String group) {
        auth(s, true);
        store.transaction(
                db -> {
                    Row g = db.one("SELECT * FROM competition_group WHERE id=?", group);
                    editable(db, g.text("competition_id"));
                    require(
                            !db.exists("SELECT id FROM group_assignment WHERE group_id=?", group),
                            "只能删除尚未分配选手的空小组");
                    db.execute("DELETE FROM competition_group WHERE id=?", group);
                    return null;
                });
    }

    @Override
    public synchronized void updateRound(
            Session s, String rid, String name, String type, int sequence, int seconds) {
        auth(s, true);
        questionBankService.updateRound(rid, name, type, sequence, seconds);
    }

    @Override
    public synchronized void deleteEmptyRound(Session s, String rid) {
        auth(s, true);
        questionBankService.deleteEmptyRound(rid);
    }

    @Override
    public synchronized List<QuestionView> questions(Session s) {
        auth(s, true);
        return questionBankService.questions();
    }

    @Override
    public synchronized List<QuestionView> questionsForCompetition(Session s, String cid) {
        auth(s, true);
        return questionBankService.questionsForCompetition(cid);
    }

    @Override
    public synchronized String saveQuestion(Session s, String existing, QuestionInput q) {
        auth(s, true);
        return questionBankService.saveQuestion(existing, q);
    }

    @Override
    public synchronized void questionState(Session s, String qid, boolean active) {
        auth(s, true);
        questionBankService.setQuestionActive(qid, active);
    }

    @Override
    public synchronized void deleteQuestion(Session s, String qid) {
        auth(s, true);
        questionBankService.deleteQuestion(qid);
    }

    @Override
    public synchronized List<RoundView> rounds(Session s, String cid) {
        auth(s, true);
        return questionBankService.rounds(cid);
    }

    @Override
    public synchronized String addRound(
            Session s, String cid, String name, String type, int sequence, int seconds) {
        auth(s, true);
        return questionBankService.addRound(cid, name, type, sequence, seconds);
    }

    @Override
    public synchronized List<RoundQuestionView> roundQuestions(Session s, String rid) {
        auth(s, true);
        return questionBankService.roundQuestions(rid);
    }

    @Override
    public synchronized void addQuestionToRound(Session s, String rid, String qid, int sequence) {
        auth(s, true);
        questionBankService.addQuestionToRound(rid, qid, sequence);
    }

    @Override
    public synchronized void removeRoundQuestion(Session s, String rqid) {
        auth(s, true);
        questionBankService.removeRoundQuestion(rqid);
    }

    @Override
    public synchronized void startCompetition(Session s, String cid) {
        auth(s, true);
        store.transaction(
                db -> {
                    require(competition(db, cid).text("status").equals("报名截止"), "请先截止报名");
                    require(
                            !db.exists("SELECT id FROM competition WHERE status='比赛中'"),
                            "已有一场竞赛正在运行");
                    require(
                            db.exists(
                                    "SELECT id FROM registration WHERE competition_id=? AND"
                                            + " status='有效'",
                                    cid),
                            "没有正式参赛选手");
                    require(
                            !db.exists(
                                    "SELECT r.id FROM registration r LEFT JOIN group_assignment a"
                                        + " ON a.registration_id=r.id WHERE r.competition_id=? AND"
                                        + " r.status='有效' AND a.id IS NULL",
                                    cid),
                            "还有选手未分组");
                    var gs = db.list("SELECT * FROM competition_group WHERE competition_id=?", cid);
                    var rs = db.list("SELECT * FROM competition_round WHERE competition_id=?", cid);
                    require(!gs.isEmpty() && !rs.isEmpty(), "请配置小组和轮次");
                    for (Row g : gs)
                        require(
                                db.exists(
                                        "SELECT id FROM group_assignment WHERE group_id=?",
                                        g.text("id")),
                                "存在空小组：" + g.text("name"));
                    for (Row r : rs)
                        require(
                                db.exists(
                                        "SELECT id FROM round_question WHERE round_id=?",
                                        r.text("id")),
                                "轮次尚未配置题目：" + r.text("name"));
                    for (Row g : gs)
                        for (Row r : rs)
                            db.execute(
                                    "INSERT INTO group_round VALUES(?,?,?,'待开始')",
                                    id("GR"),
                                    g.text("id"),
                                    r.text("id"));
                    db.execute("UPDATE competition SET status='比赛中' WHERE id=?", cid);
                    return null;
                });
    }

    private Row current(UnitOfWork db, String cid) throws Exception {
        var rows =
                db.list(
                        "SELECT gr.*,g.name group_name,r.name round_name,r.time_limit,r.round_type"
                            + " FROM group_round gr JOIN competition_group g ON g.id=gr.group_id"
                            + " JOIN competition_round r ON r.id=gr.round_id WHERE"
                            + " g.competition_id=? AND gr.status<>'已完成' ORDER BY"
                            + " g.sequence_no,r.sequence_no LIMIT 1",
                        cid);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    @Override
    public synchronized CompetitionProgressView progress(Session s, String cid) {
        auth(s, true);
        return store.transaction(
                db -> {
                    Row row = current(db, cid);
                    return row == null
                            ? null
                            : new CompetitionProgressView(
                                    row.text("id"),
                                    row.text("group_id"),
                                    row.text("round_id"),
                                    row.text("group_name"),
                                    row.text("round_name"),
                                    (int) row.number("time_limit"),
                                    row.text("round_type"),
                                    row.text("status"));
                });
    }

    @Override
    public synchronized int completedQuestions(Session s, String cid) {
        auth(s, true);
        return store.transaction(
                db -> {
                    Row gr = current(db, cid);
                    return gr == null
                            ? 0
                            : (int)
                                    db.one(
                                                    "SELECT COUNT(*) n FROM question_release WHERE"
                                                        + " group_round_id=? AND closed_at IS NOT"
                                                        + " NULL",
                                                    gr.text("id"))
                                            .number("n");
                });
    }

    private void running(UnitOfWork db, String cid) throws Exception {
        require(competition(db, cid).text("status").equals("比赛中"), "竞赛不在比赛中");
    }

    @Override
    public synchronized void startRound(Session s, String cid) {
        auth(s, true);
        store.transaction(
                db -> {
                    running(db, cid);
                    Row gr = current(db, cid);
                    require(gr != null && gr.text("status").equals("待开始"), "没有待开始轮次");
                    db.execute("UPDATE group_round SET status='进行中' WHERE id=?", gr.text("id"));
                    return null;
                });
    }

    @Override
    public synchronized String publish(Session s, String cid) {
        auth(s, true);
        recover();
        return store.transaction(
                db -> {
                    running(db, cid);
                    Row gr = current(db, cid);
                    require(gr != null && gr.text("status").equals("进行中"), "请先开始当前轮次");
                    require(
                            !db.exists("SELECT id FROM question_release WHERE closed_at IS NULL"),
                            "当前题目尚未结束");
                    var next =
                            db.list(
                                    "SELECT rq.* FROM round_question rq WHERE rq.round_id=? AND NOT"
                                            + " EXISTS(SELECT 1 FROM question_release qr WHERE"
                                            + " qr.round_question_id=rq.id AND qr.group_round_id=?)"
                                            + " ORDER BY rq.sequence_no LIMIT 1",
                                    gr.text("round_id"),
                                    gr.text("id"));
                    require(!next.isEmpty(), "本轮全部题目已发布，请结束轮次");
                    String release = id("PUB");
                    long start = now();
                    db.execute(
                            "INSERT INTO question_release VALUES(?,?,?,?,?,NULL)",
                            release,
                            gr.text("id"),
                            next.getFirst().text("id"),
                            start,
                            Math.addExact(
                                    start, Math.multiplyExact(gr.number("time_limit"), 1000)));
                    return release;
                });
    }

    private static final String RELEASE_SQL =
            "SELECT qr.*,gr.group_id,gr.round_id,r.competition_id,r.round_type,r.name"
                + " round_name,q.content,q.option_a,q.option_b,q.option_c,q.option_d,q.correct_answer"
                + " FROM question_release qr JOIN group_round gr ON gr.id=qr.group_round_id JOIN"
                + " competition_round r ON r.id=gr.round_id JOIN round_question rq ON"
                + " rq.id=qr.round_question_id JOIN question q ON q.id=rq.question_id ";

    private List<Row> members(UnitOfWork db, String gid) throws Exception {
        return db.list(
                "SELECT r.player_id FROM group_assignment a JOIN registration r ON"
                        + " r.id=a.registration_id WHERE a.group_id=? AND r.status='有效'",
                gid);
    }

    @Override
    public synchronized int submit(Session s, String release, String option) {
        auth(s, false);
        recover();
        return executionService.submitAnswer(s.id(), release, option);
    }

    public synchronized boolean recover() {
        return store.transaction(
                db -> {
                    var expired =
                            db.list(
                                    RELEASE_SQL + "WHERE qr.closed_at IS NULL AND qr.deadline<=?",
                                    now());
                    for (Row qr : expired) {
                        for (Row p : members(db, qr.text("group_id")))
                            if (!db.exists(
                                    "SELECT id FROM answer_record WHERE release_id=? AND"
                                            + " player_id=?",
                                    qr.text("id"),
                                    p.text("player_id")))
                                db.execute(
                                        "INSERT OR IGNORE INTO timeout_record VALUES(?,?,?,?)",
                                        id("T"),
                                        qr.text("id"),
                                        p.text("player_id"),
                                        now());
                        db.execute(
                                "UPDATE question_release SET closed_at=deadline WHERE id=?",
                                qr.text("id"));
                    }
                    return !expired.isEmpty();
                });
    }

    @Override
    public synchronized ActiveReleaseView activeRelease(Session s, String cid) {
        auth(s, true);
        return store.transaction(
                db -> {
                    var rows =
                            db.list(
                                    RELEASE_SQL
                                            + "WHERE r.competition_id=? AND qr.closed_at IS NULL",
                                    cid);
                    if (rows.isEmpty()) return null;
                    Row row = rows.getFirst();
                    return new ActiveReleaseView(
                            row.text("id"), row.text("content"), row.number("deadline"));
                });
    }

    @Override
    public synchronized void closeQuestion(Session s, String cid) {
        auth(s, true);
        recover();
        store.transaction(
                db -> {
                    running(db, cid);
                    Row qr =
                            db.one(
                                    RELEASE_SQL
                                            + "WHERE r.competition_id=? AND qr.closed_at IS NULL",
                                    cid);
                    long count =
                            db.one(
                                            "SELECT COUNT(*) n FROM answer_record WHERE"
                                                    + " release_id=?",
                                            qr.text("id"))
                                    .number("n");
                    require(count == members(db, qr.text("group_id")).size(), "还有选手未提交，请等待倒计时结束");
                    db.execute(
                            "UPDATE question_release SET closed_at=? WHERE id=?",
                            now(),
                            qr.text("id"));
                    return null;
                });
    }

    @Override
    public synchronized void finishRound(Session s, String cid) {
        auth(s, true);
        recover();
        store.transaction(
                db -> {
                    running(db, cid);
                    Row gr = current(db, cid);
                    require(gr != null && gr.text("status").equals("进行中"), "没有进行中的轮次");
                    require(
                            !db.exists(
                                    "SELECT rq.id FROM round_question rq WHERE rq.round_id=? AND"
                                        + " NOT EXISTS(SELECT 1 FROM question_release qr WHERE"
                                        + " qr.group_round_id=? AND qr.round_question_id=rq.id AND"
                                        + " qr.closed_at IS NOT NULL)",
                                    gr.text("round_id"),
                                    gr.text("id")),
                            "本轮还有未完成题目");
                    db.execute("UPDATE group_round SET status='已完成' WHERE id=?", gr.text("id"));
                    return null;
                });
    }

    @Override
    public synchronized PublishedQuestionView room(Session s, String cid) {
        auth(s, false);
        recover();
        return store.transaction(
                db -> {
                    requireComplete(db, s);
                    var rows =
                            db.list(
                                    RELEASE_SQL
                                            + "JOIN group_assignment ga ON ga.group_id=gr.group_id"
                                            + " JOIN registration reg ON reg.id=ga.registration_id"
                                            + " WHERE r.competition_id=? AND reg.player_id=? ORDER"
                                            + " BY qr.started_at DESC,qr.rowid DESC LIMIT 1",
                                    cid,
                                    s.id());
                    if (rows.isEmpty()) return null;
                    Row qr = rows.getFirst();
                    var answers =
                            db.list(
                                    "SELECT * FROM answer_record WHERE release_id=? AND"
                                            + " player_id=?",
                                    qr.text("id"),
                                    s.id());
                    Row a = answers.isEmpty() ? null : answers.getFirst();
                    String status =
                            a != null
                                    ? (a.number("correct") == 1 ? "回答正确" : "回答错误")
                                    : qr.nil("closed_at") ? "可作答" : "已超时，本题 0 分";
                    return new PublishedQuestionView(
                            qr.text("id"),
                            qr.text("round_name"),
                            qr.text("content"),
                            List.of(
                                    qr.text("option_a"),
                                    qr.text("option_b"),
                                    qr.text("option_c"),
                                    qr.text("option_d")),
                            qr.number("deadline"),
                            status,
                            a == null ? "" : a.text("user_answer"),
                            a == null ? null : (int) a.number("score_change"));
                });
    }

    @Override
    public synchronized List<PlayerSubmissionView> monitor(Session s, String cid) {
        auth(s, true);
        return store.transaction(
                db -> {
                    Row gr = current(db, cid);
                    if (gr == null) return List.of();
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
                                    gr.text("id"),
                                    gr.text("id"),
                                    gr.text("group_id"))
                            .stream()
                            .map(
                                    row ->
                                            new PlayerSubmissionView(
                                                    row.text("name"),
                                                    row.text("answer_status")))
                            .toList();
                });
    }

    @Override
    public synchronized List<RankingEntry> ranking(Session s, String cid) {
        signed(s);
        return resultService.ranking(cid);
    }

    @Override
    public synchronized List<RankingEntry> preview(Session s, String cid) {
        auth(s, true);
        return resultService.preview(cid);
    }

    @Override
    public synchronized void archive(Session s, String cid) {
        auth(s, true);
        resultService.archive(cid);
    }

    public synchronized List<Row> history(Session s) {
        signed(s);
        return store.transaction(
                db ->
                        s.staff()
                                ? db.list(
                                        "SELECT c.name,c.id,c.competition_time FROM competition c"
                                            + " WHERE status='已结束' ORDER BY competition_time DESC")
                                : db.list(
                                        "SELECT"
                                            + " c.name,c.id,c.competition_time,r.final_score,r.ranking,CASE"
                                            + " r.promoted WHEN 1 THEN '晋级' ELSE '未晋级' END"
                                            + " promotion FROM result r JOIN competition c ON"
                                            + " c.id=r.competition_id WHERE r.player_id=? ORDER BY"
                                            + " c.competition_time DESC",
                                        s.id()));
    }

    @Override
    public synchronized String exportCsv(Session s, String cid) {
        auth(s, true);
        store.transaction(
                db -> {
                    require(
                            competition(db, cid).text("status").equals("已结束"),
                            "只能导出已归档成绩");
                    return null;
                });
        StringBuilder csv =
                new StringBuilder("\uFEFF名次,选手编号,姓名,小组,最终成绩,晋级结果\r\n");
        for (RankingEntry r : resultService.ranking(cid))
            csv.append(r.rank())
                    .append(',')
                    .append(cell(r.playerId()))
                    .append(',')
                    .append(cell(r.name()))
                    .append(',')
                    .append(cell(r.group()))
                    .append(',')
                    .append(r.score())
                    .append(',')
                    .append(cell(r.promotion()))
                    .append("\r\n");
        return csv.toString();
    }

    private static String cell(String value) {
        if (!value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0) value = "'" + value;
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
