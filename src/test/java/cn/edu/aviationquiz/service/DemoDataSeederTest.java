package cn.edu.aviationquiz.service;

import static org.junit.jupiter.api.Assertions.*;

import cn.edu.aviationquiz.dao.Store;
import cn.edu.aviationquiz.dao.jdbc.JdbcAccountDao;
import cn.edu.aviationquiz.dao.jdbc.JdbcCompetitionDao;
import cn.edu.aviationquiz.dao.jdbc.JdbcGameDao;
import cn.edu.aviationquiz.dao.jdbc.JdbcQuestionBankDao;
import cn.edu.aviationquiz.dao.jdbc.JdbcRegistrationDao;
import cn.edu.aviationquiz.dao.jdbc.JdbcResultDao;
import cn.edu.aviationquiz.service.impl.CompetitionExecutionServiceImpl;
import cn.edu.aviationquiz.service.impl.QuestionBankServiceImpl;
import cn.edu.aviationquiz.service.impl.AccountManagementServiceImpl;
import cn.edu.aviationquiz.service.impl.CompetitionManagementServiceImpl;
import cn.edu.aviationquiz.service.impl.RegistrationManagementServiceImpl;
import cn.edu.aviationquiz.service.impl.ResultServiceImpl;
import cn.edu.aviationquiz.service.impl.StandardRoundFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.*;

class DemoDataSeederTest {
    @TempDir Path temp;

    @Test
    void createsCompleteIdempotentDemoData() {
        Store store = new Store(temp.resolve("demo.db"));
        Clock clock = Clock.fixed(Instant.parse("2026-09-12T06:00:00Z"), ZoneOffset.UTC);
        assertTrue(DemoDataSeeder.seed(store, clock));
        assertFalse(DemoDataSeeder.seed(store, clock));
        store.transaction(
                db -> {
                    assertEquals(80, db.one("SELECT COUNT(*) n FROM player").number("n"));
                    assertEquals(60, db.one("SELECT COUNT(*) n FROM question").number("n"));
                    assertEquals(12, db.one("SELECT COUNT(*) n FROM competition").number("n"));
                    assertEquals(24, db.one("SELECT COUNT(*) n FROM competition_category").number("n"));
                    assertEquals(526, db.one("SELECT COUNT(*) n FROM registration").number("n"));
                    assertEquals(31, db.one("SELECT COUNT(*) n FROM reservation").number("n"));
                    assertEquals(12, db.one("SELECT COUNT(*) n FROM competition_group").number("n"));
                    assertEquals(480, db.one("SELECT COUNT(*) n FROM group_assignment").number("n"));
                    assertEquals(3, db.one("SELECT COUNT(*) n FROM competition_round").number("n"));
                    assertEquals(9, db.one("SELECT COUNT(*) n FROM round_question").number("n"));
                    assertEquals(240, db.one("SELECT COUNT(*) n FROM result").number("n"));
                    assertEquals("ok", db.one("PRAGMA integrity_check").text("integrity_check"));
                    return null;
                });
        RoundFactory roundFactory = new StandardRoundFactory();
        QuizService service =
                new QuizService(
                        store,
                        clock,
                        new AccountManagementServiceImpl(new JdbcAccountDao(store)),
                        new CompetitionManagementServiceImpl(
                                new JdbcCompetitionDao(store), clock),
                        new CompetitionExecutionServiceImpl(
                                new JdbcGameDao(store), clock, roundFactory),
                        new QuestionBankServiceImpl(
                                new JdbcQuestionBankDao(store), roundFactory),
                        new RegistrationManagementServiceImpl(
                                new JdbcRegistrationDao(store), clock),
                        new ResultServiceImpl(new JdbcResultDao(store)));
        var player = service.login(false, "player01", PASSWORD_FOR_TESTS);
        assertEquals(10, service.mine(player).size());
        assertEquals(3, service.history(player).size());
    }

    private static final String PASSWORD_FOR_TESTS = "123456";
}
