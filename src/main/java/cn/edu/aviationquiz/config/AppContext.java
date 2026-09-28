package cn.edu.aviationquiz.config;

import cn.edu.aviationquiz.dao.Store;
import cn.edu.aviationquiz.dao.jdbc.JdbcGameDao;
import cn.edu.aviationquiz.dao.jdbc.JdbcRegistrationDao;
import cn.edu.aviationquiz.dao.jdbc.JdbcResultDao;
import cn.edu.aviationquiz.service.QuizService;
import cn.edu.aviationquiz.service.RoundFactory;
import cn.edu.aviationquiz.service.impl.CompetitionExecutionServiceImpl;
import cn.edu.aviationquiz.service.impl.RegistrationManagementServiceImpl;
import cn.edu.aviationquiz.service.impl.ResultServiceImpl;
import cn.edu.aviationquiz.service.impl.StandardRoundFactory;
import cn.edu.aviationquiz.ui.UiRuntime;

import java.nio.file.Path;
import java.time.Clock;

/**
 * Application composition root. Concrete infrastructure is created only here and injected into
 * the upper layers.
 */
public final class AppContext implements AutoCloseable {
    private final UiRuntime runtime;

    private AppContext(UiRuntime runtime) {
        this.runtime = runtime;
    }

    public static AppContext create(Path database, Clock clock) {
        Store store = new Store(database);
        RoundFactory roundFactory = new StandardRoundFactory();
        QuizService service =
                new QuizService(
                        store,
                        clock,
                        roundFactory,
                        new CompetitionExecutionServiceImpl(
                                new JdbcGameDao(store), clock, roundFactory),
                        new RegistrationManagementServiceImpl(
                                new JdbcRegistrationDao(store), clock),
                        new ResultServiceImpl(new JdbcResultDao(store)));
        return new AppContext(new UiRuntime(service));
    }

    public UiRuntime runtime() {
        return runtime;
    }

    @Override
    public void close() {
        runtime.close();
    }
}
