package cn.edu.aviationquiz.config;

import cn.edu.aviationquiz.dao.Store;
import cn.edu.aviationquiz.dao.jdbc.JdbcAccountDao;
import cn.edu.aviationquiz.dao.jdbc.JdbcCompetitionDao;
import cn.edu.aviationquiz.dao.jdbc.JdbcCompetitionLiveDao;
import cn.edu.aviationquiz.dao.jdbc.JdbcGameDao;
import cn.edu.aviationquiz.dao.jdbc.JdbcQuestionBankDao;
import cn.edu.aviationquiz.dao.jdbc.JdbcRegistrationDao;
import cn.edu.aviationquiz.dao.jdbc.JdbcResultDao;
import cn.edu.aviationquiz.controller.AccountController;
import cn.edu.aviationquiz.controller.CompetitionLiveController;
import cn.edu.aviationquiz.controller.CompetitionManagementController;
import cn.edu.aviationquiz.controller.CompetitionRoomController;
import cn.edu.aviationquiz.controller.QuestionBankController;
import cn.edu.aviationquiz.controller.RegistrationController;
import cn.edu.aviationquiz.controller.ResultController;
import cn.edu.aviationquiz.service.QuizService;
import cn.edu.aviationquiz.service.RoundFactory;
import cn.edu.aviationquiz.service.impl.AccountManagementServiceImpl;
import cn.edu.aviationquiz.service.impl.CompetitionManagementServiceImpl;
import cn.edu.aviationquiz.service.impl.CompetitionLiveServiceImpl;
import cn.edu.aviationquiz.service.impl.CompetitionExecutionServiceImpl;
import cn.edu.aviationquiz.service.impl.QuestionBankServiceImpl;
import cn.edu.aviationquiz.service.impl.RegistrationManagementServiceImpl;
import cn.edu.aviationquiz.service.impl.ResultServiceImpl;
import cn.edu.aviationquiz.service.impl.StandardRoundFactory;
import cn.edu.aviationquiz.ui.UiRuntime;
import cn.edu.aviationquiz.ui.UiDependencies;

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
                        clock,
                        new AccountManagementServiceImpl(new JdbcAccountDao(store)),
                        new CompetitionManagementServiceImpl(
                                new JdbcCompetitionDao(store), clock),
                        new CompetitionLiveServiceImpl(
                                new JdbcCompetitionLiveDao(store), clock),
                        new CompetitionExecutionServiceImpl(
                                new JdbcGameDao(store), clock, roundFactory),
                        new QuestionBankServiceImpl(
                                new JdbcQuestionBankDao(store), roundFactory),
                        new RegistrationManagementServiceImpl(
                                new JdbcRegistrationDao(store), clock),
                        new ResultServiceImpl(new JdbcResultDao(store)));
        UiDependencies ui =
                new UiDependencies(
                        new AccountController(service),
                        new CompetitionLiveController(service),
                        new CompetitionManagementController(service),
                        new CompetitionRoomController(service),
                        new QuestionBankController(service),
                        new RegistrationController(service),
                        new ResultController(service),
                        service);
        return new AppContext(new UiRuntime(ui));
    }

    public UiRuntime runtime() {
        return runtime;
    }

    @Override
    public void close() {
        runtime.close();
    }
}
