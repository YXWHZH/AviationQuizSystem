package cn.edu.aviationquiz.ui;

import static org.junit.jupiter.api.Assertions.*;

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
import cn.edu.aviationquiz.entity.Models.*;
import cn.edu.aviationquiz.service.QuizService;
import cn.edu.aviationquiz.service.RoundFactory;
import cn.edu.aviationquiz.service.impl.CompetitionExecutionServiceImpl;
import cn.edu.aviationquiz.service.impl.QuestionBankServiceImpl;
import cn.edu.aviationquiz.service.impl.AccountManagementServiceImpl;
import cn.edu.aviationquiz.service.impl.CompetitionManagementServiceImpl;
import cn.edu.aviationquiz.service.impl.CompetitionLiveServiceImpl;
import cn.edu.aviationquiz.service.impl.RegistrationManagementServiceImpl;
import cn.edu.aviationquiz.service.impl.ResultServiceImpl;
import cn.edu.aviationquiz.service.impl.StandardRoundFactory;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.stage.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;

import javax.imageio.ImageIO;

/** Opt-in JavaFX scene test; uses its own temporary database and only application windows. */
@EnabledIfSystemProperty(named = "quiz.uiTest", matches = "true")
class QuizWindowsTest {
    @TempDir Path temp;

    static <T> T fx(Callable<T> call) throws Exception {
        FutureTask<T> task = new FutureTask<>(call);
        Platform.runLater(task);
        return task.get(10, TimeUnit.SECONDS);
    }

