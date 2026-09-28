package cn.edu.aviationquiz.service;

import static org.junit.jupiter.api.Assertions.*;

import cn.edu.aviationquiz.dao.Store;
import cn.edu.aviationquiz.entity.*;
import cn.edu.aviationquiz.entity.Models.*;
import cn.edu.aviationquiz.exception.BusinessException;
import cn.edu.aviationquiz.exception.DataAccessException;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

class QuizServiceTest {
    @TempDir Path temp;
    Store store;
    QuizService service;
    Session staff;
    MutableClock clock;

    static final class MutableClock extends Clock {
        long millis = 1_800_000_000_000L;

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(millis);
        }

        @Override
        public long millis() {
            return millis;
        }
    }

    @BeforeEach
    void setup() {
        clock = new MutableClock();
        store = new Store(temp.resolve("v14.db"));
        service = new QuizService(store, clock);
        service.setupStaff("admin01", "secret12", "李老师");
        staff = service.login(true, "admin01", "secret12");
    }

    Session player(String name) {
        service.register(name, "secret12", new PlayerProfileInput("测试大学", "航空学院", "航空专业", name, name, "13800000000"));
        return service.login(false, name, "secret12");
    }

    String competition(int quota) {
        return service.saveCompetition(
                staff,
                null,
                new CompetitionInput(
                        "航空竞赛",
                        "测试",
                        clock.millis - 1000,
                        clock.millis + 1000,
                        clock.millis + 2000,
                        quota));
    }

    String question(String name) {
        return service.saveQuestion(
                staff,
                null,
                new QuestionInput(name, "民航史", List.of("甲", "乙", "丙", "丁"), "A", true));
    }

    record Fixture(String cid, List<Session> players, List<String> groups, List<String> rounds) {}

    Fixture ready(int quota, int groupCount, int perGroup, String... types) {
        String cid = competition(quota);
        service.registrationState(staff, cid, "报名中");
        List<Session> players = new ArrayList<>();
        for (int i = 0; i < groupCount * perGroup; i++) {
            Session p = player("user" + i);
            players.add(p);
            service.join(p, cid, false);
        }
        clock.millis += 1001;
        service.registrationState(staff, cid, "报名截止");
        List<String> groups = new ArrayList<>();
        var registrations = service.people(staff, cid, false);
        for (int i = 0; i < groupCount; i++) {
            String gid = service.addGroup(staff, cid, "组" + i, i + 1);
            groups.add(gid);
            for (int j = 0; j < perGroup; j++) {
                Session p = players.get(i * perGroup + j);
                String rid =
                        registrations.stream()
                                .filter(r -> r.text("username").equals(p.username()))
                                .findFirst()
                                .orElseThrow()
                                .text("id");
                service.assign(staff, rid, gid);
            }
        }
        List<String> rounds = new ArrayList<>();
        for (int i = 0; i < types.length; i++) {
            String rid = service.addRound(staff, cid, "轮次" + i, types[i], i + 1, 30);
            rounds.add(rid);
            service.addQuestionToRound(staff, rid, question("题目" + i), 1);
        }
        service.startCompetition(staff, cid);
        return new Fixture(cid, players, groups, rounds);
    }

    long count(String table) {
        return store.transaction(db -> db.one("SELECT COUNT(*) n FROM " + table).number("n"));
    }

    @Test
    void registrationAuthorityAndIndependentSessions() {
        assertFalse(service.needsSetup());
        assertThrows(
                IllegalArgumentException.class,
                () -> service.setupStaff("other", "secret12", "老师"));
        Session a = player("userA"), b = player("userB");
        assertEquals(a, service.login(false, "userA", "secret12"));
        assertThrows(BusinessException.class, () -> service.login(false, "userA", "bad"));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.register("userA", "secret12", new PlayerProfileInput("测试大学", "航空学院", "航空专业", "other001", "重复", "13800000001")));
        String cid = competition(1);
        service.join(a, cid, true);
        assertThrows(IllegalArgumentException.class, () -> service.join(a, cid, true));
        assertEquals(0, count("registration"));
        assertThrows(IllegalArgumentException.class, () -> service.join(a, cid, false));
        service.registrationState(staff, cid, "报名中");
        service.join(a, cid, false);
        assertThrows(IllegalArgumentException.class, () -> service.join(a, cid, false));
        assertThrows(IllegalArgumentException.class, () -> service.addGroup(a, cid, "A", 1));
        Session forged = new Session("fake", staff.id(), staff.username(), staff.name(), true);
        assertThrows(IllegalArgumentException.class, () -> service.questions(forged));
        service.logout(a);
        assertThrows(IllegalArgumentException.class, () -> service.mine(a));
        assertNotNull(service.mine(b));
        assertEquals(1, count("registration"));
        String hash =
                store.transaction(
                        db ->
                                db.one("SELECT password_hash FROM player WHERE id=?", b.id())
                                        .text("password_hash"));
        assertFalse(hash.contains("secret12"));
    }

    @Test
    void playerCompetitionListShowsParticipationAndAllowsNewRegistration() {
        Session player = player("newPlayer");
        String cid = competition(1);
        assertEquals("未参与", service.competitionsForPlayer(player).getFirst().text("my_status"));
        service.registrationState(staff, cid, "报名中");
        service.join(player, cid, false);
        assertEquals("已报名", service.competitionsForPlayer(player).getFirst().text("my_status"));
        assertThrows(IllegalArgumentException.class, () -> service.join(player, cid, false));
    }

    @Test
    void cancelledReservationAndRegistrationReuseTheirOriginalRecords() {
        Session player = player("cancelUser");
        String cid = competition(1);
        service.join(player, cid, true);
        service.cancelParticipation(player, cid, true);
        assertEquals("已取消预约", service.competitionsForPlayer(player).getFirst().text("my_status"));
        service.join(player, cid, true);
        assertEquals(1, count("reservation"));
        assertEquals("已预约", service.competitionsForPlayer(player).getFirst().text("my_status"));

        service.registrationState(staff, cid, "报名中");
        service.join(player, cid, false);
        assertEquals("已取消", store.transaction(db -> db.one(
                "SELECT status FROM reservation WHERE player_id=? AND competition_id=?", player.id(), cid).text("status")));
        service.cancelParticipation(player, cid, false);
        assertEquals("已取消报名", service.competitionsForPlayer(player).getFirst().text("my_status"));
        service.join(player, cid, false);
        assertEquals(1, count("registration"));
        assertEquals("已报名", service.competitionsForPlayer(player).getFirst().text("my_status"));
    }

    @Test
    void cancellationRespectsCompetitionTimeAndGrouping() {
        Session player = player("lockedCancel");
        String cid = competition(1);
        service.registrationState(staff, cid, "报名中");
        service.join(player, cid, false);
        clock.millis += 1001;
        assertThrows(IllegalArgumentException.class,
                () -> service.cancelParticipation(player, cid, false));
    }

    @Test
    void groupingAndQuestionConstraints() {
        Session p = player("userA");
        String cid = competition(1), other = competition(1);
        service.registrationState(staff, cid, "报名中");
        service.registrationState(staff, other, "报名中");
        service.join(p, cid, false);
        assertThrows(IllegalArgumentException.class, () -> service.addGroup(staff, cid, "A", 1));
        clock.millis += 1001;
        service.registrationState(staff, cid, "报名截止");
        service.registrationState(staff, other, "报名截止");
        String gid = service.addGroup(staff, cid, "A", 1),
                wrong = service.addGroup(staff, other, "B", 1),
                rid = service.people(staff, cid, false).getFirst().text("id");
        assertThrows(IllegalArgumentException.class, () -> service.assign(staff, rid, wrong));
        service.assign(staff, rid, gid);
        assertThrows(IllegalArgumentException.class, () -> service.assign(staff, rid, gid));
        String round = service.addRound(staff, cid, "必答", "REQUIRED", 1, 30),
                round2 = service.addRound(staff, cid, "抢答", "BUZZER", 2, 30),
                qid = question("测试题");
        service.addQuestionToRound(staff, round, qid, 1);
        assertThrows(
                IllegalArgumentException.class,
                () -> service.addQuestionToRound(staff, round2, qid, 1));
        assertThrows(IllegalArgumentException.class, () -> service.deleteQuestion(staff, qid));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        service.saveQuestion(
                                staff,
                                qid,
                                new QuestionInput(
                                        "修改", "民航史", List.of("甲", "乙", "丙", "丁"), "B", true)));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        service.saveQuestion(
                                staff,
                                null,
                                new QuestionInput("少选项", "民航史", List.of("甲", "乙"), "A", true)));
        assertThrows(IllegalArgumentException.class, () -> service.startCompetition(staff, cid));
        assertEquals(0, count("group_round"));
    }

    @Test
    void completeTwoGroupThreeRoundCompetitionAndArchive() {
        Fixture f = ready(2, 2, 2, "REQUIRED", "BUZZER", "RISK");
        assertEquals(4, service.ranking(staff, f.cid).size());
        assertNull(service.room(f.players.get(2), f.cid));
        for (int group = 0; group < 2; group++)
            for (int round = 0; round < 3; round++) {
                service.startRound(staff, f.cid);
                String release = service.publish(staff, f.cid);
                int g = group;
                if (group == 0) {
                    assertNull(service.room(f.players.get(2), f.cid));
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> service.submit(f.players.get(2), release, "A"));
                }
                assertThrows(
                        IllegalArgumentException.class, () -> service.closeQuestion(staff, f.cid));
                clock.millis += 500;
                assertEquals(
                        round == 2 ? 20 : 10,
                        service.submit(f.players.get(group * 2), release, "A"));
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.submit(f.players.get(g * 2), release, "A"));
                clock.millis += 250;
                assertEquals(
                        round == 0 ? 0 : round == 1 ? -10 : -20,
                        service.submit(f.players.get(group * 2 + 1), release, "B"));
                service.closeQuestion(staff, f.cid);
                service.finishRound(staff, f.cid);
            }
        var preview = service.preview(staff, f.cid);
        assertEquals(List.of(40, 40, -30, -30), preview.stream().map(RankingEntry::score).toList());
        assertEquals(2, preview.stream().filter(r -> r.promotion().equals("晋级（预览）")).count());
        assertEquals(preview.get(0).playerId().compareTo(preview.get(1).playerId()) < 0, true);
        service.archive(staff, f.cid);
        service.archive(staff, f.cid);
        assertEquals(4, count("result"));
        assertEquals(12, count("answer_record"));
        assertEquals(0, count("timeout_record"));
        assertEquals(1, service.history(f.players.getFirst()).size());
        String csv = service.exportCsv(staff, f.cid);
        assertTrue(csv.startsWith("\uFEFF名次"));
        assertEquals(5, csv.lines().count());
        assertTrue(csv.contains("未晋级"));
        assertThrows(IllegalArgumentException.class, () -> service.publish(staff, f.cid));
        var reopened = new QuizService(new Store(temp.resolve("v14.db")), clock);
        Session admin = reopened.login(true, "admin01", "secret12");
        assertEquals(service.ranking(staff, f.cid), reopened.ranking(admin, f.cid));
    }

    @Test
    void restartRetainsDeadlineAndTimeoutDoesNotDeduct() {
        Fixture f = ready(10, 1, 2, "RISK");
        service.startRound(staff, f.cid);
        String release = service.publish(staff, f.cid);
        long deadline = service.room(f.players.get(0), f.cid).deadline();
        clock.millis += 1000;
        service.submit(f.players.get(0), release, "A");
        service = new QuizService(new Store(temp.resolve("v14.db")), clock);
        staff = service.login(true, "admin01", "secret12");
        Session p = service.login(false, "user1", "secret12");
        assertEquals(deadline, service.room(p, f.cid).deadline());
        clock.millis = deadline;
        assertThrows(IllegalArgumentException.class, () -> service.submit(p, release, "B"));
        assertEquals(1, count("timeout_record"));
        service.recover();
        service.recover();
        assertEquals(1, count("timeout_record"));
        assertEquals(1, count("answer_record"));
        assertEquals(
                0,
                service.ranking(staff, f.cid).stream()
                        .filter(r -> r.playerId().equals(p.id()))
                        .findFirst()
                        .orElseThrow()
                        .score());
        service.finishRound(staff, f.cid);
        service.archive(staff, f.cid);
        assertEquals(
                2,
                service.ranking(staff, f.cid).stream()
                        .filter(r -> r.promotion().equals("晋级"))
                        .count());
    }

    @Test
    void concurrentDuplicateSubmitAndTimeoutAreExclusive() throws Exception {
        Fixture f = ready(1, 1, 1, "BUZZER");
        service.startRound(staff, f.cid);
        String release = service.publish(staff, f.cid);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> results =
                    pool.invokeAll(
                            List.of(
                                    () -> {
                                        try {
                                            service.submit(f.players.getFirst(), release, "A");
                                            return true;
                                        } catch (IllegalArgumentException e) {
                                            return false;
                                        }
                                    },
                                    () -> {
                                        try {
                                            service.submit(f.players.getFirst(), release, "A");
                                            return true;
                                        } catch (IllegalArgumentException e) {
                                            return false;
                                        }
                                    }));
            assertEquals(
                    1,
                    results.stream()
                            .filter(
                                    r -> {
                                        try {
                                            return r.get();
                                        } catch (Exception e) {
                                            throw new RuntimeException(e);
                                        }
                                    })
                            .count());
        } finally {
            pool.shutdownNow();
        }
        clock.millis += 30_000;
        service.recover();
        assertEquals(1, count("answer_record"));
        assertEquals(0, count("timeout_record"));
    }

    @Test
    void archiveRollsBackEveryResultAndStatusOnFailure() {
        Fixture f = ready(1, 1, 2, "REQUIRED");
        service.startRound(staff, f.cid);
        service.publish(staff, f.cid);
        clock.millis += 30_000;
        service.recover();
        service.finishRound(staff, f.cid);
        store.transaction(
                db -> {
                    db.execute(
                            "CREATE TRIGGER fail_second BEFORE INSERT ON result WHEN (SELECT"
                                    + " COUNT(*) FROM result)=1 BEGIN SELECT RAISE(ABORT,'simulated"
                                    + " disk failure'); END");
                    return null;
                });
        assertThrows(DataAccessException.class, () -> service.archive(staff, f.cid));
        assertEquals(0, count("result"));
        assertEquals("比赛中", service.competitions().getFirst().text("status"));
        store.transaction(
                db -> {
                    db.execute("DROP TRIGGER fail_second");
                    return null;
                });
        service.archive(staff, f.cid);
        assertEquals(2, count("result"));
    }

    @Test
    void foreignKeysAndTransactionsProtectDatabase() {
        assertThrows(
                DataAccessException.class,
                () ->
                        store.transaction(
                                db -> {
                                    db.execute(
                                            "INSERT INTO reservation"
                                                    + " VALUES('bad','missing','missing',0,'有效')");
                                    return null;
                                }));
        assertEquals(0, count("reservation"));
        assertEquals(
                16,
                store.transaction(db -> db.one("PRAGMA user_version").number("user_version"))
                        .intValue());
        new Store(temp.resolve("v14.db"));
        assertEquals(1, count("staff"));
    }

    @Test
    void playerProfileIsRequiredAndStudentNumberIsUniqueWithinSchool() {
        assertThrows(IllegalArgumentException.class,
                () -> service.register("profile1", "secret12", new PlayerProfileInput("测试大学", "航空学院", "飞行专业", "20260001", "张三", "")));
        service.register("profile1", "secret12", new PlayerProfileInput("甲大学", "航空学院", "飞行专业", "20260001", "张三", "13800000001"));
        assertThrows(IllegalArgumentException.class,
                () -> service.register("profile2", "secret12", new PlayerProfileInput("甲大学", "其他学院", "其他专业", "20260001", "李四", "13800000002")));
        service.register("profile2", "secret12", new PlayerProfileInput("乙大学", "航空学院", "飞行专业", "20260001", "李四", "13800000002"));
        Session player = service.login(false, "profile1", "secret12");
        assertTrue(service.profileComplete(player));
        assertEquals("甲大学", service.playerProfile(player).text("school"));
        service.updatePlayerProfile(player, new PlayerProfileInput("甲大学", "工程学院", "动力工程", "20260003", "张三", "13900000001"));
        assertEquals("20260003", service.playerProfile(player).text("student_number"));
    }

    @Test
    void competitionCategoriesRestrictRoundQuestions() {
        long now = clock.millis();
        String cid = service.saveCompetition(staff, null,
                new CompetitionInput("民航史专题赛", "分类约束", now + 1_000, now + 2_000,
                        now + 3_000, 1, Set.of("民航史")));
        String rid = service.addRound(staff, cid, "第一轮", "REQUIRED", 1, 30);
        String history = service.saveQuestion(staff, null,
                new QuestionInput("ICAO的中文名称是什么？", "民航史", List.of("国际民用航空组织", "国际航协", "民航局", "机场协会"), "A", true));
        String principles = service.saveQuestion(staff, null,
                new QuestionInput("升力方向通常如何？", "飞行原理", List.of("向上", "向下", "向前", "向后"), "A", true));
        service.addQuestionToRound(staff, rid, history, 1);
        assertThrows(IllegalArgumentException.class,
                () -> service.addQuestionToRound(staff, rid, principles, 2));
        assertThrows(IllegalArgumentException.class,
                () -> service.saveCompetition(staff, cid,
                        new CompetitionInput("民航史专题赛", "分类约束", now + 1_000, now + 2_000,
                                now + 3_000, 1, Set.of("航空法规"))));
    }

    @Test
    void version14PlayerDataMigratesWithoutLoss() throws Exception {
        Path file = temp.resolve("legacy.db");
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
                Statement statement = c.createStatement()) {
            statement.execute("CREATE TABLE player(id TEXT PRIMARY KEY,username TEXT,password_hash TEXT,name TEXT,phone TEXT)");
            statement.execute("CREATE TABLE competition(id TEXT PRIMARY KEY)");
            statement.execute("INSERT INTO player VALUES('P1','legacy','hash','旧选手','13800000000')");
            statement.execute("PRAGMA user_version=14");
        }
        Store migrated = new Store(file);
        assertEquals(16L, migrated.transaction(db -> db.one("PRAGMA user_version").number("user_version")).longValue());
        assertEquals("旧选手", migrated.transaction(db -> db.one("SELECT * FROM player WHERE id='P1'").text("name")));
        assertEquals(0L, migrated.transaction(db -> db.one("SELECT * FROM player WHERE id='P1'").number("profile_complete")).longValue());
    }

    @Test
    void roundPolymorphismUsesFixedRules() {
        assertEquals(10, new RequiredRound().calculateScore(true));
        assertEquals(0, new RequiredRound().calculateScore(false));
        assertEquals(10, new BuzzerRound().calculateScore(true));
        assertEquals(-10, new BuzzerRound().calculateScore(false));
        assertEquals(20, new RiskRound().calculateScore(true));
        assertEquals(-20, new RiskRound().calculateScore(false));
    }

    @Test
    void rankingUsesCorrectCountBeforeTimeAndThenId() {
        Fixture f = ready(2, 1, 4, "REQUIRED", "REQUIRED", "RISK");
        for (int i = 0; i < 2; i++) {
            service.startRound(staff, f.cid);
            String release = service.publish(staff, f.cid);
            clock.millis += 100;
            service.submit(f.players.get(0), release, "A");
            clock.millis += 30_000;
            service.recover();
            service.finishRound(staff, f.cid);
        }
        service.startRound(staff, f.cid);
        String release = service.publish(staff, f.cid);
        clock.millis += 50;
        service.submit(f.players.get(1), release, "A");
        clock.millis += 50;
        service.submit(f.players.get(2), release, "A");
        service.submit(f.players.get(3), release, "A");
        clock.millis += 30_000;
        service.recover();
        service.finishRound(staff, f.cid);
        var ranked = service.preview(staff, f.cid);
        assertTrue(ranked.stream().allMatch(r -> r.score() == 20));
        assertEquals(f.players.get(0).id(), ranked.get(0).playerId());
        assertEquals(2, ranked.get(0).correctCount());
        assertEquals(200, ranked.get(0).elapsedMillis());
        assertEquals(f.players.get(1).id(), ranked.get(1).playerId());
        assertEquals(50, ranked.get(1).elapsedMillis());
        assertTrue(ranked.get(2).playerId().compareTo(ranked.get(3).playerId()) < 0);
    }

    @Test
    void databaseGuardsRejectCrossGroupAndConflictingSettlement() {
        Fixture f = ready(1, 2, 1, "BUZZER");
        service.startRound(staff, f.cid);
        String release = service.publish(staff, f.cid);
        assertThrows(
                DataAccessException.class,
                () ->
                        store.transaction(
                                db -> {
                                    db.execute(
                                            "INSERT INTO answer_record VALUES('invalid',?,?,"
                                                    + " 'A',1,10,?)",
                                            release,
                                            f.players.get(1).id(),
                                            clock.millis);
                                    return null;
                                }));
        service.submit(f.players.get(0), release, "A");
        clock.millis += 30_000;
        assertThrows(
                DataAccessException.class,
                () ->
                        store.transaction(
                                db -> {
                                    db.execute(
                                            "INSERT INTO timeout_record VALUES('conflict',?,?,?)",
                                            release,
                                            f.players.get(0).id(),
                                            clock.millis);
                                    return null;
                                }));
        assertEquals(1, count("answer_record"));
        assertEquals(0, count("timeout_record"));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.updateRound(staff, f.rounds.get(0), "改名", "RISK", 1, 90));
        assertThrows(
                IllegalArgumentException.class,
                () -> service.deleteEmptyGroup(staff, f.groups.get(0)));
    }
}
