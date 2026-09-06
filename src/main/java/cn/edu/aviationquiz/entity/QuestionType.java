package cn.edu.aviationquiz.entity;

public enum QuestionType {
    SINGLE_CHOICE("单选题"), TRUE_FALSE("判断题");
    private final String displayName;
    QuestionType(String displayName) { this.displayName = displayName; }
    @Override public String toString() { return displayName; }
}