    static void await(BooleanSupplier predicate) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < end) {
            if (fx(predicate::getAsBoolean)) return;
            Thread.sleep(50);
        }
        fail("UI condition did not become true");
    }

    static List<Node> nodes(Node node) {
        List<Node> list = new ArrayList<>();
        list.add(node);
        if (node instanceof Parent p)
            for (Node child : p.getChildrenUnmodifiable()) list.addAll(nodes(child));
        return list;
    }

    static boolean shown(Node n) {
        for (Node p = n; p != null; p = p.getParent()) if (!p.isVisible()) return false;
        return true;
    }

    static Button button(Stage s, String text) {
        return nodes(s.getScene().getRoot()).stream()
                .filter(n -> shown(n) && n instanceof Button b && b.getText().equals(text))
                .map(n -> (Button) n)
                .findFirst()
                .orElseThrow();
    }

    static Stage named(String title) {
        return Window.getWindows().stream()
                .filter(w -> w instanceof Stage s && s.getTitle().equals(title))
                .map(w -> (Stage) w)
                .findFirst()
                .orElse(null);
    }

    static boolean text(Stage s, String value) {
        return nodes(s.getScene().getRoot()).stream()
                .anyMatch(
                        n ->
                                n instanceof Labeled l
                                        && l.getText() != null
                                        && l.getText().contains(value));
    }

    static void resize(Stage stage, double width, double height) throws Exception {
        fx(
                () -> {
                    stage.setMaximized(false);
                    stage.setWidth(width);
                    stage.setHeight(height);
                    stage.getScene().getRoot().applyCss();
                    stage.getScene().getRoot().layout();
                    return null;
                });
        await(
                () ->
                        Math.abs(stage.getWidth() - width) < 40
                                && Math.abs(stage.getHeight() - height) < 40
                                && Math.abs(stage.getScene().getWidth() - width) < 60
                                && Math.abs(stage.getScene().getHeight() - height) < 80);
    }

    static void assertViewportFilled(Stage stage) throws Exception {
        double[] sizes =
                fx(
                        () -> {
                            Parent root = stage.getScene().getRoot();
                            root.applyCss();
                            root.layout();
                            return new double[] {
                                stage.getScene().getWidth(),
                                stage.getScene().getHeight(),
                                root.getLayoutBounds().getWidth(),
                                root.getLayoutBounds().getHeight()
                            };
                        });
        assertEquals(sizes[0], sizes[2], 1.5, "根节点应铺满 Scene 宽度");
        assertEquals(sizes[1], sizes[3], 1.5, "根节点应铺满 Scene 高度");
    }

    static void assertInsideScene(Stage stage, Node node) throws Exception {
        double[] bounds =
                fx(
                        () -> {
                            Bounds b = node.localToScene(node.getBoundsInLocal());
                            return new double[] {
                                b.getMinX(), b.getMinY(), b.getMaxX(), b.getMaxY(),
                                stage.getScene().getWidth(), stage.getScene().getHeight()
                            };
                        });
        assertTrue(bounds[0] >= -1 && bounds[1] >= -1, "组件不应越过 Scene 左上边界");
        assertTrue(bounds[2] <= bounds[4] + 1 && bounds[3] <= bounds[5] + 1, "组件不应越过 Scene 右下边界");
    }

    static void assertTableUsesAvailableSpace(Stage stage) throws Exception {
        double[] size =
                fx(
                        () -> {
                            TableView<?> table =
                                    nodes(stage.getScene().getRoot()).stream()
                                            .filter(n -> shown(n) && n instanceof TableView<?>)
                                            .map(n -> (TableView<?>) n)
                                            .findFirst()
                                            .orElseThrow();
                            assertSame(
                                    TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN,
                                    table.getColumnResizePolicy());
                            return new double[] {
                                table.getWidth(), table.getHeight(), stage.getScene().getWidth()
                            };
                        });
        assertTrue(size[0] >= Math.min(500, size[2] - 140), "表格应使用主要可用宽度");
        assertTrue(size[1] >= 180, "表格应使用主要可用高度");
    }

    static void snapshot(Stage stage, String name) throws Exception {
        fx(
                () -> {
                    stage.getScene().getRoot().applyCss();
                    stage.getScene().getRoot().layout();
                    WritableImage image = stage.getScene().snapshot(null);
                    BufferedImage output =
                            new BufferedImage(
                                    (int) image.getWidth(),
                                    (int) image.getHeight(),
                                    BufferedImage.TYPE_INT_ARGB);
                    for (int y = 0; y < output.getHeight(); y++)
                        for (int x = 0; x < output.getWidth(); x++)
                            output.setRGB(x, y, image.getPixelReader().getArgb(x, y));
                    Path dir = Path.of("target", "ui-snapshots");
                    Files.createDirectories(dir);
                    ImageIO.write(output, "png", dir.resolve(name + ".png").toFile());
                    return null;
                });
    }

    static void login(Stage entry, boolean staff, String account) throws Exception {
        double previousWidth = fx(() -> entry.getScene().getWidth());
        fx(
                () -> {
                    button(entry, staff ? "工作人员登录" : "选手登录").fire();
                    return null;
                });
        await(() -> Math.abs(entry.getScene().getWidth() - previousWidth) < 2);
        assertViewportFilled(entry);
        Stage form = entry;
        fx(
                () -> {
                    List<TextField> fields =
                            nodes(form.getScene().getRoot()).stream()
                                    .filter(n -> n instanceof TextField)
                                    .map(n -> (TextField) n)
                                    .toList();
                    fields.get(0).setText(account);
                    fields.get(1).setText("secret12");
                    button(form, "登录").fire();
                    return null;
                });
        await(() -> entry.getTitle().endsWith(" · " + account));
    }

    static TabPane tabPane(Stage stage, String styleClass) {
        return nodes(stage.getScene().getRoot()).stream()
                .filter(n -> n instanceof TabPane p && p.getStyleClass().contains(styleClass))
                .map(n -> (TabPane) n)
                .findFirst()
                .orElseThrow();
    }

    static void assertResponsiveTabs(Stage stage, String styleClass) throws Exception {
        TabPane tabs = fx(() -> tabPane(stage, styleClass));
        int selected = fx(() -> tabs.getSelectionModel().getSelectedIndex());
        for (int i = 0; i < fx(() -> tabs.getTabs().size()); i++) {
            int index = i;
            fx(
                    () -> {
                        tabs.getSelectionModel().select(index);
                        stage.getScene().getRoot().applyCss();
                        stage.getScene().getRoot().layout();
                        return null;
                    });
            assertViewportFilled(stage);
            boolean hasTable =
                    fx(
                            () ->
                                    nodes(stage.getScene().getRoot()).stream()
                                            .anyMatch(n -> shown(n) && n instanceof TableView<?>));
            if (hasTable) assertTableUsesAvailableSpace(stage);
        }
        fx(
                () -> {
                    tabs.getSelectionModel().select(selected);
                    return null;
                });
    }

    @Test
    void windowsPublishAndSubmitWithoutLeakingAnotherGroup() throws Exception {
        CountDownLatch startup = new CountDownLatch(1);
        Platform.startup(startup::countDown);
        assertTrue(startup.await(10, TimeUnit.SECONDS));
        Platform.setImplicitExit(false);
        List<Throwable> errors = new CopyOnWriteArrayList<>();
        fx(
                () -> {
                    Thread.currentThread().setUncaughtExceptionHandler((t, e) -> errors.add(e));
                    return null;
                });
        Store store = new Store(temp.resolve("ui.db"));
        Clock clock = Clock.systemUTC();
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
        service.setupStaff("admin01", "secret12", "李老师");
        Session staff = service.login(true, "admin01", "secret12");
        service.register("userA", "secret12", new PlayerProfileInput("测试大学", "航空学院", "飞行专业", "20260001", "张三", "13800000001"));
        service.register("userB", "secret12", new PlayerProfileInput("测试大学", "航空学院", "飞行专业", "20260002", "李四", "13800000002"));
        Session a = service.login(false, "userA", "secret12"),
                b = service.login(false, "userB", "secret12");
        long now = System.currentTimeMillis();
        String cid =
                service.saveCompetition(
                        staff,
                        null,
                        new CompetitionInput(
                                "航空知识竞赛", "多窗口验证", now - 1000, now + 60_000, now + 120_000, 1));
        service.registrationState(staff, cid, "报名中");
        service.join(a, cid, false);
        service.join(b, cid, false);
        service.saveCompetition(
                staff,
                cid,
                new CompetitionInput(
                        "航空知识竞赛", "多窗口验证", now - 60_000, now - 1000, now + 120_000, 1));
        service.registrationState(staff, cid, "报名截止");
        String ga = service.addGroup(staff, cid, "A组", 1),
                gb = service.addGroup(staff, cid, "B组", 2);
        for (var r : service.participants(staff, cid, false))
            service.assign(staff, r.id(), r.username().equals("userA") ? ga : gb);
        String rid = service.addRound(staff, cid, "必答轮", "REQUIRED", 1, 120);
        String qid =
                service.saveQuestion(
                        staff,
                        null,
                        new QuestionInput(
                                "飞机升力主要由什么产生？",
                                "飞行原理",
                                List.of("机翼上下表面的压强差", "客舱灯光", "座椅数量", "机场名称"),
                                "A",
                                true));
        service.addQuestionToRound(staff, rid, qid, 1);
        UiRuntime runtime =
                new UiRuntime(
                        new UiDependencies(
                                new AccountController(service),
                                new CompetitionLiveController(service),
                                new CompetitionManagementController(service),
                                new CompetitionRoomController(service),
                                new QuestionBankController(service),
                                new RegistrationController(service),
                                new ResultController(service),
                                service));
        Stage entry =
                fx(
                        () -> {
                            Stage s = new Stage();
                            new QuizWindows(runtime).open(s);
                            return s;
                        });
        try {
            await(() -> text(entry, "航空知识竞赛"));
            assertTrue(fx(entry::isMaximized), "主 Stage 启动后应默认最大化");
            resize(entry, 1920, 1080);
            assertViewportFilled(entry);
            assertTableUsesAvailableSpace(entry);
            assertInsideScene(entry, button(entry, "选手登录"));
            resize(entry, 1366, 768);
            assertViewportFilled(entry);
            assertTableUsesAvailableSpace(entry);
            assertInsideScene(entry, button(entry, "工作人员登录"));
            resize(entry, 700, 600);
            assertViewportFilled(entry);
            assertTableUsesAvailableSpace(entry);
            assertInsideScene(entry, button(entry, "选手登录"));
            fx(
                    () -> {
                        entry.setMaximized(true);
                        return null;
                    });
            assertTrue(fx(entry::isMaximized), "重新最大化后应保持最大化状态");
            resize(entry, 1440, 900);
            await(() -> entry.getScene().getWidth() > 1400 && entry.getScene().lookup("#competition-wide-grid") != null);
            fx(
                    () -> {
                        entry.getScene().getRoot().applyCss();
                        entry.getScene().getRoot().layout();
                        return null;
                    });
            double[] firstWidths =
                    fx(
                            () ->
                                    new double[] {
                                        entry.getScene().lookup("#competition-list-card").getBoundsInParent().getWidth(),
                                        entry.getScene().lookup("#competition-detail-card").getBoundsInParent().getWidth()
                                    });
            Thread.sleep(250);
            double[] settledWidths =
                    fx(
                            () ->
                                    new double[] {
                                        entry.getScene().lookup("#competition-list-card").getBoundsInParent().getWidth(),
                                        entry.getScene().lookup("#competition-detail-card").getBoundsInParent().getWidth()
                                    });
            assertEquals(firstWidths[0], settledWidths[0], 1.0);
            assertEquals(firstWidths[1], settledWidths[1], 1.0);
            assertEquals(56.0 / 44.0, settledWidths[0] / settledWidths[1], .04);
            snapshot(entry, "01-public");
            login(entry, true, "admin01");
            await(() -> named("工作人员 · 李老师 · admin01") != null);
            Stage management = fx(() -> named("工作人员 · 李老师 · admin01"));
            await(
                    () ->
                            nodes(management.getScene().getRoot()).stream()
                                    .anyMatch(
                                            n ->
                                                    n instanceof TableView<?> t
                                                    && !t.getItems().isEmpty()));
            assertEquals(javafx.geometry.Side.TOP, fx(() -> nodes(management.getScene().getRoot()).stream().filter(n -> n instanceof TabPane).map(n -> (TabPane) n).findFirst().orElseThrow().getSide()));
            assertTrue(fx(() -> management.getScene().getWidth() > 1300), "页面切换应保留当前窗口宽度");
            resize(management, 1366, 768);
            assertViewportFilled(management);
            assertTableUsesAvailableSpace(management);
            resize(management, 700, 650);
            assertViewportFilled(management);
            assertTableUsesAvailableSpace(management);
            assertResponsiveTabs(management, "staff-navigation");
            resize(management, 1366, 768);
            snapshot(management, "02-staff-console");
            fx(
                    () -> {
                        TableView<?> t =
                                (TableView<?>) nodes(management.getScene().getRoot()).stream()
                                        .filter(n -> shown(n) && n instanceof TableView<?>)
                                        .findFirst().orElseThrow();
                        t.getSelectionModel().selectFirst();
                        button(management, "修改竞赛").fire();
                        return null;
                    });
            await(() -> entry.getTitle().equals("修改竞赛"));
            assertTrue(fx(() -> entry.getScene().getWidth() > 1300), "表单页不应把主窗口恢复成固定小尺寸");
            assertViewportFilled(entry);
            assertEquals(1, fx(() -> Window.getWindows().stream().filter(Window::isShowing).count()));
            fx(() -> { button(entry, "取消").fire(); return null; });
            await(() -> entry.getTitle().equals("工作人员 · 李老师 · admin01"));
            fx(
                    () -> {
                        TableView<?> t =
                                (TableView<?>)
                                        nodes(management.getScene().getRoot()).stream()
                                                .filter(n -> shown(n) && n instanceof TableView<?>)
                                                .findFirst()
                                                .orElseThrow();
                        t.getSelectionModel().selectFirst();
                        button(management, "进入工作区").fire();
                        return null;
                    });
            fx(
                    () -> {
                        for (Node n : nodes(management.getScene().getRoot()))
                            if (n instanceof TabPane tabs)
                                for (Tab t : tabs.getTabs())
                                    if (t.getText().contains("现场控制"))
                                        tabs.getSelectionModel().select(t);
                        return null;
                    });
            resize(management, 900, 700);
            SplitPane controlSplit =
                    fx(
                            () ->
                                    nodes(management.getScene().getRoot()).stream()
                                            .filter(
                                                    n ->
                                                            shown(n)
                                                                    && n instanceof SplitPane s
                                                                    && s.getStyleClass()
                                                                            .contains("control-split"))
                                            .map(n -> (SplitPane) n)
                                            .findFirst()
                                            .orElseThrow());
            assertEquals(Orientation.VERTICAL, fx(controlSplit::getOrientation));
            resize(management, 1366, 768);
            assertEquals(Orientation.HORIZONTAL, fx(controlSplit::getOrientation));
            service.startCompetition(staff, cid);
            service.startRound(staff, cid);
            fx(
                    () -> {
                        runtime.changed();
                        return null;
                    });
            assertEquals(1, fx(() -> Window.getWindows().stream().filter(Window::isShowing).count()));
            fx(
                    () -> {
                        button(entry, "退出登录").fire();
                        return null;
                    });
            await(() -> text(entry, "正在进行的航空知识竞赛"));
            Session staff2 = service.login(true, "admin01", "secret12");
            login(entry, false, "userA");
            resize(entry, 700, 650);
            assertResponsiveTabs(entry, "player-pages");
            fx(() -> { button(entry, "竞赛大厅").fire(); return null; });
            await(() -> entry.getScene().getWidth() < 750 && nodes(entry.getScene().getRoot()).stream().anyMatch(n -> shown(n) && n instanceof TableView<?> t && !t.getItems().isEmpty()));
            assertTrue(fx(() -> shown(button(entry, "正式报名"))));
            fx(() -> {
                TableView<?> t = (TableView<?>) nodes(entry.getScene().getRoot()).stream().filter(n -> shown(n) && n instanceof TableView<?>).findFirst().orElseThrow();
                t.getSelectionModel().selectFirst();
                button(entry, "进入比赛室").fire();
                return null;
            });
            assertTrue(errors.isEmpty(), errors.toString());
            snapshot(entry, "debug-room");
            String release = service.publish(staff2, cid);
            fx(
                    () -> {
                        runtime.changed();
                        return null;
                    });
            await(() -> text(entry, "飞机升力主要"));
            snapshot(entry, "03-player-wide");
            snapshot(entry, "04-player-narrow");
            fx(
                    () -> {
                        RadioButton radio =
                                (RadioButton)
                                        nodes(entry.getScene().getRoot()).stream()
                                                .filter(n -> n instanceof RadioButton)
                                                .findFirst()
                                                .orElseThrow();
                        radio.fire();
                        button(entry, "提交答案").fire();
                        return null;
                    });
            await(() -> text(entry, "回答正确"));
            assertEquals(10, service.ranking(staff2, cid).getFirst().score());
            assertEquals("A", service.room(a, cid).answer());
            snapshot(entry, "05-result");
            fx(() -> { button(entry, "退出登录").fire(); return null; });
            await(() -> text(entry, "正在进行的航空知识竞赛"));
            login(entry, false, "userB");
            fx(() -> { button(entry, "竞赛大厅").fire(); return null; });
            await(() -> nodes(entry.getScene().getRoot()).stream().anyMatch(n -> shown(n) && n instanceof TableView<?> t && !t.getItems().isEmpty()));
            fx(() -> {
                TableView<?> t = (TableView<?>) nodes(entry.getScene().getRoot()).stream().filter(n -> shown(n) && n instanceof TableView<?>).findFirst().orElseThrow();
                t.getSelectionModel().selectFirst();
                button(entry, "进入比赛室").fire();
                return null;
            });
            assertFalse(fx(() -> text(entry, "飞机升力主要")));
            assertEquals(1, fx(() -> Window.getWindows().stream().filter(Window::isShowing).count()));
            assertTrue(errors.isEmpty(), errors.toString());
            assertNotNull(release);
        } finally {
            fx(
                    () -> {
                        for (Window w : List.copyOf(Window.getWindows())) w.hide();
                        return null;
                    });
            runtime.close();
            Platform.exit();
        }
    }
}
