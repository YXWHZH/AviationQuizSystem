package cn.edu.aviationquiz.entity;

public abstract class CompetitionRound {
    public abstract int calculateScore(boolean correct);

    public static CompetitionRound of(String type) {
        return switch (type) {
            case "REQUIRED" -> new RequiredRound();
            case "BUZZER" -> new BuzzerRound();
            case "RISK" -> new RiskRound();
            default -> throw new IllegalArgumentException("无效轮次类型");
        };
    }
}
