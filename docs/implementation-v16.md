# V1.6 实现与验收对照

系统采用 JavaFX 单窗口页面切换、SQLite 持久化、个人计分和分组串行比赛。全系统同一时刻只运行一场竞赛及一次题目发布，不实现多终端网络同步。

## 分层与数据模型

- View 负责控件、页面切换和 Timeline 倒计时显示，不决定提交是否超时。
- Controller 检查页面输入，通过用例接口发起操作，不访问数据库。
- QuizService 负责会话与角色权限，并把账号资料、竞赛配置、题库题单、现场运行、报名分组、答题计分、排名归档委托给独立领域服务。
- 领域服务依赖 AccountDao、CompetitionDao、QuestionBankDao、CompetitionLiveDao、GameDao、RegistrationDao、ResultDao 接口；JDBC 实现负责 SQLite 映射与事务。
- AppContext 集中完成依赖组装，View 和业务规则均不直接创建 JDBC DAO。
- 现场运行控制已完成 CompetitionLiveController、CompetitionLiveService 和 CompetitionLiveDao 分层；竞赛大厅、参与状态、参赛人员、小组和历史成绩均使用强类型 View。QuizService 已移除 Store 依赖，所有业务 SQL 均位于 JDBC DAO。
- `competition_category` 保存竞赛多分类；`round_question` 保存题单；`group_round` 保存小组轮次状态；`question_release` 保存一次发布的开始、截止和关闭时间。
- `answer_record` 与 `timeout_record` 分别保存有效答案和超时结算，Service 事务与双向触发器共同保证二者互斥。

## 关键规则

- 预约和报名记录使用“有效/已取消”。恢复操作更新原记录，不插入第二条记录；尚无分组记录时界面显示“待分组”。
- 预约只能在未开放阶段取消；报名只能在报名时间内且尚未分组时取消。正式报名会关闭已有有效预约。
- 同一选手对同一次 QuestionRelease 最多提交一次。Service 按持久化 deadline 判断超时，Timeline 只负责显示。
- 排名按总分、答对数、累计有效答题用时和选手编号排序；累计用时为每条有效答案的 `submitted_at - started_at` 之和。
- 晋级预览仅工作人员可见；归档 Result 后选手才在历史成绩中看到最终结果。
- 被题单引用的题目不得物理删除，只能停用；未引用题目允许删除。

## 数据与兼容

新数据库由 `database/v16.sql` 和 `database/v16-integrity.sql` 初始化，逻辑版本为 16。已有版本 14、15 数据库由 Store 升级到版本 16；既有版本 16 数据库无需迁移。

演示填充器幂等生成 30 名选手、60 道题、12 场竞赛、24 条竞赛分类关系，以及覆盖预约、报名、分组、题单和历史结果的数据。

## 验收

```powershell
./tools/apache-maven-3.9.11/bin/mvn.cmd clean test package
./tools/apache-maven-3.9.11/bin/mvn.cmd '-Dquiz.uiTest=true' test
```

业务测试覆盖取消与恢复、唯一记录、报名时间、分组锁定、分类限制、题目引用、题目发布、提交与超时互斥、累计用时、稳定排名、晋级、历史查询和归档导出。接口替身测试验证 Controller 委托、强类型查询、DAO 隔离和可插拔计分工厂。JavaFX 测试验证单窗口页面切换、分组题目隔离和提交反馈。当前完整测试共 60 项。类结构、执行过程和扩展性证据见 [核心功能分层设计](layered-design.md)。

## 整改进度（2026-09-30）

| 检查项 | 完成度 | 当前证据 |
|---|---:|---|
| 分层结构 | 100% | 七个核心闭环均有 Controller、Service 接口/实现和 DAO 接口/实现，门面无 SQL |
| 核心功能 | 95% | 账号、竞赛、报名分组、题库题单、现场答题、排名归档均可运行 |
| 继承、多态与接口扩展 | 90% | 三种轮次继承 `CompetitionRound`，`RoundFactory` 可注册新规则 |
| 界面与数据一致性 | 82% | 主要页面已使用强类型 View；仍需最终人工逐页验收 |
| 自动化测试 | 88% | 60 项测试通过，包含 JavaFX 主流程、历史查询和安全导出 |
| 报告与答辩材料 | 55% | 已有类结构和七个执行过程，尚需套用学校 Word 模板并完成最终截图 |

按以上项目加权估算，当前总体完成度约 **86%**。下一阶段优先完成最终界面巡检、课程设计报告、系统截图和答辩清单。
