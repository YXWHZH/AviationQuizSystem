package cn.edu.aviationquiz.service;

import cn.edu.aviationquiz.dao.Store;

import java.nio.file.Path;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.IntStream;

/** Creates a complete, idempotent local demonstration data set. */
public final class DemoDataSeeder {
    private static final String PASSWORD = "123456";
    private static final List<String> PLAYER_IDS =
            IntStream.rangeClosed(1, 30).mapToObj(i -> "DEMO_P" + String.format("%02d", i)).toList();
    private static final List<String> PLAYER_NAMES = List.of(
            "张宇航", "李晨曦", "王浩然", "赵雨桐", "陈思远", "刘欣怡", "杨博文", "黄子涵", "周俊杰", "吴佳宁",
            "徐天佑", "孙梦琪", "胡嘉豪", "朱雅雯", "高明轩", "林诗涵", "何宇辰", "郭雨欣", "马睿哲", "罗心怡",
            "梁泽宇", "宋语桐", "郑凯文", "谢婉清", "韩子墨", "唐可欣", "冯皓轩", "曹静怡", "彭一航", "曾若琳");

    private DemoDataSeeder() {}

    public static boolean seed(Store store, Clock clock) {
        return store.transaction(
                db -> {
                    long before = db.one("SELECT (SELECT COUNT(*) FROM player)+(SELECT COUNT(*) FROM competition)+(SELECT COUNT(*) FROM question)+(SELECT COUNT(*) FROM registration)+(SELECT COUNT(*) FROM reservation)+(SELECT COUNT(*) FROM result) n").number("n");

                    long now = clock.millis();
                    for (int i = 0; i < PLAYER_IDS.size(); i++) {
                        db.execute(
                                "INSERT OR IGNORE INTO player VALUES(?,?,?,?,?,?,?,?,?,1)",
                                PLAYER_IDS.get(i),
                                "player0" + (i + 1),
                                Passwords.hash(PASSWORD),
                                PLAYER_NAMES.get(i),
                                "13" + String.format("%09d", 800000000 + i),
                                switch (i % 3) { case 0 -> "中国民航大学"; case 1 -> "中国民用航空飞行学院"; default -> "南京航空航天大学"; },
                                switch (i % 3) { case 0 -> "航空工程学院"; case 1 -> "空中交通管理学院"; default -> "民航学院"; },
                                switch (i % 3) { case 0 -> "飞行器动力工程"; case 1 -> "交通运输"; default -> "航空服务艺术与管理"; },
                                "2026" + String.format("%04d", i + 1));
                    }

                    List<QuestionSeed> questions = questions();
                    for (int i = 0; i < questions.size(); i++) {
                        QuestionSeed q = questions.get(i);
                        db.execute(
                                "INSERT OR IGNORE INTO question VALUES(?,?,?,?,?,?,?,?,1)",
                                questionId(i + 1),
                                q.content(),
                                q.category(),
                                q.options().get(0),
                                q.options().get(1),
                                q.options().get(2),
                                q.options().get(3),
                                q.answer());
                    }

                    addCompetition(
                            db,
                            "DEMO_FUTURE",
                            "未来航空知识预约赛",
                            "面向航空爱好者的预约竞赛，目前尚未开放正式报名。",
                            days(now, 7),
                            days(now, 10),
                            days(now, 14),
                            "未开放",
                            3);
                    addCompetition(
                            db,
                            "DEMO_OPEN",
                            "航空知识公开选拔赛",
                            "公开报名中的航空知识选拔赛，可体验选手自主报名流程。",
                            days(now, -1),
                            days(now, 5),
                            days(now, 7),
                            "报名中",
                            3);
                    addCompetition(
                            db,
                            "DEMO_READY",
                            "航空知识竞赛决赛",
                            "已完成报名、分组和题单配置，可由工作人员直接开始比赛。",
                            days(now, -10),
                            days(now, -2),
                            days(now, 1),
                            "报名截止",
                            3);
                    addCompetition(
                            db,
                            "DEMO_HISTORY",
                            "航空知识竞赛往届赛",
                            "已结束并归档的往届竞赛，用于历史成绩查询和 CSV 导出。",
                            days(now, -40),
                            days(now, -35),
                            days(now, -30),
                            "已结束",
                            3);
                    addCompetition(db, "DEMO_FUTURE_HISTORY", "中国民航发展史挑战赛", "从早期航线到现代枢纽，系统考察中国与世界民航发展历程。", days(now, 12), days(now, 16), days(now, 20), "未开放", 5);
                    addCompetition(db, "DEMO_FUTURE_PRINCIPLE", "飞行原理新星赛", "面向航空相关专业低年级学生的空气动力学与飞行基础竞赛。", days(now, 20), days(now, 24), days(now, 28), "未开放", 6);
                    addCompetition(db, "DEMO_OPEN_LAW", "航空安全法规知识赛", "围绕旅客安全、危险品运输和机场运行规范开展的法规专题赛。", days(now, -2), days(now, 4), days(now, 6), "报名中", 8);
                    addCompetition(db, "DEMO_OPEN_MIX", "校园航空文化节综合赛", "覆盖民航史、飞行原理和航空法规的校园综合知识竞赛。", days(now, -3), days(now, 3), days(now, 5), "报名中", 10);
                    addCompetition(db, "DEMO_READY_HISTORY", "民航史经典案例赛", "报名已截止，参赛选手将按小组完成民航史专题答题。", days(now, -12), days(now, -3), days(now, 2), "报名截止", 6);
                    addCompetition(db, "DEMO_READY_LAW", "客舱与机场安全规范赛", "聚焦客舱安全、机场秩序和危险品运输规定。", days(now, -9), days(now, -1), days(now, 3), "报名截止", 6);
                    addCompetition(db, "DEMO_HISTORY_PRINCIPLE", "飞行原理春季邀请赛", "已归档的飞行原理专题赛事，可用于查看历史排名。", days(now, -70), days(now, -65), days(now, -60), "已结束", 5);
                    addCompetition(db, "DEMO_HISTORY_MIX", "航空知识年度精英赛", "已归档的年度综合赛事，包含三类知识和多轮比赛。", days(now, -100), days(now, -95), days(now, -90), "已结束", 8);

                    categories(db, "DEMO_FUTURE", "民航史", "飞行原理", "航空法规");
                    categories(db, "DEMO_OPEN", "民航史", "飞行原理", "航空法规");
                    categories(db, "DEMO_READY", "民航史", "飞行原理", "航空法规");
                    categories(db, "DEMO_HISTORY", "民航史", "飞行原理", "航空法规");
                    categories(db, "DEMO_FUTURE_HISTORY", "民航史");
                    categories(db, "DEMO_FUTURE_PRINCIPLE", "飞行原理");
                    categories(db, "DEMO_OPEN_LAW", "航空法规");
                    categories(db, "DEMO_OPEN_MIX", "民航史", "飞行原理", "航空法规");
                    categories(db, "DEMO_READY_HISTORY", "民航史");
                    categories(db, "DEMO_READY_LAW", "航空法规");
                    categories(db, "DEMO_HISTORY_PRINCIPLE", "飞行原理");
                    categories(db, "DEMO_HISTORY_MIX", "民航史", "飞行原理", "航空法规");

                    reserve(db, "DEMO_FUTURE", 0, now - 3_600_000);
                    reserve(db, "DEMO_FUTURE", 1, now - 1_800_000);
                    reserve(db, "DEMO_FUTURE", 2, now - 600_000);
                    reserve(db, "DEMO_OPEN", 0, now - 86_400_000);
                    reserve(db, "DEMO_OPEN", 1, now - 80_000_000);
                    for (int i = 0; i < 4; i++)
                        register(db, "DEMO_OPEN", i, now - 50_000_000 + i * 1_000);
                    for (int i = 0; i < 12; i++) reserve(db, "DEMO_FUTURE_HISTORY", i, now - 200_000 + i * 1_000);
                    for (int i = 8; i < 22; i++) reserve(db, "DEMO_FUTURE_PRINCIPLE", i, now - 180_000 + i * 1_000);
                    for (int i = 3; i < 21; i++) register(db, "DEMO_OPEN_LAW", i, now - 160_000 + i * 1_000);
                    for (int i = 0; i < 24; i++) register(db, "DEMO_OPEN_MIX", i, now - 140_000 + i * 1_000);

                    seedEntrants(db, "DEMO_READY", now - 100_000, false);
                    seedEntrants(db, "DEMO_HISTORY", days(now, -34), true);
                    seedEntrants(db, "DEMO_READY_HISTORY", now - 90_000, false);
                    seedEntrants(db, "DEMO_READY_LAW", now - 80_000, false);
                    seedEntrants(db, "DEMO_HISTORY_PRINCIPLE", days(now, -64), true);
                    seedEntrants(db, "DEMO_HISTORY_MIX", days(now, -94), true);
                    seedRounds(db);
                    seedResults(db);
                    seedResults(db, "DEMO_HISTORY_PRINCIPLE", 110);
                    seedResults(db, "DEMO_HISTORY_MIX", 140);
                    long after = db.one("SELECT (SELECT COUNT(*) FROM player)+(SELECT COUNT(*) FROM competition)+(SELECT COUNT(*) FROM question)+(SELECT COUNT(*) FROM registration)+(SELECT COUNT(*) FROM reservation)+(SELECT COUNT(*) FROM result) n").number("n");
                    return after > before;
                });
    }

