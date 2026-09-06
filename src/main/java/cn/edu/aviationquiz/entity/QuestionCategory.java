package cn.edu.aviationquiz.entity;

public enum QuestionCategory {
    CIVIL_AVIATION_HISTORY("民航史"),
    FLIGHT_PRINCIPLE("飞行原理"),
    AVIATION_REGULATION("航空法规");

    private final String displayName;
    QuestionCategory(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
    @Override public String toString() { return displayName; }
}
