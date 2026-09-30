package cn.edu.aviationquiz.service;

/** Background recovery boundary used by the UI scheduler. */
@FunctionalInterface
public interface CompetitionRecovery {
    boolean recover();
}