    private static void addCompetition(
            Store.UnitOfWork db,
            String id,
            String name,
            String description,
            long start,
            long end,
            long time,
            String status,
            int quota)
            throws Exception {
        db.execute(
                "INSERT OR IGNORE INTO competition VALUES(?,?,?,?,?,?,?,?)",
                id,
                name,
                description,
                start,
                end,
                time,
                status,
                quota);
    }

    private static void categories(Store.UnitOfWork db, String competition, String... values) throws Exception {
        for (String category : values)
            db.execute("INSERT OR IGNORE INTO competition_category VALUES(?,?)", competition, category);
    }

    private static void reserve(Store.UnitOfWork db, String competition, int player, long time)
            throws Exception {
        db.execute(
                "INSERT OR IGNORE INTO reservation VALUES(?,?,?,?, '有效')",
                "DEMO_V_" + competition.substring(5) + "_" + (player + 1),
                PLAYER_IDS.get(player),
                competition,
                time);
    }

    private static String register(Store.UnitOfWork db, String competition, int player, long time)
            throws Exception {
        String id = "DEMO_REG_" + competition.substring(5) + "_" + (player + 1);
        db.execute(
                "INSERT OR IGNORE INTO registration VALUES(?,?,?,?, '有效')",
                id,
                PLAYER_IDS.get(player),
                competition,
                time);
        return id;
    }

