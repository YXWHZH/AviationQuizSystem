package cn.edu.aviationquiz.service.impl;

import cn.edu.aviationquiz.entity.BuzzerRound;
import cn.edu.aviationquiz.entity.CompetitionRound;
import cn.edu.aviationquiz.entity.RequiredRound;
import cn.edu.aviationquiz.entity.RiskRound;
import cn.edu.aviationquiz.exception.BusinessException;
import cn.edu.aviationquiz.service.RoundFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Registry-based factory. New round rules can be injected without changing competition services. */
public final class StandardRoundFactory implements RoundFactory {
    private final Map<String, Supplier<? extends CompetitionRound>> creators;

    public StandardRoundFactory() {
        this(defaultCreators());
    }

    public StandardRoundFactory(
            Map<String, Supplier<? extends CompetitionRound>> creators) {
        this.creators = Map.copyOf(creators);
    }

    @Override
    public CompetitionRound create(String roundType) {
        Supplier<? extends CompetitionRound> creator = creators.get(roundType);
        if (creator == null) throw new BusinessException("无效轮次类型");
        return creator.get();
    }

    private static Map<String, Supplier<? extends CompetitionRound>> defaultCreators() {
        Map<String, Supplier<? extends CompetitionRound>> creators = new HashMap<>();
        creators.put("REQUIRED", RequiredRound::new);
        creators.put("BUZZER", BuzzerRound::new);
        creators.put("RISK", RiskRound::new);
        return creators;
    }
}
