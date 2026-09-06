package cn.edu.aviationquiz.entity;

public enum RoundType { REQUIRED("必答题"), BUZZER("抢答题"), RISK("风险题");
    private final String displayName;
    RoundType(String displayName) { this.displayName = displayName; }
    @Override public String toString() { return displayName; }
}
