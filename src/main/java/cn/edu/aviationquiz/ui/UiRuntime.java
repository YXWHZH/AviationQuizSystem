package cn.edu.aviationquiz.ui;

import javafx.application.Platform;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/** All service calls run on one worker; listeners and results run on the FX thread. */
public final class UiRuntime implements AutoCloseable {
    private final UiDependencies dependencies;
    private final ScheduledExecutorService worker =
            Executors.newSingleThreadScheduledExecutor(
                    r -> {
                        Thread t = new Thread(r, "quiz-business");
                        t.setDaemon(true);
                        return t;
                    });
    private final List<Runnable> listeners = new ArrayList<>();
    private Consumer<Throwable> backgroundError = Throwable::printStackTrace;

    public UiRuntime(UiDependencies dependencies) {
        this.dependencies = dependencies;
        worker.scheduleWithFixedDelay(
                () -> {
                    try {
                        if (dependencies.recovery().recover())
                            Platform.runLater(this::changed);
                    } catch (Exception e) {
                        Platform.runLater(() -> backgroundError.accept(e));
                    }
                },
                1,
                1,
                TimeUnit.SECONDS);
    }

    public UiDependencies dependencies() {
        return dependencies;
    }

    public void onBackgroundError(Consumer<Throwable> handler) {
        backgroundError = handler;
    }

    public void listen(Runnable listener) {
        listeners.add(listener);
    }

    public void unlisten(Runnable listener) {
        listeners.remove(listener);
    }

    public void changed() {
        List.copyOf(listeners).forEach(Runnable::run);
    }

    public <T> void read(Callable<T> call, Consumer<T> success, Consumer<Throwable> failure) {
        worker.execute(
                () -> {
                    try {
                        T value = call.call();
                        Platform.runLater(() -> success.accept(value));
                    } catch (Exception e) {
                        Platform.runLater(() -> failure.accept(e));
                    }
                });
    }

    public <T> void mutate(Callable<T> call, Consumer<T> success, Consumer<Throwable> failure) {
        read(
                call,
                value -> {
                    success.accept(value);
                    changed();
                },
                failure);
    }

    @Override
    public void close() {
        worker.shutdown();
    }
}