    private static void seedEntrants(
            Store.UnitOfWork db, String competition, long time, boolean history) throws Exception {
        String suffix = competition.substring(5);
        String groupA = "DEMO_G_" + suffix + "_A";
        String groupB = "DEMO_G_" + suffix + "_B";
        db.execute("INSERT OR IGNORE INTO competition_group VALUES(?,?,?,1)", groupA, competition, "A组");
        db.execute("INSERT OR IGNORE INTO competition_group VALUES(?,?,?,2)", groupB, competition, "B组");
        for (int i = 0; i < PLAYER_IDS.size(); i++) {
            String registration = register(db, competition, i, time + i * 1_000);
            db.execute(
                    "INSERT OR IGNORE INTO group_assignment VALUES(?,?,?,?)",
                    "DEMO_GA_" + suffix + "_" + (i + 1),
                    registration,
                    i < 3 ? groupA : groupB,
                    time + 10_000 + i * 1_000);
        }
    }

    private static void seedRounds(Store.UnitOfWork db) throws Exception {
        String[] names = {"第一轮 必答题", "第二轮 抢答题", "第三轮 风险题"};
        String[] types = {"REQUIRED", "BUZZER", "RISK"};
        for (int round = 0; round < 3; round++) {
            String roundId = "DEMO_RD_" + (round + 1);
            db.execute(
                    "INSERT OR IGNORE INTO competition_round VALUES(?,?,?,?,?,30)",
                    roundId,
                    "DEMO_READY",
                    names[round],
                    types[round],
                    round + 1);
            for (int position = 0; position < 3; position++) {
                int questionNumber = round * 3 + position + 1;
                if (!db.exists("SELECT id FROM round_question WHERE id=?", "DEMO_RQ_" + questionNumber))
                    db.execute(
                            "INSERT INTO round_question VALUES(?,?,?,?)",
                            "DEMO_RQ_" + questionNumber,
                            roundId,
                            questionId(questionNumber),
                            position + 1);
            }
        }
    }

