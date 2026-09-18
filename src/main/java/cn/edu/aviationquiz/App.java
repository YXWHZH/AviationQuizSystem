package cn.edu.aviationquiz;

import cn.edu.aviationquiz.dao.Store;
import cn.edu.aviationquiz.service.QuizService;
import cn.edu.aviationquiz.ui.QuizWindows;
import cn.edu.aviationquiz.ui.UiRuntime;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.time.Clock;

public final class App extends Application {
    private UiRuntime runtime;

    @Override
    public void init() {
        runtime =
                new UiRuntime(
                        new QuizService(
                                new Store(
                                        Path.of(
                                                System.getProperty(
                                                        "quiz.database",
                                                        "data/aviation_quiz_v14.db"))),
                                Clock.systemUTC()));
    }

    @Override
    public void start(Stage stage) {
        Platform.setImplicitExit(false);
        new QuizWindows(runtime).open(stage);
    }

    @Override
    public void stop() {
        if (runtime != null) runtime.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
