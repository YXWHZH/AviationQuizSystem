package cn.edu.aviationquiz;

import cn.edu.aviationquiz.dao.QuestionDao;
import cn.edu.aviationquiz.dao.SqliteQuestionDao;
import cn.edu.aviationquiz.entity.*;
import cn.edu.aviationquiz.service.RankingService;
import cn.edu.aviationquiz.service.ScoringService;
import cn.edu.aviationquiz.util.CsvService;
import cn.edu.aviationquiz.util.Database;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class App extends Application {
    private final QuestionDao questionDao = new SqliteQuestionDao();
    private final List<Team> teams = new ArrayList<>();
    private final IntegerProperty remaining = new SimpleIntegerProperty(30);
    private Timeline timeline;
    private TableView<Question> questionTable;
    private TableView<Team> rankingTable;
    private Label status;

    @Override public void start(Stage stage) {
        Database.initialize();
        seedQuestions(); seedTeams();
        BorderPane root=new BorderPane();
        root.setTop(header()); root.setCenter(tabs(stage));
        status=new Label("系统已就绪"); status.getStyleClass().add("status-bar"); root.setBottom(status);
        Scene scene=new Scene(root,1180,760);
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
        stage.setTitle("航空知识竞赛管理系统"); stage.setMinWidth(960); stage.setMinHeight(640);
        stage.setScene(scene); stage.show();
    }

    private HBox header(){
        Label title=new Label("航空知识竞赛管理系统"); title.getStyleClass().add("app-title");
        Label subtitle=new Label("题库 · 竞赛 · 排名 · 归档"); subtitle.getStyleClass().add("app-subtitle");
        Region spacer=new Region(); HBox.setHgrow(spacer,Priority.ALWAYS);
        HBox box=new HBox(16,title,spacer,subtitle); box.setAlignment(Pos.CENTER_LEFT); box.getStyleClass().add("header"); return box;
    }

    private TabPane tabs(Stage stage){
        TabPane pane=new TabPane(); pane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        pane.getTabs().addAll(new Tab("题库管理",questionPage()),new Tab("竞赛控制台",competitionPage()),
                new Tab("实时排名",rankingPage(stage)),new Tab("系统说明",aboutPage())); return pane;
    }

    private Pane questionPage(){
        questionTable=new TableView<>();
        TableColumn<Question,Number> id=column("编号",q->new SimpleLongProperty(q.getValue().id())); id.setPrefWidth(70);
        TableColumn<Question,String> category=column("分类",q->new SimpleStringProperty(q.getValue().category().toString())); category.setPrefWidth(120);
        TableColumn<Question,String> type=column("题型",q->new SimpleStringProperty(q.getValue().type().toString())); type.setPrefWidth(100);
        TableColumn<Question,String> content=column("题干",q->new SimpleStringProperty(q.getValue().content())); content.setPrefWidth(520);
        TableColumn<Question,Number> score=column("分值",q->new SimpleIntegerProperty(q.getValue().defaultScore())); score.setPrefWidth(80);
        questionTable.getColumns().addAll(id,category,type,content,score); refreshQuestions();
        Button add=new Button("新增题目"); add.getStyleClass().add("primary-button"); add.setOnAction(e->showQuestionDialog());
        Button delete=new Button("删除所选"); delete.setOnAction(e->deleteQuestion());
        Label help=new Label("支持民航史、飞行原理、航空法规三类题目；数据实时保存到 SQLite。"); help.getStyleClass().add("help-text");
        HBox actions=new HBox(10,add,delete,new Region(),help); HBox.setHgrow(actions.getChildren().get(2),Priority.ALWAYS); actions.setAlignment(Pos.CENTER_LEFT);
        VBox page=new VBox(14,sectionTitle("竞赛题库"),actions,questionTable); VBox.setVgrow(questionTable,Priority.ALWAYS); return padded(page);
    }

    private Pane competitionPage(){
        ComboBox<Team> teamBox=new ComboBox<>(FXCollections.observableArrayList(teams));
        teamBox.setConverter(new javafx.util.StringConverter<>(){public String toString(Team t){return t==null?"":t.getName();}public Team fromString(String s){return null;}});
        teamBox.getSelectionModel().selectFirst();
        ComboBox<Question> questionBox=new ComboBox<>(FXCollections.observableArrayList(questionDao.findAll()));
        questionBox.setConverter(new javafx.util.StringConverter<>(){public String toString(Question q){return q==null?"":"#"+q.id()+" "+q.content();}public Question fromString(String s){return null;}});
        questionBox.getSelectionModel().selectFirst();
        ComboBox<RoundType> roundBox=new ComboBox<>(FXCollections.observableArrayList(RoundType.values())); roundBox.getSelectionModel().selectFirst();
        TextField answer=new TextField(); answer.setPromptText("例如 A"); answer.setAccessibleHelp("输入题目的选项字母");
        Label timer=new Label(); timer.textProperty().bind(remaining.asString("%d 秒")); timer.getStyleClass().add("timer");
        Button start=new Button("开始计时"); start.getStyleClass().add("primary-button");
        start.setOnAction(e->startTimer());
        Button submit=new Button("提交答案"); submit.setOnAction(e->{
            Team team=teamBox.getValue(); Question q=questionBox.getValue(); if(team==null||q==null){message("请选择队伍和题目",true);return;}
            CompetitionRound round=createRound(roundBox.getValue()); long elapsed=30-remaining.get(); if(timeline!=null)timeline.stop();
            int change=new ScoringService().score(team,round,q,answer.getText(),elapsed);
            message("判分完成："+(change>=0?"+":"")+change+" 分",false); if(rankingTable!=null)refreshRanking(); answer.clear();
        });
        GridPane form=new GridPane(); form.setHgap(14); form.setVgap(16);
        form.addRow(0,new Label("参赛队伍"),teamBox); form.addRow(1,new Label("竞赛轮次"),roundBox);
        form.addRow(2,new Label("当前题目"),questionBox); form.addRow(3,new Label("选手答案"),answer);
        teamBox.setMaxWidth(Double.MAX_VALUE);roundBox.setMaxWidth(Double.MAX_VALUE);questionBox.setMaxWidth(Double.MAX_VALUE);answer.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(teamBox,Priority.ALWAYS);GridPane.setHgrow(roundBox,Priority.ALWAYS);GridPane.setHgrow(questionBox,Priority.ALWAYS);GridPane.setHgrow(answer,Priority.ALWAYS);
        HBox timerBar=new HBox(18,timer,start,submit); timerBar.setAlignment(Pos.CENTER_LEFT);
        Label rules=new Label("必答题答错不扣分；抢答题答错或超时扣分；风险题按 2 倍分值加减分。倒计时归零后按超时处理。"); rules.setWrapText(true);rules.getStyleClass().add("help-text");
        VBox card=new VBox(18,sectionTitle("现场答题控制"),form,timerBar,rules); card.getStyleClass().add("card");
        VBox page=new VBox(18,card); page.setMaxWidth(820); return padded(page);
    }

    private Pane rankingPage(Stage stage){
        rankingTable=new TableView<>();
        TableColumn<Team,String> name=column("队伍",t->new SimpleStringProperty(t.getValue().getName()));name.setPrefWidth(260);
        TableColumn<Team,Number> score=column("总分",t->new SimpleIntegerProperty(t.getValue().getScore()));score.setPrefWidth(160);
        TableColumn<Team,Number> correct=column("答对数",t->new SimpleIntegerProperty(t.getValue().getCorrectAnswers()));correct.setPrefWidth(160);
        TableColumn<Team,Number> time=column("累计用时（秒）",t->new SimpleLongProperty(t.getValue().getElapsedSeconds()));time.setPrefWidth(200);
        rankingTable.getColumns().addAll(name,score,correct,time);refreshRanking();
        Button refresh=new Button("刷新排名"); refresh.setOnAction(e->refreshRanking());
        Button export=new Button("导出成绩单");export.getStyleClass().add("primary-button");export.setOnAction(e->export(stage));
        Label rule=new Label("排序规则：总分降序 → 答对数降序 → 用时升序 → 队伍编号升序");rule.getStyleClass().add("help-text");
        HBox actions=new HBox(10,refresh,export,new Region(),rule);HBox.setHgrow(actions.getChildren().get(2),Priority.ALWAYS);actions.setAlignment(Pos.CENTER_LEFT);
        VBox page=new VBox(14,sectionTitle("实时排行榜"),actions,rankingTable);VBox.setVgrow(rankingTable,Priority.ALWAYS);return padded(page);
    }

    private Pane aboutPage(){
        Label text=new Label("本系统用于组织航空知识竞赛，覆盖题库管理、选手分组、多轮计时计分、实时排名、晋级判定和历史成绩归档。\n\n"+
                "数据采用符合第三范式的 SQLite 关系数据库保存；计分规则由轮次继承体系多态实现；成绩单使用 UTF-8 CSV 导出。");
        text.setWrapText(true);text.getStyleClass().add("about-text");
        return padded(new VBox(18,sectionTitle("系统说明"),text));
    }

    private void showQuestionDialog(){
        Dialog<Question> dialog=new Dialog<>();dialog.setTitle("新增题目");dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK,ButtonType.CANCEL);
        ComboBox<QuestionCategory> category=new ComboBox<>(FXCollections.observableArrayList(QuestionCategory.values()));category.getSelectionModel().selectFirst();
        ComboBox<QuestionType> type=new ComboBox<>(FXCollections.observableArrayList(QuestionType.values()));type.getSelectionModel().selectFirst();
        TextField content=new TextField(), options=new TextField("正确,错误"), answer=new TextField("A"), score=new TextField("10");
        GridPane grid=new GridPane();grid.setHgap(10);grid.setVgap(12);grid.addRow(0,new Label("分类"),category);grid.addRow(1,new Label("题型"),type);grid.addRow(2,new Label("题干"),content);grid.addRow(3,new Label("选项（逗号分隔）"),options);grid.addRow(4,new Label("正确答案"),answer);grid.addRow(5,new Label("分值"),score);dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(b->{if(b!=ButtonType.OK)return null;try{return new Question(0,category.getValue(),type.getValue(),content.getText(),List.of(options.getText().split("[,，]")),answer.getText(),Integer.parseInt(score.getText()),true);}catch(Exception e){message(e.getMessage(),true);return null;}});
        dialog.showAndWait().ifPresent(q->{questionDao.save(q);refreshQuestions();message("题目已保存",false);});
    }
    private void deleteQuestion(){Question q=questionTable.getSelectionModel().getSelectedItem();if(q==null){message("请先选择题目",true);return;}questionDao.delete(q.id());refreshQuestions();message("题目已删除",false);}
    private void refreshQuestions(){if(questionTable!=null)questionTable.setItems(FXCollections.observableArrayList(questionDao.findAll()));}
    private void refreshRanking(){rankingTable.setItems(FXCollections.observableArrayList(new RankingService().rank(teams)));rankingTable.refresh();}
    private void startTimer(){if(timeline!=null)timeline.stop();remaining.set(30);timeline=new Timeline(new KeyFrame(Duration.seconds(1),e->{remaining.set(remaining.get()-1);if(remaining.get()<=0){timeline.stop();message("答题超时，请提交进行判分",true);}}));timeline.setCycleCount(30);timeline.play();message("计时已开始",false);}
    private CompetitionRound createRound(RoundType type){return switch(type){case REQUIRED->new RequiredRound("必答轮",30);case BUZZER->new BuzzerRound("抢答轮",30);case RISK->new RiskRound("风险轮",30,2);};}
    private void export(Stage stage){FileChooser chooser=new FileChooser();chooser.setTitle("导出成绩单");chooser.setInitialFileName("竞赛成绩单.csv");chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV 文件","*.csv"));File file=chooser.showSaveDialog(stage);if(file==null)return;try{new CsvService().exportRanking(file.toPath(),teams);message("成绩单已导出："+file.getName(),false);}catch(Exception e){message("导出失败："+e.getMessage(),true);}}
    private void seedQuestions(){if(questionDao.count()>0)return;questionDao.save(new Question(0,QuestionCategory.CIVIL_AVIATION_HISTORY,QuestionType.SINGLE_CHOICE,"中华人民共和国第一家民用航空公司成立于哪一年？",List.of("1949 年","1950 年","1955 年","1960 年"),"B",10,true));questionDao.save(new Question(0,QuestionCategory.FLIGHT_PRINCIPLE,QuestionType.SINGLE_CHOICE,"飞机升力主要与下列哪项有关？",List.of("机翼上下表面的气流","客舱灯光","座椅数量","机场名称"),"A",10,true));questionDao.save(new Question(0,QuestionCategory.AVIATION_REGULATION,QuestionType.TRUE_FALSE,"旅客可以随意进入机场控制区。",List.of("正确","错误"),"B",10,true));}
    private void seedTeams(){Team a=new Team(1,"蓝天队");a.addMember(new Player(1,"2026001","张三"));a.addMember(new Player(2,"2026002","李四"));Team b=new Team(2,"飞鹰队");b.addMember(new Player(3,"2026003","王五"));b.addMember(new Player(4,"2026004","赵六"));teams.addAll(List.of(a,b));}
    private Label sectionTitle(String text){Label label=new Label(text);label.getStyleClass().add("section-title");return label;}
    private VBox padded(javafx.scene.Node node){VBox box=new VBox(node);box.setPadding(new Insets(24));VBox.setVgrow(node,Priority.ALWAYS);return box;}
    private void message(String text,boolean error){if(status!=null){status.setText(text);status.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("error"),error);}}
    private <S,T> TableColumn<S,T> column(String title,javafx.util.Callback<TableColumn.CellDataFeatures<S,T>,javafx.beans.value.ObservableValue<T>> value){TableColumn<S,T> column=new TableColumn<>(title);column.setCellValueFactory(value);return column;}
    public static void main(String[] args){launch(args);}
}
