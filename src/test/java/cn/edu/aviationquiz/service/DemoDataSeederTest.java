package cn.edu.aviationquiz.service;

import static org.junit.jupiter.api.Assertions.*;

import cn.edu.aviationquiz.dao.Store;

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
                    assertEquals(30, db.one("SELECT COUNT(*) n FROM player").number("n"));
                    assertEquals(60, db.one("SELECT COUNT(*) n FROM question").number("n"));
                    assertEquals(12, db.one("SELECT COUNT(*) n FROM competition").number("n"));
                    assertEquals(24, db.one("SELECT COUNT(*) n FROM competition_category").number("n"));
                    assertEquals(226, db.one("SELECT COUNT(*) n FROM registration").number("n"));
                    assertEquals(31, db.one("SELECT COUNT(*) n FROM reservation").number("n"));
                    assertEquals(12, db.one("SELECT COUNT(*) n FROM competition_group").number("n"));
                    assertEquals(180, db.one("SELECT COUNT(*) n FROM group_assignment").number("n"));
                    assertEquals(3, db.one("SELECT COUNT(*) n FROM competition_round").number("n"));
                    assertEquals(9, db.one("SELECT COUNT(*) n FROM round_question").number("n"));
                    assertEquals(90, db.one("SELECT COUNT(*) n FROM result").number("n"));
                    assertEquals("ok", db.one("PRAGMA integrity_check").text("integrity_check"));
                    return null;
                });
        QuizService service = new QuizService(store, clock);
        var player = service.login(false, "player01", PASSWORD_FOR_TESTS);
        assertEquals(10, service.mine(player).size());
        assertEquals(3, service.history(player).size());
    }

    private static final String PASSWORD_FOR_TESTS = "123456";
}
