package cn.edu.aviationquiz.service;

/** Business operations performed while a competition is running. */
public interface CompetitionExecutionService {
    int submitAnswer(String playerId, String releaseId, String option);
}
