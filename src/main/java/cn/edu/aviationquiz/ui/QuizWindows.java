package cn.edu.aviationquiz.ui;

import cn.edu.aviationquiz.controller.AccountController;
import cn.edu.aviationquiz.controller.CompetitionLiveController;
import cn.edu.aviationquiz.controller.CompetitionManagementController;
import cn.edu.aviationquiz.controller.CompetitionRoomController;
import cn.edu.aviationquiz.controller.QuestionBankController;
import cn.edu.aviationquiz.controller.RegistrationController;
import cn.edu.aviationquiz.controller.ResultController;
import cn.edu.aviationquiz.entity.Models.*;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.util.Duration;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.*;

/** JavaFX screens. Business validation and SQL belong to the service/DAO layers. */
public final class QuizWindows {
    private final UiRuntime runtime;
    private final AccountController accountController;
    private final CompetitionLiveController competitionLiveController;
    private final CompetitionManagementController competitionManagementController;
    private final CompetitionRoomController competitionRoomController;
    private final QuestionBankController questionBankController;
    private final RegistrationController registrationController;
    private final ResultController resultController;
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private Stage entry;
    private Dashboard activeDashboard;
    private final List<Runnable> lobbyListeners = new ArrayList<>();
    private final Label global = new Label("欢迎浏览航空知识竞赛");

    public QuizWindows(UiRuntime runtime) {
        this.runtime = runtime;
        UiDependencies dependencies = runtime.dependencies();
        accountController = dependencies.accounts();
        competitionLiveController = dependencies.liveCompetition();
        competitionManagementController = dependencies.competitions();
        competitionRoomController = dependencies.competitionRoom();
        questionBankController = dependencies.questionBank();
        registrationController = dependencies.registrations();
        resultController = dependencies.results();
    }