    private static void seedResults(Store.UnitOfWork db) throws Exception {
        seedResults(db, "DEMO_HISTORY", 120);
    }

    private static void seedResults(Store.UnitOfWork db, String competition, int topScore) throws Exception {
        String suffix = competition.substring(5);
        for (int i = 0; i < PLAYER_IDS.size(); i++)
            db.execute(
                    "INSERT OR IGNORE INTO result VALUES(?,?,?,?,?,?)",
                    "DEMO_RS_" + suffix + "_" + (i + 1),
                    competition,
                    PLAYER_IDS.get(i),
                    Math.max(10, topScore - i * 3),
                    i + 1,
                    i < 3 ? 1 : 0);
    }

    private static long days(long instant, int count) {
        return Instant.ofEpochMilli(instant).plus(count, ChronoUnit.DAYS).toEpochMilli();
    }

    private static String questionId(int number) {
        return "DEMO_Q" + String.format("%02d", number);
    }

    private record QuestionSeed(
            String category, String content, List<String> options, String answer) {}

    private static List<QuestionSeed> questions() {
        return List.of(
                q("民航史", "中华人民共和国第一家民用航空运输企业成立于哪一年？", "1949年", "1950年", "1955年", "1960年", "B"),
                q(
                        "民航史",
                        "中国民航首次开辟北京至上海定期航线是在什么时期？",
                        "20世纪20年代",
                        "20世纪30年代",
                        "20世纪50年代",
                        "20世纪80年代",
                        "C"),
                q("民航史", "被称为世界航空先驱并于1903年完成动力飞行的是谁？", "莱特兄弟", "冯如", "林白", "齐柏林", "A"),
                q("民航史", "中国第一位飞机设计师和飞行家冯如的祖籍位于哪里？", "广东", "北京", "四川", "江苏", "A"),
                q("民航史", "喷气式客机大规模投入商业运营主要始于哪个年代？", "1920年代", "1930年代", "1950年代", "1990年代", "C"),
                q("民航史", "国际民用航空组织的英文缩写是什么？", "IATA", "ICAO", "FAA", "CAAC", "B"),
                q("飞行原理", "飞机升力主要由什么产生？", "机翼上下表面的压强差", "发动机重量", "地面摩擦", "客舱压力", "A"),
                q("飞行原理", "飞机保持等速水平飞行时，升力与什么力基本平衡？", "推力", "阻力", "重力", "侧向力", "C"),
                q("飞行原理", "机翼前缘与后缘连线通常称为什么？", "翼展", "翼弦", "展弦比", "后掠角", "B"),
                q("飞行原理", "飞机转弯时产生向心力的主要来源是什么？", "升力的水平分量", "重力的水平分量", "发动机重量", "轮胎摩擦", "A"),
                q("飞行原理", "迎角超过临界值后，机翼通常会发生什么现象？", "超音速", "失速", "结冰", "倒飞", "B"),
                q("飞行原理", "飞机减速着陆时放下襟翼的主要目的是什么？", "减小升力", "增加升力和阻力", "减小机翼面积", "关闭发动机", "B"),
                q("航空法规", "进入机场控制区通常需要什么？", "有效通行证件", "任意车票", "购物凭证", "无需证件", "A"),
                q("航空法规", "旅客随身携带充电宝乘机时，通常应放在哪里？", "托运行李", "随身行李", "货舱散装", "机翼内", "B"),
                q("航空法规", "发现无人认领的可疑行李时，正确做法是什么？", "自行打开", "带离现场", "报告工作人员并远离", "交给其他旅客", "C"),
                q("航空法规", "飞行过程中是否可以擅自开启应急出口？", "可以", "经邻座同意可以", "不可以", "夜间可以", "C"),
                q("航空法规", "乘机人应在何时遵守安全带指示？", "仅起飞时", "仅降落时", "指示灯亮起及机组要求时", "从不需要", "C"),
                q("航空法规", "干扰机组人员正常履行职责属于什么行为？", "正常交流", "危害航空安全的行为", "优先服务请求", "登机手续", "B"),
                q("民航史", "《国际民用航空公约》通常称为什么公约？", "巴黎公约", "芝加哥公约", "东京公约", "华沙公约", "B"),
                q("民航史", "芝加哥公约签署于哪一年？", "1919年", "1944年", "1947年", "1958年", "B"),
                q("民航史", "ICAO正式成立于哪一年？", "1939年", "1944年", "1947年", "1950年", "C"),
                q("民航史", "世界上第一架成功持续受控飞行的动力飞机首飞地位于哪个国家？", "美国", "法国", "英国", "德国", "A"),
                q("民航史", "中国航空先驱冯如制造的飞机主要在哪个国家完成早期试飞？", "中国", "美国", "法国", "英国", "B"),
                q("民航史", "早期大型远程客运中曾广泛使用的齐柏林飞艇属于哪类航空器？", "旋翼航空器", "轻于空气航空器", "滑翔机", "水上飞机", "B"),
                q("民航史", "喷气式客机相较活塞式客机推动民航发展的主要优势之一是什么？", "巡航速度更高", "无需跑道", "不消耗燃料", "可以垂直起降", "A"),
                q("民航史", "协和式客机最具代表性的技术特征是什么？", "太阳能动力", "超音速巡航", "无人驾驶", "旋翼推进", "B"),
                q("民航史", "波音747因其机身外形和载客量曾被广泛称为什么？", "空中女王", "空中巴士", "飞行堡垒", "云中快车", "A"),
                q("民航史", "空中客车A380属于哪一类民用飞机？", "窄体支线客机", "双层宽体客机", "单座教练机", "超音速公务机", "B"),
                q("民航史", "国际航空运输协会的英文缩写是什么？", "ICAO", "IATA", "FAA", "EASA", "B"),
                q("民航史", "民航运输从纸质客票广泛转向电子客票主要体现了哪项发展？", "信息化", "飞艇化", "军用化", "低速化", "A"),
                q("民航史", "现代枢纽机场通过中转衔接大量航线的运营方式通常称为什么？", "点对点模式", "枢纽辐射模式", "目视飞行模式", "通用航空模式", "B"),
                q("民航史", "中国民航局英文简称通常写作什么？", "CAAC", "CAA", "ICAO", "IATA", "A"),
                q("飞行原理", "与飞机前进方向相反的空气动力称为什么？", "升力", "推力", "阻力", "重力", "C"),
                q("飞行原理", "发动机或螺旋桨推动飞机向前的力称为什么？", "升力", "推力", "阻力", "侧力", "B"),
                q("飞行原理", "翼展与平均翼弦之比相关的机翼参数是什么？", "展弦比", "迎角", "后掠角", "安装角", "A"),
                q("飞行原理", "空气流速增加时静压通常降低，这一关系常用什么原理解释？", "牛顿第一定律", "伯努利原理", "欧姆定律", "帕斯卡定律", "B"),
                q("飞行原理", "飞机绕横轴转动称为什么运动？", "滚转", "俯仰", "偏航", "平移", "B"),
                q("飞行原理", "飞机绕纵轴转动称为什么运动？", "滚转", "俯仰", "偏航", "爬升", "A"),
                q("飞行原理", "飞机绕垂直轴转动称为什么运动？", "滚转", "俯仰", "偏航", "下滑", "C"),
                q("飞行原理", "副翼主要控制飞机的哪种运动？", "俯仰", "滚转", "偏航", "速度", "B"),
                q("飞行原理", "方向舵主要控制飞机的哪种运动？", "偏航", "滚转", "俯仰", "升降", "A"),
                q("飞行原理", "升降舵主要控制飞机的哪种运动？", "滚转", "偏航", "俯仰", "刹车", "C"),
                q("飞行原理", "飞机起飞时使用襟翼的主要作用是什么？", "增加低速升力", "减小机翼面积", "关闭气流", "降低发动机推力", "A"),
                q("飞行原理", "空速相同条件下，空气密度降低通常会使升力怎样变化？", "增大", "减小", "不变", "变为零", "B"),
                q("飞行原理", "飞机稳定水平飞行时，推力通常与什么力平衡？", "重力", "升力", "阻力", "浮力", "C"),
                q("飞行原理", "机翼结冰对飞行性能最典型的影响是什么？", "升力减小且阻力增大", "升力增大且阻力减小", "重量减小", "油耗归零", "A"),
                q("航空法规", "超过100Wh但不超过160Wh的备用锂电池通常需要经过谁批准？", "机场商店", "航空公司", "同行旅客", "出租车司机", "B"),
                q("航空法规", "备用锂电池乘机时应如何防止短路？", "暴露电极", "做好单独绝缘保护", "放入水中", "连接充电器", "B"),
                q("航空法规", "旅客在机上使用便携式电子设备时应首先遵守谁的要求？", "机组人员", "其他旅客", "网络平台", "机场商户", "A"),
                q("航空法规", "托运行李中通常是否允许放置充电宝？", "允许", "仅国际航班允许", "禁止", "仅夜间允许", "C"),
                q("航空法规", "携带危险品乘机时隐瞒不报可能造成什么后果？", "获得优先登机", "危害航空运输安全", "减少安检时间", "自动升级舱位", "B"),
                q("航空法规", "乘坐民航班机时，旅客是否应接受安全检查？", "应当接受", "可以拒绝", "仅儿童接受", "仅机组接受", "A"),
                q("航空法规", "在机场控制区内使用通行证件应遵循什么原则？", "转借他人", "本人按授权区域使用", "任意区域通行", "过期后继续使用", "B"),
                q("航空法规", "飞机滑行和起降阶段，旅客小桌板通常应处于什么状态？", "打开", "收起并固定", "拆除", "放置行李", "B"),
                q("航空法规", "紧急情况下旅客应优先听从谁的指令？", "机组人员", "网络主播", "亲友电话", "其他乘客", "A"),
                q("航空法规", "客舱安全演示的主要目的是什么？", "介绍餐食", "说明安全设备和应急程序", "推销商品", "安排座位升级", "B"),
                q("航空法规", "在禁止吸烟的民用航空器内吸烟属于什么行为？", "允许行为", "违反客舱安全规定的行为", "奖励行为", "机组专属行为", "B"),
                q("航空法规", "旅客发现行李内含不确定危险物品时应如何处理？", "主动向航空公司或安检人员申报", "隐瞒携带", "交给陌生人", "丢在候机区", "A"),
                q("航空法规", "未经允许进入跑道或滑行道可能导致什么风险？", "提高航班效率", "跑道侵入和航空器冲突", "缩短安检", "增加座位", "B"),
                q("航空法规", "航空器舱门关闭后，旅客行李应放在哪里？", "阻塞过道", "规定的行李架或座椅下方", "应急出口前", "洗手间内", "B"));
    }

    private static QuestionSeed q(
            String category,
            String content,
            String a,
            String b,
            String c,
            String d,
            String answer) {
        return new QuestionSeed(category, content, List.of(a, b, c, d), answer);
    }

    public static void main(String[] args) {
        Path database = Path.of(args.length == 0 ? "data/aviation_quiz_v14.db" : args[0]);
        boolean created = seed(new Store(database), Clock.systemUTC());
        System.out.println(created ? "演示数据填充完成：" + database : "演示数据已经存在，未重复写入：" + database);
    }
}
