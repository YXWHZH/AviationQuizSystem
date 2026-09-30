package cn.edu.aviationquiz.service.impl;

import cn.edu.aviationquiz.dao.QuestionBankDao;
import cn.edu.aviationquiz.entity.Models.QuestionInput;
import cn.edu.aviationquiz.entity.Models.QuestionView;
import cn.edu.aviationquiz.entity.Models.RoundQuestionView;
import cn.edu.aviationquiz.entity.Models.RoundView;
import cn.edu.aviationquiz.exception.BusinessException;
import cn.edu.aviationquiz.service.QuestionBankService;
import cn.edu.aviationquiz.service.RoundFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Default question-bank and round-list rules, independent of JavaFX and JDBC. */
public final class QuestionBankServiceImpl implements QuestionBankService {
    private static final List<String> CATEGORIES =
            List.of("民航史", "飞行原理", "航空法规");
    private final QuestionBankDao questionBankDao;
    private final RoundFactory roundFactory;

    public QuestionBankServiceImpl(
            QuestionBankDao questionBankDao, RoundFactory roundFactory) {
        this.questionBankDao = questionBankDao;
        this.roundFactory = roundFactory;
    }

    @Override
    public List<QuestionView> questions() {
        return questionBankDao.inTransaction(QuestionBankDao.Transaction::listQuestions);
    }

    @Override
    public List<QuestionView> questionsForCompetition(String competitionId) {
        return questionBankDao.inTransaction(
                db -> db.listQuestionsForCompetition(competitionId));
    }

    @Override
    public List<RoundView> rounds(String competitionId) {
        return questionBankDao.inTransaction(db -> db.listRounds(competitionId));
    }

    @Override
    public List<RoundQuestionView> roundQuestions(String roundId) {
        return questionBankDao.inTransaction(db -> db.listRoundQuestions(roundId));
    }

    @Override
    public String saveQuestion(String existingId, QuestionInput input) {
        return questionBankDao.inTransaction(
                db -> {
                    QuestionInput validInput = validQuestion(input);
                    String questionId = existingId == null ? id("Q") : existingId;
                    if (existingId == null) db.insertQuestion(questionId, validInput);
                    else {
                        require(
                                !db.isQuestionReferenced(questionId),
                                "引用中的题目不可修改，请复制为新题");
                        db.updateQuestion(questionId, validInput);
                    }
                    return questionId;
                });
    }

    @Override
    public void setQuestionActive(String questionId, boolean active) {
        questionBankDao.inTransaction(
                db -> {
                    db.setQuestionActive(questionId, active);
                    return null;
                });
    }

    @Override
    public void deleteQuestion(String questionId) {
        questionBankDao.inTransaction(
                db -> {
                    require(
                            !db.isQuestionReferenced(questionId),
                            "引用中的题目不能删除");
                    db.deleteQuestion(questionId);
                    return null;
                });
    }

    @Override
    public String addRound(
            String competitionId, String name, String type, int sequence, int seconds) {
        return questionBankDao.inTransaction(
                db -> {
                    requireEditable(db.findCompetitionStatus(competitionId));
                    validateRound(type, sequence, seconds);
                    String roundId = id("RD");
                    db.insertRound(
                            roundId,
                            competitionId,
                            bounded(name, 1, 50, "轮次名称"),
                            type,
                            sequence,
                            seconds);
                    return roundId;
                });
    }

    @Override
    public void updateRound(
            String roundId, String name, String type, int sequence, int seconds) {
        questionBankDao.inTransaction(
                db -> {
                    String competitionId = db.findRoundCompetitionId(roundId);
                    requireEditable(db.findCompetitionStatus(competitionId));
                    validateRound(type, sequence, seconds);
                    db.updateRound(
                            roundId,
                            bounded(name, 1, 50, "轮次名称"),
                            type,
                            sequence,
                            seconds);
                    return null;
                });
    }

    @Override
    public void deleteEmptyRound(String roundId) {
        questionBankDao.inTransaction(
                db -> {
                    String competitionId = db.findRoundCompetitionId(roundId);
                    requireEditable(db.findCompetitionStatus(competitionId));
                    require(db.isRoundEmpty(roundId), "请先移除该轮次题单，再删除空轮次");
                    db.deleteRound(roundId);
                    return null;
                });
    }

    @Override
    public void addQuestionToRound(String roundId, String questionId, int sequence) {
        questionBankDao.inTransaction(
                db -> {
                    String competitionId = db.findRoundCompetitionId(roundId);
                    requireEditable(db.findCompetitionStatus(competitionId));
                    require(sequence > 0, "题序必须大于 0");
                    require(db.isQuestionActive(questionId), "请选择启用的题目");
                    require(
                            db.isQuestionAllowedForCompetition(
                                    questionId, competitionId),
                            "题目分类不属于该竞赛");
                    require(
                            !db.isQuestionUsedInCompetition(
                                    questionId, competitionId),
                            "同场竞赛不能重复用题");
                    db.insertRoundQuestion(id("RQ"), roundId, questionId, sequence);
                    return null;
                });
    }

    @Override
    public void removeRoundQuestion(String roundQuestionId) {
        questionBankDao.inTransaction(
                db -> {
                    String competitionId =
                            db.findRoundQuestionCompetitionId(roundQuestionId);
                    requireEditable(db.findCompetitionStatus(competitionId));
                    db.deleteRoundQuestion(roundQuestionId);
                    return null;
                });
    }

    private QuestionInput validQuestion(QuestionInput input) {
        require(input != null, "请填写题目信息");
        String content = bounded(input.content(), 1, 200, "题干");
        require(CATEGORIES.contains(input.category()), "请选择知识分类");
        require(input.options() != null && input.options().size() == 4, "必须有四个选项");
        List<String> options = new ArrayList<>(4);
        for (String option : input.options())
            options.add(bounded(option, 1, 1000, "选项"));
        require(List.of("A", "B", "C", "D").contains(input.answer()), "标准答案必须为 A/B/C/D");
        return new QuestionInput(
                content,
                input.category(),
                List.copyOf(options),
                input.answer(),
                input.active());
    }

    private void validateRound(String type, int sequence, int seconds) {
        roundFactory.create(type);
        require(sequence > 0 && seconds > 0, "顺序和时限必须大于 0");
    }

    private static void requireEditable(String status) {
        require(!List.of("比赛中", "已结束").contains(status), "开赛后配置已锁定");
    }

    private static String bounded(String text, int min, int max, String label) {
        String value = text == null ? "" : text.trim();
        require(
                value.length() >= min && value.length() <= max,
                label + "长度须为 " + min + "～" + max);
        return value;
    }

    private static void require(boolean valid, String message) {
        if (!valid) throw new BusinessException(message);
    }

    private static String id(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "");
    }
}
