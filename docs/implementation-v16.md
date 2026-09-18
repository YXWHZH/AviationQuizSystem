# V1.6 实现与验收对照

系统采用 JavaFX 单窗口页面切换、SQLite 持久化、个人计分和分组串行比赛。全系统同一时刻只运行一场竞赛及一次题目发布，不实现多终端网络同步。

## 分层与数据模型

- View 负责控件、页面切换和 Timeline 倒计时显示，不决定提交是否超时。
- QuizService 负责权限、状态、时间、取消与恢复、判题、计分、排名和事务边界。
- Store 负责 JDBC、参数绑定、事务以及数据库版本迁移。
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

业务测试覆盖取消与恢复、唯一记录、报名时间、分组锁定、分类限制、题目引用、题目发布、提交与超时互斥、累计用时、稳定排名、晋级和归档。JavaFX 测试验证单窗口页面切换、分组题目隔离和提交反馈。
