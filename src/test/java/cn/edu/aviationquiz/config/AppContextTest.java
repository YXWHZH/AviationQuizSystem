package cn.edu.aviationquiz.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Clock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AppContextTest {
    @TempDir Path temp;

    @Test
    void assemblesApplicationDependenciesAroundAProvidedDatabase() {
        try (AppContext context = AppContext.create(temp.resolve("application.db"), Clock.systemUTC())) {
            assertTrue(context.runtime().service.needsSetup());
        }
    }
}
