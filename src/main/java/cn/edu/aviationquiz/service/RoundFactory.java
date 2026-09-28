package cn.edu.aviationquiz.service;

import cn.edu.aviationquiz.entity.CompetitionRound;

/** Creates the polymorphic scoring rule for a persisted round type. */
public interface RoundFactory {
    CompetitionRound create(String roundType);
}