    private String date(long value) {
        return dateFormat.format(Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()));
    }

    private long parseDate(String value) {
        return LocalDateTime.parse(value.trim(), dateFormat)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }

    private static Label label(String text) {
        Label l = new Label(text);
        l.setWrapText(true);
        return l;
    }

    private static Label title(String text) {
        Label l = label(text);
        l.getStyleClass().add("section-title");
        return l;
    }

    private static Button button(String text, Runnable action) {
        Button b = new Button(text);
        b.setOnAction(e -> action.run());
        return b;
    }

    private static HBox bar(Node... nodes) {
        HBox box = new HBox(10, nodes);
        box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return box;
    }

    private static VBox page(Node... nodes) {
        VBox box = new VBox(14, nodes);
        box.setPadding(new Insets(20));
        return box;
    }

    private static ImageView image(String resource, double width, double height) {
        ImageView view = new ImageView(new Image(Objects.requireNonNull(QuizWindows.class.getResource(resource)).toExternalForm(), true));
        view.setFitWidth(width);
        view.setFitHeight(height);
        view.setPreserveRatio(false);
        view.setSmooth(true);
        return view;
    }

    private static VBox brand() {
        Label mark = label("✈  航空知识竞赛管理系统");
        mark.getStyleClass().add("brand-title");
        Label sub = label("传播航空知识 · 点燃飞行梦想");
        sub.getStyleClass().add("brand-subtitle");
        return new VBox(2, mark, sub);
    }

    private static HBox header(Node... right) {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox box = new HBox(12, brand(), spacer);
        box.getChildren().addAll(right);
        box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        box.getStyleClass().add("aviation-header");
        return box;
    }

    private static Tab tab(String name, Node content) {
        Tab t = new Tab(name, content);
        t.setClosable(false);
        return t;
    }

    private static TabPane tabs(Tab... tabs) {
        return new TabPane(tabs);
    }

    private static ScrollPane scroll(Node node) {
        ScrollPane p = new ScrollPane(node);
        p.setFitToWidth(true);
        return p;
    }

    private void scene(Stage stage, String name, Parent root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets()
                .add(
                        Objects.requireNonNull(getClass().getResource("/css/app.css"))
                                .toExternalForm());
        stage.setTitle(name);
        stage.setMinWidth(650);
        stage.setMinHeight(540);
        stage.setScene(scene);
        if (Boolean.parseBoolean(System.getProperty("quiz.maximized", "true"))) {
            stage.setMaximized(true);
        }
    }

    private void error(Label target, Throwable error) {
        String message =
                error instanceof NumberFormatException
                        ? "请输入有效的整数"
                        : error instanceof java.time.format.DateTimeParseException
                                ? "日期时间格式应为 yyyy-MM-dd HH:mm:ss"
                                : error.getMessage();
        target.setText(message == null ? "操作失败，请重试" : message);
        target.getStyleClass().remove("success");
        if (!target.getStyleClass().contains("error-text"))
            target.getStyleClass().add("error-text");
    }

    private boolean confirm(Window owner, String text) {
        Alert alert =
                new Alert(Alert.AlertType.CONFIRMATION, text, ButtonType.OK, ButtonType.CANCEL);
        alert.initOwner(owner);
        alert.setHeaderText("请确认");
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private <T> void read(Callable<T> call, Consumer<T> success, Label status) {
        runtime.read(call, success, e -> error(status, e));
    }

    private void action(Label status, Runnable call) {
        runtime.mutate(
                () -> {
                    call.run();
                    return null;
                },
                v -> {
                    status.getStyleClass().remove("error-text");
                    status.setText("操作已完成");
                },
                e -> error(status, e));
    }

    private static <T> TableColumn<T, String> column(
            String heading, Function<T, String> value, double width) {
        TableColumn<T, String> column = new TableColumn<>(heading);
        column.setCellValueFactory(row -> new ReadOnlyStringWrapper(value.apply(row.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    @SafeVarargs
    private static <T> TableView<T> typedTable(TableColumn<T, String>... columns) {
        TableView<T> table = new TableView<>();
        table.setPlaceholder(label("暂无记录"));
        table.getColumns().addAll(columns);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);
        return table;
    }

    private static <T> void typedRows(
            TableView<T> table, List<T> values, Function<T, String> id) {
        T selected = table.getSelectionModel().getSelectedItem();
        String selectedId = selected == null ? "" : id.apply(selected);
        if (table.getItems().equals(values)) return;
        table.getItems().setAll(values);
        values.stream()
                .filter(value -> id.apply(value).equals(selectedId))
                .findFirst()
                .ifPresent(value -> table.getSelectionModel().select(value));
    }

    private <T> T selected(TableView<T> table) {
        T value = table.getSelectionModel().getSelectedItem();
        if (value == null) throw new IllegalArgumentException("请先选择一条记录");
        return value;
    }

    private void safe(Label status, Runnable call) {
        try {
            call.run();
        } catch (Exception e) {
            error(status, e);
        }
    }

    public void open(Stage stage) {
        entry = stage;
        if (activeDashboard != null) {
            activeDashboard.dispose();
            activeDashboard = null;
        }
        lobbyListeners.forEach(runtime::unlisten);
        lobbyListeners.clear();
        TableView<CompetitionView> all =
                typedTable(
                        column("竞赛", CompetitionView::name, 160),
                        column("分类", CompetitionView::categories, 160),
                        column("状态", CompetitionView::status, 160),
                        column("比赛时间", c -> date(c.competitionTime()), 160),
                        column("简介", CompetitionView::description, 160));
        ComboBox<String> filter =
                new ComboBox<>(
                        FXCollections.observableArrayList(
                                "全部", "未开放", "报名中", "报名截止", "比赛中", "已结束"));
        filter.setValue("全部");
        ComboBox<String> categoryFilter = new ComboBox<>(FXCollections.observableArrayList("全部分类", "民航史", "飞行原理", "航空法规"));
        categoryFilter.setValue("全部分类");
        Runnable refresh =
                () ->
                        read(
                                competitionManagementController::competitions,
                                list -> {
                                    typedRows(all, list.stream().filter(r -> (filter.getValue().equals("全部") || filter.getValue().equals(r.status()))
                                            && (categoryFilter.getValue().equals("全部分类") || r.categories().contains(categoryFilter.getValue()))).toList(), CompetitionView::id);
                                    if (all.getSelectionModel().getSelectedItem() == null && !all.getItems().isEmpty())
                                        all.getSelectionModel().selectFirst();
                                },
                                global);
        filter.setOnAction(e -> refresh.run());
        categoryFilter.setOnAction(e -> refresh.run());
        runtime.listen(refresh);
        lobbyListeners.add(refresh);
        runtime.onBackgroundError(e -> error(global, e));
        Button setup = button("初始化工作人员", () -> accountForm(true, true));
        Runnable setupRefresh =
                () ->
                        read(
                                accountController::needsStaffSetup,
                                needed -> {
                                    setup.setVisible(needed);
                                    setup.setManaged(needed);
                                },
                                global);
        runtime.listen(setupRefresh);
        lobbyListeners.add(setupRefresh);
        VBox detail = new VBox(12);
        detail.getStyleClass().add("card");
        Label detailTitle = title("选择一场竞赛");
        Label detailState = label("从左侧列表选择后查看报名时间与比赛说明");
        Label detailText = label("");
        detailText.getStyleClass().add("detail-copy");
        ImageView banner = image("/images/aviation-banner.png", 560, 0);
        banner.setPreserveRatio(true);
        banner.setManaged(false);
        banner.setVisible(false);
        StackPane bannerFrame = new StackPane(banner);
        bannerFrame.getStyleClass().add("competition-banner");
        banner.fitWidthProperty().bind(bannerFrame.widthProperty());
        detail.getChildren().addAll(detailTitle, detailState, bannerFrame, detailText);
        all.getSelectionModel().selectedItemProperty().addListener((o, old, c) -> {
            if (c == null) return;
            detailTitle.setText(c.name());
            detailState.setText("● " + c.status() + "   " + c.categories() + "   比赛时间  " + date(c.competitionTime()));
            detailText.setText(c.description() + "\n\n报名时间\n" + date(c.registerStart()) + " — " + date(c.registerEnd()) + "\n\n晋级名额  " + c.advanceCount() + " 人");
            banner.setManaged(true);
            banner.setVisible(true);
        });
        filter.setPrefWidth(120);
        Button refreshButton = button("刷新", refresh);
        FlowPane filters = new FlowPane(10, 10, label("竞赛状态"), filter, label("知识分类"), categoryFilter, refreshButton);
        filters.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        VBox listCard = new VBox(12, filters, all);
        listCard.setId("competition-list-card");
        listCard.getStyleClass().add("card");
        detail.setId("competition-detail-card");
        VBox.setVgrow(all, Priority.ALWAYS);
        GridPane wideContent = new GridPane();
        wideContent.setId("competition-wide-grid");
        wideContent.setHgap(18);
        ColumnConstraints listColumn = new ColumnConstraints();
        listColumn.setPercentWidth(56);
        listColumn.setHgrow(Priority.ALWAYS);
        ColumnConstraints detailColumn = new ColumnConstraints();
        detailColumn.setPercentWidth(44);
        detailColumn.setHgrow(Priority.ALWAYS);
        wideContent.getColumnConstraints().addAll(listColumn, detailColumn);
        wideContent.add(listCard, 0, 0);
        wideContent.add(detail, 1, 0);
        GridPane.setHgrow(listCard, Priority.ALWAYS);
        GridPane.setHgrow(detail, Priority.ALWAYS);
        GridPane.setVgrow(listCard, Priority.ALWAYS);
        GridPane.setVgrow(detail, Priority.ALWAYS);
        VBox compactContent = new VBox(18);
        ScrollPane compactScroll = scroll(compactContent);
        compactScroll.setFitToHeight(false);
        StackPane content = new StackPane(wideContent);
        content.getStyleClass().add("lobby-content");
        BorderPane root = new BorderPane();
        Button signup = button("选手注册", () -> showPlayerAuth(true, ""));
        Button login = button("选手登录", () -> showPlayerAuth(false, ""));
        login.getStyleClass().add("primary-button");
        root.setTop(header(label("知航空 · 爱祖国 · 向未来"), signup, login,
                button("工作人员登录", () -> accountForm(true, false)), setup));
        VBox center = page(title("正在进行的航空知识竞赛"), label("发现竞赛、查看赛程，登录后即可报名参赛"), content);
        center.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(content, Priority.ALWAYS);
        StackPane centered = new StackPane(center);
        centered.setAlignment(javafx.geometry.Pos.TOP_CENTER);
        root.setCenter(centered);
        root.setBottom(global);
        BorderPane.setMargin(global, new Insets(0, 20, 14, 20));
        scene(stage, "航空知识竞赛 · 公共入口", root, 1120, 760);
        stage.setMinWidth(650);
        final boolean[] compact = {false};
        stage.widthProperty().addListener((o, oldWidth, width) -> {
            boolean useCompact = width.doubleValue() < 980;
            if (useCompact == compact[0]) return;
            compact[0] = useCompact;
            if (useCompact) {
                wideContent.getChildren().clear();
                compactContent.getChildren().setAll(listCard, detail);
                listCard.setPrefHeight(360);
                content.getChildren().setAll(compactScroll);
            } else {
                compactContent.getChildren().clear();
                listCard.setPrefHeight(Region.USE_COMPUTED_SIZE);
                wideContent.add(listCard, 0, 0);
                wideContent.add(detail, 1, 0);
                GridPane.setHgrow(listCard, Priority.ALWAYS);
                GridPane.setHgrow(detail, Priority.ALWAYS);
                GridPane.setVgrow(listCard, Priority.ALWAYS);
                GridPane.setVgrow(detail, Priority.ALWAYS);
                content.getChildren().setAll(wideContent);
            }
        });
        stage.setOnCloseRequest(
                e -> {
                    e.consume();
                    if (confirm(stage, "退出整个程序？已发布题目的截止时间不会暂停。")) Platform.exit();
                });
        stage.show();
        refresh.run();
        setupRefresh.run();
    }

    private void showPlayerAuth(boolean create, String retainedUsername) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("auth-page");
        Button back = button("← 返回竞赛大厅", () -> open(entry));
        root.setTop(header(back));
        VBox card = new VBox(12);
        card.getStyleClass().addAll("card", "auth-card");
        Label heading = title(create ? "创建选手账号" : "选手登录");
        Label intro = label(create ? "加入航空知识竞赛，开启你的飞行探索" : "欢迎回来，继续你的航空知识之旅");
        TextField username = new TextField(retainedUsername);
        username.setPromptText("4～20 位账号");
        PasswordField password = new PasswordField();
        password.setPromptText("6～20 位密码");
        Label status = label("");
        card.getChildren().addAll(heading, intro, label("账号"), username, label("密码"), password);
        PasswordField confirmPassword = null;
        TextField school = null, college = null, major = null, studentNumber = null, name = null, phone = null;
        if (create) {
            confirmPassword = new PasswordField();
            confirmPassword.setPromptText("再次输入密码");
            school = new TextField(); school.setPromptText("请输入院校名称");
            college = new TextField(); college.setPromptText("请输入学院名称");
            major = new TextField(); major.setPromptText("请输入专业名称");
            studentNumber = new TextField(); studentNumber.setPromptText("4～30 位学号");
            name = new TextField(); name.setPromptText("真实姓名");
            phone = new TextField(); phone.setPromptText("11 位手机号");
            Label profileHeading = label("学籍及联系信息");
            profileHeading.getStyleClass().add("form-section-title");
            card.getChildren().addAll(label("确认密码"), confirmPassword, profileHeading,
                    label("院校 *"), school, label("学院 *"), college, label("专业 *"), major,
                    label("学号 *"), studentNumber, label("姓名 *"), name, label("手机号 *"), phone);
        }
        Button submit = button(create ? "立即注册" : "登录", () -> {});
        submit.getStyleClass().add("primary-button");
        PasswordField finalConfirmPassword = confirmPassword;
        TextField finalSchool = school, finalCollege = college, finalMajor = major,
                finalStudentNumber = studentNumber, finalName = name, finalPhone = phone;
        submit.setMaxWidth(Double.MAX_VALUE);
        submit.setOnAction(e -> {
            status.setText(create ? "正在创建账号…" : "正在登录…");
            submit.setDisable(true);
            try {
                String u = username.getText(), p = password.getText();
                runtime.mutate(() -> {
                    if (create) {
                        accountController.registerPlayer(
                                u,
                                p,
                                finalConfirmPassword.getText(),
                                new PlayerProfileInput(
                                        finalSchool.getText(),
                                        finalCollege.getText(),
                                        finalMajor.getText(),
                                        finalStudentNumber.getText(),
                                        finalName.getText(),
                                        finalPhone.getText()));
                        return null;
                    }
                    return accountController.login(false, u, p);
                }, value -> {
                    submit.setDisable(false);
                    if (create) showPlayerAuth(false, u);
                    else openPlayerAfterLogin((Session) value);
                }, ex -> { submit.setDisable(false); error(status, ex); });
            } catch (Exception ex) { submit.setDisable(false); error(status, ex); }
        });
        Button switchMode = button(create ? "已有账号？返回登录" : "还没有账号？立即注册", () -> showPlayerAuth(!create, username.getText()));
        switchMode.getStyleClass().add("link-button");
        card.getChildren().addAll(status, submit, switchMode);
        ImageView background = image("/images/auth-sky.png", 1120, 680);
        StackPane cardHolder = new StackPane(card);
        cardHolder.setPadding(new Insets(24));
        ScrollPane authScroll = scroll(cardHolder);
        authScroll.getStyleClass().add("auth-scroll");
        StackPane center = new StackPane(background, authScroll);
        card.prefWidthProperty().bind(center.widthProperty().multiply(.38));
        background.fitWidthProperty().bind(center.widthProperty());
        background.fitHeightProperty().bind(center.heightProperty());
        root.setCenter(center);
        scene(entry, create ? "航空知识竞赛 · 选手注册" : "航空知识竞赛 · 选手登录", root, 1120, 760);
    }

    private void openPlayerAfterLogin(Session session) {
        read(() -> accountController.profileComplete(session), complete -> {
            if (complete) {
                openSession(session);
            } else showProfileCompletion(session);
        }, global);
    }

    private void showProfileCompletion(Session session) {
        profileForm(entry, session, true, () -> {
            openSession(session);
        });
    }

    private void profileForm(Window owner, Session session, boolean required, Runnable after) {
        read(() -> accountController.playerProfile(session), profile -> {
            Form f = new Form(owner, required ? "完善个人资料" : "编辑个人资料");
            if (required) {
                f.body.getChildren().add(1, label("首次登录需要补全学籍与联系方式，保存后才能参赛。"));
                f.onCancel = () -> {
                    f.close();
                    runtime.read(() -> { accountController.logout(session); return null; }, v -> {}, ex -> {});
                };
            }
            TextField school = f.text("院校 *", profile.school());
            TextField college = f.text("学院 *", profile.college());
            TextField major = f.text("专业 *", profile.major());
            TextField studentNumber = f.text("学号 *", profile.studentNumber());
            TextField name = f.text("姓名 *", profile.name());
            TextField phone = f.text("手机号 *", profile.phone());
            f.save("保存资料", () -> {
                PlayerProfileInput input = new PlayerProfileInput(school.getText(), college.getText(), major.getText(),
                        studentNumber.getText(), name.getText(), phone.getText());
                return (Callable<Object>) () -> { accountController.updatePlayerProfile(session, input); return null; };
            }, after);
        }, global);
    }

    private void details(CompetitionView c) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.initOwner(entry);
        a.setTitle("竞赛详情");
        a.setHeaderText(c.name());
        a.setContentText(
                c.description()
                        + "\n状态："
                        + c.status()
                        + "\n分类："
                        + c.categories()
                        + "\n报名开始："
                        + date(c.registerStart())
                        + "\n报名截止："
                        + date(c.registerEnd())
                        + "\n比赛时间："
                        + date(c.competitionTime())
                        + "\n晋级名额："
                        + c.advanceCount());
        a.show();
    }

    private final class Form {
        final Stage stage;
        final Scene previousScene;
        final String previousTitle;
        final double previousMinWidth;
        final double previousMinHeight;
        final GridPane grid = new GridPane();
        final Label status = label("");
        final VBox body;
        final Map<String, Control> fields = new LinkedHashMap<>();
        final Map<String, Label> fieldErrors = new LinkedHashMap<>();
        Runnable onCancel;
        int row;

        Form(Window owner, String title) {
            stage = (Stage) owner;
            previousScene = stage.getScene();
            previousTitle = stage.getTitle();
            previousMinWidth = stage.getMinWidth();
            previousMinHeight = stage.getMinHeight();
            onCancel = this::close;
            grid.setHgap(16);
            grid.setVgap(10);
            body = page(QuizWindows.title(title), grid, status);
            body.getStyleClass().add("form-page");
            StackPane holder = new StackPane(body);
            holder.setPadding(new Insets(32));
            holder.getStyleClass().add("form-holder");
            scene(stage, title, scroll(holder), 620, 560);
        }

        void close() {
            stage.setScene(previousScene);
            stage.setTitle(previousTitle);
            stage.setMinWidth(previousMinWidth);
            stage.setMinHeight(previousMinHeight);
        }

        <T extends Control> T field(String name, T control) {
            Label l = label(name);
            l.setLabelFor(control);
            grid.addRow(row++, l, control);
            Label fieldError = label("");
            fieldError.getStyleClass().add("error-text");
            fieldError.setVisible(false);
            fieldError.setManaged(false);
            grid.add(fieldError, 1, row++);
            fields.put(name, control);
            fieldErrors.put(name, fieldError);
            control.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(control, Priority.ALWAYS);
            return control;
        }

        TextField text(String name, String initial) {
            return field(name, new TextField(initial));
        }

        ComboBox<String> choice(String name, List<String> values, String initial) {
            ComboBox<String> c =
                    field(name, new ComboBox<>(FXCollections.observableArrayList(values)));
            c.setValue(initial);
            return c;
        }

        void showError(Throwable ex) {
            error(status, ex);
            for (var field : fields.entrySet()) {
                String key = field.getKey().split("[（（ ]")[0];
                if (status.getText().contains(key)) {
                    Label message = fieldErrors.get(field.getKey());
                    message.setText(status.getText());
                    message.setManaged(true);
                    message.setVisible(true);
                    field.getValue().requestFocus();
                    break;
                }
            }
        }

        void save(String text, Callable<? extends Callable<?>> operation, Runnable after) {
            Button b = button(text, () -> {});
            b.getStyleClass().add("primary-button");
            b.setOnAction(
                    e -> {
                        b.setDisable(true);
                        fieldErrors
                                .values()
                                .forEach(
                                        l -> {
                                            l.setText("");
                                            l.setManaged(false);
                                            l.setVisible(false);
                                        });
                        status.setText("正在保存…");
                        // Capture and validate JavaFX inputs on the FX thread before queuing work.
                        try {
                            Callable<?> work = operation.call();
                            runtime.mutate(
                                    work,
                                    v -> {
                                        close();
                                        after.run();
                                    },
                                    ex -> {
                                        b.setDisable(false);
                                        showError(ex);
                                    });
                        } catch (Exception ex) {
                            b.setDisable(false);
                            showError(ex);
                        }
                    });
            body.getChildren().add(bar(b, button("取消", () -> onCancel.run())));
        }
    }

    private void accountForm(boolean staff, boolean create) {
        if (create && !staff) {
            showPlayerAuth(true, "");
            return;
        }
        Form f =
                new Form(
                        entry,
                        create ? (staff ? "首次创建工作人员" : "选手注册") : (staff ? "工作人员登录" : "选手登录"));
        TextField username = f.text("账号（4～20 位）", "");
        PasswordField password = f.field("密码（6～20 位）", new PasswordField());
        TextField name = create ? f.text("姓名（2～20 字）", "") : null;
        TextField phone = create && !staff ? f.text("电话（选填）", "") : null;
        Session[] loggedIn = {null};
        f.save(
                create ? "创建账号" : "登录",
                () -> {
                    String u = username.getText(),
                            p = password.getText(),
                            n = name == null ? "" : name.getText(),
                            ph = phone == null ? "" : phone.getText();
                    return (Callable<Object>)
                            () -> {
                                if (create) {
                                    if (staff) accountController.setupStaff(u, p, n);
                                    else throw new IllegalStateException("请使用选手注册页面");
                                    return null;
                                }
                                loggedIn[0] = accountController.login(staff, u, p);
                                return null;
                            };
                },
                () -> {
                    if (loggedIn[0] != null) openSession(loggedIn[0]);
                });
    }

    private void openSession(Session session) {
        lobbyListeners.forEach(runtime::unlisten);
        lobbyListeners.clear();
        if (activeDashboard != null) activeDashboard.dispose();
        Stage stage = entry;
        Dashboard dashboard = new Dashboard(stage, session);
        activeDashboard = dashboard;
        scene(
                stage,
                (session.staff() ? "工作人员" : "选手")
                        + " · "
                        + session.name()
                        + " · "
                        + session.username(),
                dashboard.root,
                session.staff() ? 1200 : 950,
                780);
        if (session.staff()) stage.setMinWidth(1050);
        dashboard.refresh();
    }

    private void logoutToLobby(Dashboard dashboard) {
        if (dashboard != activeDashboard) return;
        dashboard.dispose();
        activeDashboard = null;
        runtime.read(
                () -> {
                    accountController.logout(dashboard.session);
                    return null;
                },
                v -> open(entry),
                ex -> {
                    error(global, ex);
                    open(entry);
                });
    }

    private final class Dashboard {
        final Stage stage;
        final Session session;
        final Label status = label("请选择竞赛");
        final VBox root;
        final List<Runnable> refreshers = new ArrayList<>();
        final Runnable listener = this::refresh;
        final TabPane navigation = new TabPane();
        boolean disposed;

        Dashboard(Stage stage, Session session) {
            this.stage = stage;
            this.session = session;
            if (session.staff()) {
                navigation.getTabs().add(tab("竞赛浏览", competitionPage(false)));
                navigation.getTabs().add(tab("题库管理", questionPage()));
                navigation.getTabs().add(tab("历史成绩", historyPage()));
                navigation.setSide(Side.TOP);
                navigation.getStyleClass().add("staff-navigation");
                VBox.setVgrow(navigation, Priority.ALWAYS);
                Label eyebrow = label("航空知识竞赛 · 赛事运营中心"); eyebrow.getStyleClass().add("staff-eyebrow");
                VBox identity = new VBox(2, title("竞赛组织工作台"), eyebrow);
                Label account = label(session.name() + " · " + session.username()); account.getStyleClass().add("staff-account");
                Button logout = button("退出登录", () -> logoutToLobby(this)); logout.getStyleClass().add("quiet-button");
                Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
                HBox top = new HBox(18, identity, spacer, account, logout); top.setAlignment(javafx.geometry.Pos.CENTER_LEFT); top.getStyleClass().add("staff-topbar");
                root = new VBox(top, navigation, status); root.getStyleClass().add("staff-shell");
            } else {
                navigation.getStyleClass().add("player-pages");
                navigation.getTabs().addAll(
                        tab("首页", homePage()),
                        tab("竞赛大厅", competitionPage(false)),
                        tab("我的竞赛", competitionPage(true)),
                        tab("历史成绩", historyPage()),
                        tab("个人资料", profilePage()));
                Button[] nav = {
                    navButton("首页", 0), navButton("竞赛大厅", 1),
                    navButton("我的竞赛", 2), navButton("历史成绩", 3), navButton("个人资料", 4)
                };
                HBox top = header(nav[0], nav[1], nav[2], nav[3], nav[4], label(session.name()), button("退出登录", () -> logoutToLobby(this)));
                Node playerBrand = top.getChildren().getFirst();
                stage.widthProperty().addListener((o, old, width) -> {
                    boolean compact = width.doubleValue() < 820;
                    playerBrand.setManaged(!compact);
                    playerBrand.setVisible(!compact);
                });
                navigation.getSelectionModel().selectedIndexProperty().addListener((o, old, value) -> {
                    for (int i = 0; i < nav.length; i++) {
                        nav[i].getStyleClass().remove("nav-active");
                        if (i == value.intValue()) nav[i].getStyleClass().add("nav-active");
                    }
                });
                nav[0].getStyleClass().add("nav-active");
                VBox.setVgrow(navigation, Priority.ALWAYS);
                root = new VBox(top, navigation, status);
                root.getStyleClass().add("player-shell");
            }
            runtime.listen(listener);
        }

        Button navButton(String text, int index) {
            Button button = button(text, () -> navigation.getSelectionModel().select(index));
            button.getStyleClass().add("nav-button");
            return button;
        }

        Node homePage() {
            Label registered = title("—"), reserved = title("—"), completed = title("—");
            VBox registeredCard = statCard("已报名竞赛", registered, "查看我的竞赛");
            VBox reservedCard = statCard("已预约竞赛", reserved, "报名开放后记得确认");
            VBox completedCard = statCard("已完成竞赛", completed, "回顾成绩与排名");
            HBox stats = new HBox(16, registeredCard, reservedCard, completedCard);
            stats.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
            Label next = label("正在加载最近赛程…");
            VBox nextCard = new VBox(12, title("最近可参与竞赛"), image("/images/aviation-banner.png", 760, 245), next,
                    button("前往竞赛大厅", () -> navigation.getSelectionModel().select(1)));
            nextCard.getStyleClass().add("card");
            Runnable refresh = () -> read(() -> competitionManagementController.competitionsForPlayer(session), list -> {
                long reg = list.stream().filter(r -> Set.of("已报名", "已分组").contains(r.myStatus())).count();
                long res = list.stream().filter(r -> r.myStatus().equals("已预约")).count();
                long done = list.stream().filter(r -> r.status().equals("已结束") && Set.of("已报名", "已分组").contains(r.myStatus())).count();
                registered.setText(String.valueOf(reg)); reserved.setText(String.valueOf(res)); completed.setText(String.valueOf(done));
                list.stream().filter(r -> !r.status().equals("已结束")).min(Comparator.comparingLong(CompetitionView::competitionTime))
                        .ifPresentOrElse(r -> next.setText(r.name() + "\n" + r.status() + " · " + date(r.competitionTime())), () -> next.setText("当前没有待参与的竞赛"));
            }, status);
            refreshers.add(refresh);
            VBox content = page(title("你好，" + session.name()), label("欢迎回到航空知识竞赛中心，今天也向蓝天更近一步。"), stats, nextCard);
            content.getStyleClass().add("dashboard-home");
            return scroll(content);
        }

        VBox statCard(String caption, Label value, String note) {
            VBox card = new VBox(7, label(caption), value, label(note));
            card.getStyleClass().addAll("card", "stat-card");
            card.setMaxWidth(Double.MAX_VALUE);
            return card;
        }

        Node profilePage() {
            GridPane fields = new GridPane();
            fields.setHgap(24);
            fields.setVgap(16);
            String[] labels = {"账号", "姓名", "院校", "学院", "专业", "学号", "手机号"};
            List<Label> values = new ArrayList<>();
            for (int i = 0; i < labels.length; i++) {
                Label value = label("—");
                value.getStyleClass().add("profile-value");
                fields.add(label(labels[i]), 0, i);
                fields.add(value, 1, i);
                values.add(value);
            }
            Runnable refresh = () -> read(() -> accountController.playerProfile(session), profile -> {
                List<String> profileFields =
                        List.of(
                                profile.username(),
                                profile.name(),
                                profile.school(),
                                profile.college(),
                                profile.major(),
                                profile.studentNumber(),
                                profile.phone());
                for (int i = 0; i < profileFields.size(); i++)
                    values.get(i).setText(profileFields.get(i));
            }, status);
            refreshers.add(refresh);
            VBox card = new VBox(20, title("个人资料"), label("维护参赛身份和联系信息"), fields,
                    button("编辑资料", () -> profileForm(stage, session, false, () -> { refresh.run(); refresh(); })));
            card.getStyleClass().add("card");
            return page(card);
        }

        void refresh() {
            if (!disposed) List.copyOf(refreshers).forEach(Runnable::run);
        }

        void dispose() {
            disposed = true;
            runtime.unlisten(listener);
            roomTimers.forEach(Animation::stop);
        }

        final List<Timeline> roomTimers = new ArrayList<>();

        Node competitionPage(boolean mine) {
            TableView<CompetitionView> table =
                    mine
                            ? typedTable(
                                    column("竞赛", CompetitionView::name, 160),
                                    column("分类", CompetitionView::categories, 160),
                                    column("报名情况", CompetitionView::participation, 160),
                                    column("分组", CompetitionView::groupName, 160),
                                    column("状态", CompetitionView::status, 160),
                                    column("比赛时间", c -> date(c.competitionTime()), 160))
                            : session.staff()
                                    ? typedTable(
                                            column("竞赛", CompetitionView::name, 160),
                                            column("分类", CompetitionView::categories, 160),
                                            column("状态", CompetitionView::status, 160),
                                            column("比赛时间", c -> date(c.competitionTime()), 160),
                                            column("简介", CompetitionView::description, 160))
                                    : typedTable(
                                            column("竞赛", CompetitionView::name, 160),
                                            column("分类", CompetitionView::categories, 160),
                                            column("状态", CompetitionView::status, 160),
                                            column("我的状态", CompetitionView::myStatus, 160),
                                            column("比赛时间", c -> date(c.competitionTime()), 160),
                                            column("简介", CompetitionView::description, 160));
            ComboBox<String> categoryFilter = new ComboBox<>(FXCollections.observableArrayList("全部分类", "民航史", "飞行原理", "航空法规"));
            categoryFilter.setValue("全部分类");
            Label total = label("—"), open = label("—"), running = label("—");
            Runnable refresh =
                    () ->
                            read(
                                    () ->
                                            mine
                                                    ? competitionManagementController
                                                            .participatedCompetitions(session)
                                                    : session.staff()
                                                            ? competitionManagementController
                                                                    .competitions()
                                                            : competitionManagementController
                                                                    .competitionsForPlayer(session),
                                    v -> { if (!disposed) { total.setText(String.valueOf(v.size())); open.setText(String.valueOf(v.stream().filter(r -> Set.of("未开放", "报名中", "报名截止").contains(r.status())).count())); running.setText(String.valueOf(v.stream().filter(r -> r.status().equals("比赛中")).count())); typedRows(table, v.stream().filter(r -> categoryFilter.getValue().equals("全部分类") || r.categories().contains(categoryFilter.getValue())).toList(), CompetitionView::id); } },
                                    status);
            categoryFilter.setOnAction(e -> refresh.run());
            refreshers.add(refresh);
            HBox actions =
                    bar(
                            label("分类"),
                            categoryFilter,
                            button("刷新", refresh),
                            button("详情", () -> safe(status, () -> details(selected(table)))));
            if (session.staff())
                actions.getChildren()
                        .addAll(
                                button("新建竞赛", () -> competitionForm(null)),
                                button(
                                        "修改竞赛",
                                        () -> safe(status, () -> competitionForm(selected(table)))),
                                button(
                                        "进入工作区",
                                        () -> safe(status, () -> workspace(selected(table)))));
            if (session.staff()) {
                actions.getStyleClass().add("workspace-toolbar"); actions.getChildren().get(4).getStyleClass().add("primary-button");
                HBox summary = new HBox(14, staffStat("全部竞赛", total, "当前已维护的赛事"), staffStat("待开赛", open, "报名及准备阶段"), staffStat("进行中", running, "正在进行的赛场"));
                summary.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
                VBox content = page(summary, title("竞赛中心"), label("集中维护赛程、报名状态与比赛工作区"), actions, table); content.getStyleClass().add("staff-page"); return content;
            }

            Label operationHint = label("请先选择一场竞赛");
            Button
                    reserve =
                            button(
                                    "预约",
                                    () ->
                                            safe(
                                                    status,
                                                    () -> {
                                                        String cid = selected(table).id();
                                                        action(
                                                                status,
                                                                () ->
                                                                        registrationController.join(
                                                                                session, cid,
                                                                                true));
                                                    })),
                    register =
                            button(
                                    "正式报名",
                                    () ->
                                            safe(
                                                    status,
                                                    () -> {
                                                        String cid = selected(table).id();
                                                        action(
                                                                status,
                                                                () ->
                                                                        registrationController.join(
                                                                                session, cid,
                                                                                false));
                                                    })),
                    cancelReserve =
                            button(
                                    "取消预约",
                                    () -> safe(status, () -> {
                                        String cid = selected(table).id();
                                        action(status, () -> registrationController.cancel(session, cid, true));
                                    })),
                    cancelRegister =
                            button(
                                    "取消报名",
                                    () -> safe(status, () -> {
                                        String cid = selected(table).id();
                                        action(status, () -> registrationController.cancel(session, cid, false));
                                    })),
                    enter = button("进入比赛室", () -> safe(status, () -> room(selected(table))));
            register.getStyleClass().add("primary-button");
            FlowPane participationActions = new FlowPane(10, 10, reserve, cancelReserve, register, cancelRegister, enter);
            participationActions.getStyleClass().add("participation-actions");

            Runnable updateActions =
                    () -> {
                        CompetitionView selected = table.getSelectionModel().getSelectedItem();
                        if (selected == null) {
                            reserve.setDisable(true);
                            cancelReserve.setDisable(true);
                            register.setDisable(true);
                            cancelRegister.setDisable(true);
                            enter.setDisable(true);
                            operationHint.setText("请先选择一场竞赛");
                            return;
                        }
                        String state = selected.status();
                        String participation = selected.myStatus();
                        long now = System.currentTimeMillis();
                        boolean reserved = participation.equals("已预约");
                        boolean cancelledReservation = participation.equals("已取消预约");
                        boolean cancelledRegistration = participation.equals("已取消报名");
                        boolean registered =
                                participation.equals("已报名") || participation.equals("已分组");
                        boolean canReserve = state.equals("未开放") && !reserved && !registered && !cancelledRegistration;
                        boolean inRegistrationTime =
                                now >= selected.registerStart()
                                        && now < selected.registerEnd();
                        boolean canRegister =
                                state.equals("报名中") && inRegistrationTime && !registered;
                        reserve.setDisable(!canReserve);
                        cancelReserve.setDisable(!(state.equals("未开放") && reserved));
                        register.setDisable(!canRegister);
                        cancelRegister.setDisable(!(state.equals("报名中") && inRegistrationTime
                                && participation.equals("已报名")));
                        enter.setDisable(!registered);
                        if (registered)
                            operationHint.setText(
                                    participation.equals("已分组")
                                            ? "您已报名并完成分组，可以进入比赛室"
                                            : "您已报名，无需重复提交；比赛开始后可进入比赛室");
                        else if (state.equals("未开放"))
                            operationHint.setText(reserved ? "您已经预约，可在开放报名前取消" : cancelledReservation
                                    ? "预约已取消，可重新预约" : "该竞赛尚未开放，可先预约");
                        else if (state.equals("报名中") && inRegistrationTime)
                            operationHint.setText(cancelledRegistration ? "报名已取消，可在截止前重新报名"
                                    : "该竞赛正在报名，请点击“正式报名”");
                        else if (state.equals("报名中")) operationHint.setText("竞赛状态为报名中，但当前不在报名时间范围");
                        else if (state.equals("报名截止")) operationHint.setText("报名已经截止，不能新增报名");
                        else if (state.equals("比赛中")) operationHint.setText("比赛已经开始，不能新增报名");
                        else operationHint.setText("竞赛已经结束，只能查询历史结果");
                    };
            table.getSelectionModel()
                    .selectedItemProperty()
                    .addListener((observable, oldValue, newValue) -> updateActions.run());
            updateActions.run();
            return page(actions, label("所选竞赛操作"), participationActions, operationHint, table);
        }

        VBox staffStat(String caption, Label value, String note) {
            value.getStyleClass().add("staff-stat-value"); VBox card = new VBox(5, label(caption), value, label(note)); card.setMaxWidth(Double.MAX_VALUE); card.getStyleClass().add("staff-stat-card"); return card;
        }


        void competitionForm(CompetitionView c) {
            Form f = new Form(stage, c == null ? "新建竞赛" : "修改竞赛");
            long base = System.currentTimeMillis();
            TextField name = f.text("名称", c == null ? "" : c.name());
            TextArea desc =
                    f.field("简介（最多 200 字）", new TextArea(c == null ? "" : c.description()));
            desc.setPrefRowCount(2);
            CheckBox history = new CheckBox("民航史"), principles = new CheckBox("飞行原理"), law = new CheckBox("航空法规");
            String existingCategories = c == null ? "民航史/飞行原理/航空法规" : c.categories();
            history.setSelected(existingCategories.contains("民航史"));
            principles.setSelected(existingCategories.contains("飞行原理"));
            law.setSelected(existingCategories.contains("航空法规"));
            f.field("竞赛分类（可多选）", history);
            f.field("", principles);
            f.field("", law);
            TextField start = f.text("报名开始", date(c == null ? base : c.registerStart())),
                    end =
                            f.text(
                                    "报名截止",
                                    date(c == null ? base + 3600000 : c.registerEnd())),
                    time =
                            f.text(
                                    "比赛时间",
                                    date(
                                            c == null
                                                    ? base + 7200000
                                                    : c.competitionTime())),
                    quota = f.text("晋级名额", c == null ? "1" : String.valueOf(c.advanceCount()));
            f.body.getChildren().add(label("时间格式：yyyy-MM-dd HH:mm:ss"));
            f.save(
                    "保存",
                    () -> {
                        CompetitionInput input =
                                new CompetitionInput(
                                        name.getText(),
                                        desc.getText(),
                                        parseDate(start.getText()),
                                        parseDate(end.getText()),
                                        parseDate(time.getText()),
                                        Integer.parseInt(quota.getText()),
                                        java.util.stream.Stream.of(history, principles, law)
                                                .filter(CheckBox::isSelected).map(CheckBox::getText)
                                                .collect(java.util.stream.Collectors.toUnmodifiableSet()));
                        return (Callable<String>)
                                () ->
                                        competitionManagementController.save(
                                                session, c == null ? null : c.id(), input);
                    },
                    () -> {});
        }

        Node questionPage() {
            TableView<QuestionView> table =
                    typedTable(
                            column("分类", QuestionView::category, 160),
                            column("题干", QuestionView::content, 380),
                            column("标准答案", QuestionView::answer, 160),
                            column("启用（1/0）", q -> q.active() ? "1" : "0", 160));
            TextField search = new TextField();
            search.setPromptText("按题干或分类查询");
            Runnable refresh =
                    () -> {
                        String query = search.getText().trim();
                        read(
                                () -> questionBankController.questions(session),
                                list ->
                                        typedRows(
                                                table,
                                                list.stream()
                                                        .filter(
                                                                q ->
                                                                        q.content()
                                                                                        .contains(
                                                                                                query)
                                                                                || q.category()
                                                                                        .contains(
                                                                                                query))
                                                        .toList(),
                                                QuestionView::id),
                                status);
                    };
            refreshers.add(refresh);
            search.setOnAction(e -> refresh.run());
            return page(
                    bar(search, button("查询", refresh)),
                    bar(
                            button("新增", () -> questionForm(null, false)),
                            button(
                                    "编辑",
                                    () -> safe(status, () -> questionForm(selected(table), false))),
                            button(
                                    "复制为新题",
                                    () -> safe(status, () -> questionForm(selected(table), true))),
                            button(
                                    "启用 / 停用",
                                    () ->
                                            safe(
                                                    status,
                                                    () -> {
                                                        QuestionView q = selected(table);
                                                        action(
                                                                status,
                                                                () ->
                                                                        questionBankController.setQuestionActive(
                                                                                session,
                                                                                q.id(),
                                                                                !q.active()));
                                                    })),
                            button(
                                    "删除",
                                    () ->
                                            safe(
                                                    status,
                                                    () -> {
                                                        QuestionView q = selected(table);
                                                        if (confirm(stage, "删除所选未引用题目？"))
                                                            action(
                                                                    status,
                                                                    () ->
                                                                            questionBankController.deleteQuestion(
                                                                                    session,
                                                                                    q.id()));
                                                    }))),
                    table);
        }

        void questionForm(QuestionView q, boolean copy) {
            Form f = new Form(stage, q == null ? "新增题目" : copy ? "复制为新题" : "编辑题目");
            TextArea content = f.field("题干", new TextArea(q == null ? "" : q.content()));
            content.setPrefRowCount(3);
            ComboBox<String> category =
                    f.choice(
                            "分类",
                            List.of("民航史", "飞行原理", "航空法规"),
                            q == null ? "民航史" : q.category());
            List<TextField> options = new ArrayList<>();
            for (int i = 0; i < 4; i++)
                options.add(
                        f.text(
                                "选项 " + (char) ('A' + i),
                                q == null ? "" : q.options().get(i)));
            ComboBox<String> answer =
                    f.choice(
                            "标准答案",
                            List.of("A", "B", "C", "D"),
                            q == null ? "A" : q.answer());
            f.save(
                    "保存",
                    () -> {
                        QuestionInput input =
                                new QuestionInput(
                                        content.getText(),
                                        category.getValue(),
                                        options.stream().map(TextField::getText).toList(),
                                        answer.getValue(),
                                        q == null || copy || q.active());
                        return (Callable<String>)
                                () ->
                                        questionBankController.saveQuestion(
                                                session,
                                                q == null || copy ? null : q.id(),
                                                input);
                    },
                    () -> {});
        }

        Node historyPage() {
            TableView<HistoryView> t =
                    session.staff()
                            ? typedTable(
                                    column("竞赛", HistoryView::competitionName, 200),
                                    column("比赛时间", h -> date(h.competitionTime()), 180))
                            : typedTable(
                                    column("竞赛", HistoryView::competitionName, 200),
                                    column("比赛时间", h -> date(h.competitionTime()), 180),
                                    column("最终成绩", h -> String.valueOf(h.finalScore()), 120),
                                    column("名次", h -> String.valueOf(h.rank()), 100),
                                    column("晋级", HistoryView::promotion, 120));
            Runnable refresh =
                    () ->
                            read(
                                    () -> resultController.history(session),
                                    v -> typedRows(t, v, HistoryView::competitionId),
                                    status);
            refreshers.add(refresh);
            Button view =
                    button(
                            "查看全场结果",
                            () ->
                                    safe(
                                            status,
                                            () ->
                                                    rankingTab(
                                                            selected(t).competitionId(),
                                                            selected(t).competitionName())));
            Node exportAction =
                    session.staff()
                            ? button(
                                    "导出 CSV",
                                    () -> safe(status, () -> export(selected(t).competitionId())))
                            : label("历史成绩以最终归档结果为准");
            return page(bar(button("刷新", refresh), view), exportAction, t);
        }

        void export(String cid) {
            FileChooser chooser = new FileChooser();
            chooser.setInitialFileName("航空竞赛成绩.csv");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
            java.io.File file = chooser.showSaveDialog(stage);
            if (file == null) return;
            action(
                    status,
                    () -> {
                        String csv = resultController.exportCsv(session, cid);
                        try {
                            Files.writeString(file.toPath(), csv, StandardCharsets.UTF_8);
                        } catch (Exception e) {
                            throw new IllegalStateException("导出失败：" + e.getMessage(), e);
                        }
                    });
        }

        void openTab(String key, String name, Node node) {
            for (Tab t : navigation.getTabs())
                if (key.equals(t.getId())) {
                    navigation.getSelectionModel().select(t);
                    return;
                }
            Tab t = tab(name, node);
            t.setId(key);
            navigation.getTabs().add(t);
            navigation.getSelectionModel().select(t);
        }

        boolean existing(String key) {
            for (Tab t : navigation.getTabs())
                if (key.equals(t.getId())) {
                    navigation.getSelectionModel().select(t);
                    return true;
                }
            return false;
        }

        void rankingTab(String cid, String name) {
            if (existing("rank" + cid)) return;
            TableView<RankingEntry> t = rankingTable();
            Runnable refresh =
                    () -> read(() -> resultController.ranking(session, cid), v -> rankRows(t, v), status);
            refreshers.add(refresh);
            openTab("rank" + cid, name + " · 排名", page(label("排序：总分 → 答对数 → 用时 → 编号"), t));
            refresh.run();
        }

        TableView<RankingEntry> rankingTable() {
            TableView<RankingEntry> t = new TableView<>();
            String[] names = {"名次", "姓名", "小组", "总分", "答对数", "用时/秒", "晋级"};
            for (int i = 0; i < names.length; i++) {
                final int field = i;
                TableColumn<RankingEntry, String> c = new TableColumn<>(names[i]);
                c.setCellValueFactory(
                        v -> {
                            RankingEntry r = v.getValue();
                            return new ReadOnlyStringWrapper(
                                    switch (field) {
                                        case 0 -> "" + r.rank();
                                        case 1 -> r.name();
                                        case 2 -> r.group();
                                        case 3 -> "" + r.score();
                                        case 4 -> "" + r.correctCount();
                                        case 5 ->
                                                String.format(
                                                        Locale.ROOT,
                                                        "%.3f",
                                                        r.elapsedMillis() / 1000.0);
                                        default -> r.promotion();
                                    });
                        });
                c.setPrefWidth(100);
                t.getColumns().add(c);
            }
            t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            t.setRowFactory(
                    tv ->
                            new TableRow<>() {
                                @Override
                                protected void updateItem(RankingEntry r, boolean empty) {
                                    super.updateItem(r, empty);
                                    pseudoClassStateChanged(
                                            javafx.css.PseudoClass.getPseudoClass("self"),
                                            !empty
                                                    && r != null
                                                    && r.playerId().equals(session.id()));
                                    pseudoClassStateChanged(
                                            javafx.css.PseudoClass.getPseudoClass("rank-top"),
                                            !empty && r != null && r.rank() <= 3
                                                    && !r.playerId().equals(session.id()));
                                }
                            });
            t.setPlaceholder(label("暂无参赛选手"));
            VBox.setVgrow(t, Priority.ALWAYS);
            return t;
        }

        void rankRows(TableView<RankingEntry> t, List<RankingEntry> list) {
            if (t.getItems().equals(list)) return;
            if (t.getItems().size() == list.size()) {
                for (int i = 0; i < list.size(); i++) t.getItems().set(i, list.get(i));
            } else t.getItems().setAll(list);
        }

        void workspace(CompetitionView competition) {
            String cid = competition.id();
            if (existing("work" + cid)) return;
            Label heading = title(competition.name()),
                    state = label(competition.status());
            Runnable update =
                    () ->
                            read(
                                    competitionManagementController::competitions,
                                    list ->
                                            list.stream()
                                                    .filter(c -> c.id().equals(cid))
                                                    .findFirst()
                                                    .ifPresent(
                                                            c -> {
                                                                heading.setText(c.name());
                                                                state.setText(c.status());
                                                            }),
                                    status);
            refreshers.add(update);
            state.getStyleClass().add("competition-state");
            Node setup =
                    page(
                            label("报名按设定时间开放和截止。报名截止后方可分组。"),
                            bar(
                                    button(
                                            "开放报名",
                                            () ->
                                                    action(
                                                            status,
                                                            () ->
                                                                    competitionManagementController.changeRegistrationState(
                                                                            session, cid, "报名中"))),
                                    button(
                                            "截止报名",
                                            () ->
                                                    action(
                                                            status,
                                                            () ->
                                                                    competitionManagementController.changeRegistrationState(
                                                                            session, cid,
                                                                            "报名截止")))),
                            peoplePage(cid, true));
            Button back = button("返回竞赛中心", () -> navigation.getSelectionModel().select(0)); back.getStyleClass().add("back-button");
            VBox context = new VBox(4, heading, label("当前赛事工作区 · 按流程完成报名、编排、控制与归档")); Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox workspaceHeader = new HBox(16, back, context, spacer, state); workspaceHeader.setAlignment(javafx.geometry.Pos.CENTER_LEFT); workspaceHeader.getStyleClass().add("workspace-header");
            TabPane workTabs = tabs(tab("01  报名状态", setup), tab("02  分组编排", peoplePage(cid, false)), tab("03  轮次题单", roundPage(cid)), tab("04  现场控制", controlPage(cid))); workTabs.getStyleClass().add("workspace-tabs");
            VBox content = page(workspaceHeader, workTabs); content.getStyleClass().addAll("staff-page", "workspace-page");
            openTab("work" + cid, competition.name() + " · 工作区", content);
            refresh();
        }

        Node peoplePage(String cid, boolean reserved) {
            TableView<ParticipantView> people =
                    typedTable(
                            column("姓名", ParticipantView::name, 160),
                            column("院校", ParticipantView::school, 160),
                            column("学院", ParticipantView::college, 160),
                            column("专业", ParticipantView::major, 160),
                            column("学号", ParticipantView::studentNumber, 160),
                            column("手机号", ParticipantView::phone, 160),
                            column("账号", ParticipantView::username, 160),
                            column("时间", p -> date(p.createdAt()), 160),
                            column("小组", ParticipantView::groupName, 160));
            people.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
            Runnable update =
                    () ->
                            read(
                                    () -> registrationController.participants(session, cid, reserved),
                                    v -> typedRows(people, v, ParticipantView::id),
                                    status);
            refreshers.add(update);
            if (reserved) return page(label("预约只表示参赛意向，选手仍需自行报名"), people);
            ComboBox<GroupView> group = new ComboBox<>();
            group.setPromptText("选择小组");
            Runnable groups =
                    () ->
                            read(
                                    () -> registrationController.groups(session, cid),
                                    list -> {
                                        GroupView old = group.getValue();
                                        group.getItems().setAll(list);
                                        if (old != null)
                                            list.stream()
                                                    .filter(
                                                            g ->
                                                                    g.id().equals(old.id()))
                                                    .findFirst()
                                                    .ifPresent(group::setValue);
                                    },
                                    status);
            refreshers.add(groups);
            Button create =
                    button(
                            "创建小组",
                            () -> {
                                Form f = new Form(stage, "创建小组");
                                TextField name = f.text("组名", ""), seq = f.text("出场顺序", "1");
                                f.save(
                                        "保存",
                                        () -> {
                                            String n = name.getText();
                                            int order = Integer.parseInt(seq.getText());
                                            return (Callable<String>)
                                                    () -> registrationController.addGroup(session, cid, n, order);
                                        },
                                        () -> {});
                            });
            return page(
                    bar(
                            create,
                            group,
                            button(
                                    "删除空小组",
                                    () ->
                                            safe(
                                                    status,
                                                    () -> {
                                                        if (group.getValue() == null)
                                                            throw new IllegalArgumentException(
                                                                    "请选择小组");
                                                        String gid = group.getValue().id();
                                                        if (confirm(stage, "删除当前空小组？"))
                                                            action(
                                                                    status,
                                                                    () ->
                                                                            registrationController
                                                                                    .deleteEmptyGroup(
                                                                                            session, gid));
                                                    })),
                            button(
                                    "分配所选选手",
                                    () ->
                                            safe(
                                                    status,
                                                    () -> {
                                                        String registration =
                                                                selected(people).id();
                                                        if (group.getValue() == null)
                                                            throw new IllegalArgumentException(
                                                                    "请选择小组");
                                                        String gid = group.getValue().id();
                                                        action(
                                                                status,
                                                                () ->
                                                                        registrationController.assign(
                                                                                session,
                                                                                registration,
                                                                                gid));
                                                    }))),
                    people);
        }

        Node roundPage(String cid) {
            TableView<RoundView>
                    rounds =
                            typedTable(
                                    column("轮次", RoundView::name, 180),
                                    column("类型", RoundView::type, 140),
                                    column("顺序", r -> Integer.toString(r.sequence()), 100),
                                    column(
                                            "时限（秒）",
                                            r -> Integer.toString(r.timeLimitSeconds()),
                                            120));
            TableView<RoundQuestionView> questions =
                    typedTable(
                            column(
                                    "题序",
                                    q -> Integer.toString(q.sequence()),
                                    100),
                            column("题干", RoundQuestionView::content, 420));
            Runnable update =
                    () ->
                            read(
                                    () -> questionBankController.rounds(session, cid),
                                    v -> typedRows(rounds, v, RoundView::id),
                                    status);
            refreshers.add(update);
            Runnable questionUpdate =
                    () -> {
                        RoundView r = rounds.getSelectionModel().getSelectedItem();
                        if (r != null) {
                            String rid = r.id();
                            read(
                                    () -> questionBankController.roundQuestions(session, rid),
                                    v -> {
                                        RoundView current =
                                                rounds.getSelectionModel().getSelectedItem();
                                        if (current != null && rid.equals(current.id()))
                                            typedRows(
                                                    questions,
                                                    v,
                                                    RoundQuestionView::id);
                                    },
                                    status);
                        } else questions.getItems().clear();
                    };
            refreshers.add(questionUpdate);
            rounds.getSelectionModel()
                    .selectedItemProperty()
                    .addListener((o, a, b) -> questionUpdate.run());
            Button add =
                    button(
                            "新增轮次",
                            () -> {
                                Form f = new Form(stage, "新增轮次");
                                TextField name = f.text("轮次名称", ""),
                                        seq = f.text("轮次顺序", "1"),
                                        seconds = f.text("每题时限（秒）", "30");
                                ComboBox<String> type =
                                        f.choice("类型", List.of("必答", "抢答", "风险"), "必答");
                                f.save(
                                        "保存",
                                        () -> {
                                            String n = name.getText(),
                                                    t =
                                                            switch (type.getValue()) {
                                                                case "必答" -> "REQUIRED";
                                                                case "抢答" -> "BUZZER";
                                                                default -> "RISK";
                                                            };
                                            int order = Integer.parseInt(seq.getText()),
                                                    limit = Integer.parseInt(seconds.getText());
                                            return (Callable<String>)
                                                    () ->
                                                            questionBankController.addRound(
                                                                    session, cid, n, t, order,
                                                                    limit);
                                        },
                                        () -> {});
                            });
            Button attach =
                    button(
                            "添加题目",
                            () ->
                                    safe(
                                            status,
                                            () -> {
                                                String rid = selected(rounds).id();
                                                Form f = new Form(stage, "为轮次添加题目");
                                                ComboBox<QuestionView> q =
                                                        f.field("启用题目", new ComboBox<>());
                                                q.setPrefWidth(380);
                                                read(
                                                        () ->
                                                                questionBankController
                                                                        .questionsForCompetition(
                                                                                session, cid),
                                                        list ->
                                                                q.getItems()
                                                                        .setAll(
                                                                                list.stream()
                                                                                        .filter(
                                                                                                v ->
                                                                                                        v.active())
                                                                                        .toList()),
                                                        f.status);
                                                TextField seq = f.text("题序", "1");
                                                f.save(
                                                        "添加",
                                                        () -> {
                                                            if (q.getValue() == null)
                                                                throw new IllegalArgumentException(
                                                                        "请选择题目");
                                                            String qid = q.getValue().id();
                                                            int order =
                                                                    Integer.parseInt(seq.getText());
                                                            return (Callable<Void>)
                                                                    () -> {
                                                                        questionBankController.addQuestionToRound(
                                                                                session, rid, qid,
                                                                                order);
                                                                        return null;
                                                                    };
                                                        },
                                                        () -> {});
                                            }));
            return page(
                    bar(
                            add,
                            button("编辑轮次", () -> safe(status, () -> editRound(selected(rounds)))),
                            button(
                                    "删除空轮次",
                                    () ->
                                            safe(
                                                    status,
                                                    () -> {
                                                        String rid = selected(rounds).id();
                                                        if (confirm(stage, "删除选中的空轮次？"))
                                                            action(
                                                                    status,
                                                                    () ->
                                                                            questionBankController
                                                                                    .deleteEmptyRound(
                                                                                            session,
                                                                                            rid));
                                                    })),
                            attach,
                            button(
                                    "移除所选题目",
                                    () ->
                                            safe(
                                                    status,
                                                    () -> {
                                                        String rq = selected(questions).id();
                                                        action(
                                                                status,
                                                                () ->
                                                                        questionBankController.removeRoundQuestion(
                                                                                session, rq));
                                                    }))),
                    rounds,
                    label("当前轮次题单 · 同场不得重复用题"),
                    questions);
        }

        void editRound(RoundView round) {
            Form f = new Form(stage, "编辑轮次");
            TextField name = f.text("轮次名称", round.name()),
                    seq = f.text("轮次顺序", Integer.toString(round.sequence())),
                    seconds =
                            f.text(
                                    "每题时限（秒）",
                                    Integer.toString(round.timeLimitSeconds()));
            ComboBox<String> type =
                    f.choice(
                            "类型",
                            List.of("必答", "抢答", "风险"),
                            switch (round.type()) {
                                case "REQUIRED" -> "必答";
                                case "BUZZER" -> "抢答";
                                default -> "风险";
                            });
            f.save(
                    "保存",
                    () -> {
                        String n = name.getText(),
                                t =
                                        switch (type.getValue()) {
                                            case "必答" -> "REQUIRED";
                                            case "抢答" -> "BUZZER";
                                            default -> "RISK";
                                        };
                        int order = Integer.parseInt(seq.getText()),
                                limit = Integer.parseInt(seconds.getText());
                        return (Callable<Void>)
                                () -> {
                                    questionBankController.updateRound(
                                            session, round.id(), n, t, order, limit);
                                    return null;
                                };
                    },
                    () -> {});
        }

        Node controlPage(String cid) {
            Label progress = label("尚未开始"),
                    prompt = label(""),
                    timer = label(""),
                    question = label("等待发布题目"),
                    pendingCount = label("等待赛场启动");
            timer.getStyleClass().add("timer");
            progress.getStyleClass().add("live-progress");
            question.getStyleClass().add("live-question");
            pendingCount.getStyleClass().add("live-pending");
            VBox liveBoard = new VBox(8, label("LIVE  现场态势"), progress, question, new HBox(26, timer, pendingCount));
            liveBoard.getStyleClass().add("live-board");
            TableView<PlayerSubmissionView> monitor = new TableView<>();
            monitor.setPlaceholder(label("暂无记录"));
            TableColumn<PlayerSubmissionView, String> playerName = new TableColumn<>("选手");
            playerName.setCellValueFactory(
                    value -> new ReadOnlyStringWrapper(value.getValue().name()));
            TableColumn<PlayerSubmissionView, String> submissionStatus =
                    new TableColumn<>("提交情况");
            submissionStatus.setCellValueFactory(
                    value -> new ReadOnlyStringWrapper(value.getValue().status()));
            monitor.getColumns().addAll(playerName, submissionStatus);
            TableView<RankingEntry> ranks = rankingTable();
            Button
                    start =
                            button(
                                    "开始比赛",
                                    () -> {
                                        if (confirm(stage, "开始比赛后，分组、题单、轮次与晋级名额将锁定。确认开始？"))
                                            action(
                                                    status,
                                                    () ->
                                                            competitionLiveController.startCompetition(
                                                                    session, cid));
                                    }),
                    begin =
                            button(
                                    "开始当前轮次",
                                    () ->
                                            action(
                                                    status,
                                                    () ->
                                                            competitionLiveController.startRound(
                                                                    session, cid))),
                    publish =
                            button(
                                    "发布下一题",
                                    () ->
                                            action(
                                                    status,
                                                    () ->
                                                            competitionLiveController.publish(
                                                                    session, cid))),
                    close =
                            button(
                                    "全员提交，结束本题",
                                    () ->
                                            action(
                                                    status,
                                                    () ->
                                                            competitionLiveController.closeQuestion(
                                                                    session, cid))),
                    finish =
                            button(
                                    "结束当前轮次",
                                    () ->
                                            action(
                                                    status,
                                                    () ->
                                                            competitionLiveController.finishRound(
                                                                    session, cid))),
                    archive = button("结束比赛并归档", () -> {}),
                    preview =
                            button(
                                    "预览晋级结果",
                                    () ->
                                            read(
                                                    () -> resultController.preview(session, cid),
                                                    v -> {
                                                        rankRows(ranks, v);
                                                        archive.setDisable(false);
                                                        prompt.setText("晋级预览已生成，确认后结束比赛并保存最终成绩。");
                                                    },
                                                    status));
            archive.setOnAction(
                    e -> {
                        if (confirm(stage, "确认结束竞赛并归档？归档后只能查询，不能继续答题。"))
                            action(status, () -> resultController.archive(session, cid));
                    });
            archive.setDisable(true);
            final long[] deadline = {0};
            Timeline tick =
                    new Timeline(
                            new KeyFrame(
                                    Duration.seconds(1),
                                    e ->
                                            timer.setText(
                                                    deadline[0] == 0
                                                            ? ""
                                                            : Math.max(
                                                                            0,
                                                                            (deadline[0]
                                                                                            - System
                                                                                                    .currentTimeMillis()
                                                                                            + 999)
                                                                                    / 1000)
                                                                    + " 秒")));
            tick.setCycleCount(Animation.INDEFINITE);
            tick.play();
            roomTimers.add(tick);
            record ControlSnapshot(
                    CompetitionView competition,
                    CompetitionProgressView current,
                    ActiveReleaseView release,
                    List<PlayerSubmissionView> monitor,
                    List<RankingEntry> ranks,
                    List<RoundQuestionView> questions,
                    int completed) {}
            Runnable update =
                    () ->
                            read(
                                    () -> {
                                        CompetitionView c =
                                                competitionManagementController.competitions().stream()
                                                        .filter(v -> v.id().equals(cid))
                                                        .findFirst()
                                                        .orElseThrow();
                                        CompetitionProgressView gr =
                                                competitionLiveController.progress(session, cid);
                                        List<RoundQuestionView> qs =
                                                gr == null
                                                        ? List.of()
                                                        : questionBankController.roundQuestions(
                                                                session, gr.roundId());
                                        return new ControlSnapshot(
                                                c,
                                                gr,
                                                competitionLiveController.activeRelease(session, cid),
                                                competitionLiveController.monitor(session, cid),
                                                resultController.ranking(session, cid),
                                                qs,
                                                competitionLiveController.completedQuestions(
                                                        session, cid));
                                    },
                                    v -> {
                                        boolean
                                                running =
                                                        v.competition()
                                                                .status()
                                                                .equals("比赛中"),
                                                ended =
                                                        v.competition()
                                                                .status()
                                                                .equals("已结束");
                                        CompetitionProgressView gr = v.current();
                                        ActiveReleaseView qr = v.release();
                                        boolean
                                                inRound =
                                                        gr != null
                                                                && gr.status().equals("进行中"),
                                                pending =
                                                        v.monitor().stream()
                                                                .anyMatch(
                                                                        p -> p.status().equals("待提交"));
                                        liveBoard.setVisible(!ended);
                                        liveBoard.setManaged(!ended);
                                        progress.setText(
                                                v.competition().status()
                                                        + (gr == null
                                                                ? " · 所有分组轮次已完成或尚未开赛"
                                                                : " · "
                                                                        + gr.groupName()
                                                                        + " · "
                                                                        + gr.roundName()
                                                                        + " · "
                                                                        + gr.status()
                                                                        + " · 已完成 "
                                                                        + v.completed()
                                                                        + " / "
                                                                        + v.questions().size()
                                                                        + " 题"));
                                        deadline[0] = qr == null ? 0 : qr.deadline();
                                        question.setText(
                                                qr == null ? "当前无正在作答的题目" : qr.content());
                                        long waiting =
                                                v.monitor().stream()
                                                        .filter(p -> p.status().equals("待提交"))
                                                        .count();
                                        pendingCount.setText(qr == null ? "暂无待提交题目" : "待提交  " + waiting + " 人");
                                        timer.setText(
                                                deadline[0] == 0
                                                        ? ""
                                                        : Math.max(
                                                                        0,
                                                                        (deadline[0]
                                                                                        - System
                                                                                                .currentTimeMillis()
                                                                                        + 999)
                                                                                / 1000)
                                                                + " 秒");
                                        start.setDisable(
                                                !v.competition().status().equals("报名截止"));
                                        begin.setDisable(
                                                !running
                                                        || gr == null
                                                        || !gr.status().equals("待开始"));
                                        publish.setDisable(
                                                !running
                                                        || !inRound
                                                        || qr != null
                                                        || v.completed() >= v.questions().size());
                                        close.setDisable(qr == null || pending);
                                        finish.setDisable(
                                                !running
                                                        || !inRound
                                                        || qr != null
                                                        || v.completed() < v.questions().size());
                                        preview.setDisable(!running || gr != null);
                                        archive.setDisable(true);
                                        prompt.setText(
                                                ended
                                                        ? "成绩已归档，可以导出。"
                                                        : qr != null && pending
                                                                ? "还有选手未提交，请等待倒计时结束。"
                                                                : gr == null && running
                                                                        ? "所有小组完成，请预览晋级并归档。"
                                                                        : "按小组、轮次与题序操作；不可用的操作需先完成前一步。");
                                        monitor.setItems(FXCollections.observableArrayList(v.monitor()));
                                        rankRows(ranks, v.ranks());
                                    },
                                    status);
            refreshers.add(update);
            VBox controls =
                    page(
                            label("赛事指令"),
                            bar(start, begin, publish),
                            bar(close, finish),
                            label("赛后处理"),
                            bar(preview, archive),
                            button("导出 CSV", () -> export(cid)),
                            prompt);
            VBox monitoring = page(label("当前组提交情况"), monitor, label("全场排行榜"), ranks);
            controls.getStyleClass().add("control-panel"); monitoring.getStyleClass().add("monitor-panel");
            SplitPane split = new SplitPane(scroll(controls), monitoring);
            split.setDividerPositions(.52);
            split.getStyleClass().add("control-split");
            VBox.setVgrow(split, Priority.ALWAYS);
            VBox content = page(liveBoard, split); content.getStyleClass().add("control-page"); return content;
        }

        void room(CompetitionView competition) {
            String cid = competition.id();
            if (existing("room" + cid)) return;
            Label heading = title(competition.name()),
                    info = label("等待工作人员发布本组题目"),
                    timer = label(""),
                    content = label(""),
                    feedback = label("");
            timer.getStyleClass().add("timer");
            info.getStyleClass().add("room-meta");
            content.getStyleClass().add("question-content");
            ToggleGroup choices = new ToggleGroup();
            VBox options = new VBox(12);
            List<RadioButton> radios = new ArrayList<>();
            for (String key : List.of("A", "B", "C", "D")) {
                RadioButton r = new RadioButton(key);
                r.setUserData(key);
                r.setToggleGroup(choices);
                r.setWrapText(true);
                r.setMaxWidth(Double.MAX_VALUE);
                r.getStyleClass().add("option-card");
                options.getChildren().add(r);
                radios.add(r);
            }
            TableView<RankingEntry> ranks = rankingTable();
            PublishedQuestionView[] current = {null};
            boolean[] busy = {false};
            Button submit = button("提交答案", () -> {});
            submit.getStyleClass().add("primary-button");
            Runnable buttons =
                    () -> {
                        PublishedQuestionView q = current[0];
                        boolean available =
                                q != null
                                        && q.status().equals("可作答")
                                        && System.currentTimeMillis() < q.deadline();
                        submit.setDisable(
                                !available || choices.getSelectedToggle() == null || busy[0]);
                        radios.forEach(r -> r.setDisable(!available || busy[0]));
                    };
            choices.selectedToggleProperty().addListener((o, a, b) -> buttons.run());
            submit.setOnAction(
                    e -> {
                        if (current[0] == null || choices.getSelectedToggle() == null) return;
                        String release = current[0].releaseId(),
                                answer = choices.getSelectedToggle().getUserData().toString();
                        busy[0] = true;
                        buttons.run();
                        feedback.setText("正在提交…");
                        runtime.mutate(
                                () -> competitionRoomController.submitAnswer(session, release, answer),
                                score -> {
                                    busy[0] = false;
                                    feedback.setText(
                                            "已保存，本题 " + (score >= 0 ? "+" : "") + score + " 分");
                                },
                                ex -> {
                                    busy[0] = false;
                                    buttons.run();
                                    error(feedback, ex);
                                });
                    });
            VBox questionCard = page(content, options, submit, feedback);
            questionCard.getStyleClass().add("question-card");
            ScrollPane answerPage = scroll(questionCard);
            // The compact room board shows the five required columns; full ranking retains
            // tie-break statistics.
            ranks.getColumns().remove(5);
            ranks.getColumns().remove(4);
            SplitPane split = new SplitPane();
            split.setDividerPositions(.55);
            TabPane compact = tabs(tab("答题", null), tab("排行榜", null));
            StackPane body = new StackPane();
            final boolean[] narrow = {false};
            Runnable layout =
                    () -> {
                        boolean small = stage.getWidth() < 900;
                        if (body.getChildren().isEmpty() || small != narrow[0]) {
                            narrow[0] = small;
                            split.getItems().clear();
                            compact.getTabs().forEach(t -> t.setContent(null));
                            body.getChildren().clear();
                            if (small) {
                                compact.getTabs().get(0).setContent(answerPage);
                                compact.getTabs().get(1).setContent(ranks);
                                body.getChildren().add(compact);
                            } else {
                                split.getItems().setAll(answerPage, ranks);
                                body.getChildren().add(split);
                            }
                        }
                    };
            stage.widthProperty().addListener((o, a, b) -> layout.run());
            layout.run();
            VBox.setVgrow(body, Priority.ALWAYS);
            Timeline tick =
                    new Timeline(
                            new KeyFrame(
                                    Duration.seconds(1),
                                    e -> {
                                        PublishedQuestionView q = current[0];
                                        timer.setText(
                                                q == null || !q.status().equals("可作答")
                                                        ? ""
                                                        : Math.max(
                                                                        0,
                                                                        (q.deadline()
                                                                                        - System
                                                                                                .currentTimeMillis()
                                                                                        + 999)
                                                                                / 1000)
                                                                + " 秒");
                                        buttons.run();
                                    }));
            tick.setCycleCount(Animation.INDEFINITE);
            tick.play();
            roomTimers.add(tick);
            record RoomSnapshot(
                    PublishedQuestionView question, List<RankingEntry> ranks, String state) {}
            Runnable update =
                    () ->
                            read(
                                    () ->
                                            new RoomSnapshot(
                                                    competitionLiveController.room(session, cid),
                                                    resultController.ranking(session, cid),
                                                    competitionManagementController.competitions().stream()
                                                            .filter(c -> c.id().equals(cid))
                                                            .findFirst()
                                                            .orElseThrow()
                                                            .status()),
                                    v -> {
                                        PublishedQuestionView q = v.question();
                                        if (q == null) {
                                            current[0] = null;
                                            content.setText("");
                                            options.setVisible(false);
                                            info.setText(v.state() + " · 等待工作人员发布本组题目");
                                        } else {
                                            if (current[0] == null
                                                    || !current[0]
                                                            .releaseId()
                                                            .equals(q.releaseId())) {
                                                choices.selectToggle(null);
                                                feedback.setText("");
                                            }
                                            current[0] = q;
                                            options.setVisible(true);
                                            content.setText(q.content());
                                            for (int i = 0; i < 4; i++)
                                                radios.get(i)
                                                        .setText(
                                                                "ABCD".charAt(i)
                                                                        + ". "
                                                                        + q.options().get(i));
                                            info.setText(
                                                    v.state()
                                                            + " · "
                                                            + q.roundName()
                                                            + " · "
                                                            + q.status());
                                            if (q.score() != null) {
                                                feedback.setText(
                                                        q.status()
                                                                + "，选择 "
                                                                + q.answer()
                                                                + "，本题 "
                                                                + (q.score() >= 0 ? "+" : "")
                                                                + q.score()
                                                                + " 分");
                                                choices.selectToggle(
                                                        radios.get("ABCD".indexOf(q.answer())));
                                            } else if (!q.status().equals("可作答"))
                                                feedback.setText(q.status());
                                        }
                                        rankRows(ranks, v.ranks());
                                        timer.setText(
                                                q == null || !q.status().equals("可作答")
                                                        ? ""
                                                        : Math.max(
                                                                        0,
                                                                        (q.deadline()
                                                                                        - System
                                                                                                .currentTimeMillis()
                                                                                        + 999)
                                                                                / 1000)
                                                                + " 秒");
                                        if (v.state().equals("已结束"))
                                            v.ranks().stream()
                                                    .filter(r -> r.playerId().equals(session.id()))
                                                    .findFirst()
                                                    .ifPresent(
                                                            r ->
                                                                    info.setText(
                                                                            "比赛已结束 · 最终 "
                                                                                    + r.score()
                                                                                    + " 分 · 第 "
                                                                                    + r.rank()
                                                                                    + " 名 · "
                                                                                    + r
                                                                                            .promotion()));
                                        buttons.run();
                                    },
                                    feedback);
            refreshers.add(update);
            openTab(
                    "room" + cid,
                    competition.name() + " · 比赛室",
                    page(heading, bar(info, timer), body));
            update.run();
        }
    }
}
