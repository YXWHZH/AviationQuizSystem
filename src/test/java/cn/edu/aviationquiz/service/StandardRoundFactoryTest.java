package cn.edu.aviationquiz.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cn.edu.aviationquiz.entity.CompetitionRound;
import cn.edu.aviationquiz.exception.BusinessException;
import cn.edu.aviationquiz.service.impl.StandardRoundFactory;

import org.junit.jupiter.api.Test;

import java.util.Map;

class StandardRoundFactoryTest {
    @Test
    void suppliesTheThreeStandardPolymorphicScoringRules() {
        RoundFactory factory = new StandardRoundFactory();

        assertEquals(10, factory.create("REQUIRED").calculateScore(true));
        assertEquals(-10, factory.create("BUZZER").calculateScore(false));
        assertEquals(20, factory.create("RISK").calculateScore(true));
        assertThrows(BusinessException.class, () -> factory.create("UNKNOWN"));
    }

    @Test
    void acceptsANewRuleThroughTheFactoryRegistry() {
        RoundFactory factory =
                new StandardRoundFactory(
                        Map.of(
                                "BONUS",
                                () ->
                                        new CompetitionRound() {
                                            @Override
                                            public int calculateScore(boolean correct) {
                                                return correct ? 30 : 0;
                                            }
                                        }));

        assertEquals(30, factory.create("BONUS").calculateScore(true));
    }
}
