package cn.edu.aviationquiz.entity;

import java.util.List;
import java.util.Objects;

public record Question(long id, QuestionCategory category, QuestionType type, String content,
                       List<String> options, String correctAnswer, int defaultScore, boolean active) {
    public Question {
        Objects.requireNonNull(category, "题目分类不能为空");
        Objects.requireNonNull(type, "题目类型不能为空");
        if (content == null || content.isBlank()) throw new IllegalArgumentException("题干不能为空");
        options = List.copyOf(Objects.requireNonNull(options, "选项不能为空"));
        if (options.size() < 2) throw new IllegalArgumentException("题目至少需要两个选项");
        if (correctAnswer == null || correctAnswer.isBlank()) throw new IllegalArgumentException("正确答案不能为空");
        if (defaultScore <= 0) throw new IllegalArgumentException("分值必须大于 0");
        correctAnswer = correctAnswer.trim().toUpperCase();
    }

    public boolean isCorrect(String answer) {
        return answer != null && correctAnswer.equalsIgnoreCase(answer.trim());
    }
}
