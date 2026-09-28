package cn.edu.aviationquiz;

import cn.edu.aviationquiz.config.AppContext;
import cn.edu.aviationquiz.ui.QuizWindows;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.time.Clock;

public final class App extends Application {
    private AppContext context;

    @Override
    public void init() {
        context =
                AppContext.create(
                        Path.of(
                                System.getProperty(
                                        "quiz.database", "data/aviation_quiz_v14.db")),
                        Clock.systemUTC());
    }

    @Override
    public void start(Stage stage) {
        Platform.setImplicitExit(false);
        new QuizWindows(context.runtime()).open(stage);
    }

    @Override
    public void stop() {
        if (context != null) context.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
