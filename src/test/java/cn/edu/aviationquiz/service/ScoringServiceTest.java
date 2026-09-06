package cn.edu.aviationquiz.service;

import cn.edu.aviationquiz.entity.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ScoringServiceTest {
    private final Question question=new Question(1,QuestionCategory.FLIGHT_PRINCIPLE,QuestionType.SINGLE_CHOICE,"测试题",List.of("甲","乙"),"A",10,true);
    @Test void requiredRoundDoesNotDeductForWrongAnswer(){Team t=new Team(1,"甲队");assertEquals(0,new ScoringService().score(t,new RequiredRound("必答",30),question,"B",8));assertEquals(0,t.getScore());}
    @Test void buzzerRoundDeductsForWrongAnswer(){Team t=new Team(1,"甲队");assertEquals(-10,new ScoringService().score(t,new BuzzerRound("抢答",30),question,"B",8));}
    @Test void riskRoundAppliesMultiplier(){Team t=new Team(1,"甲队");assertEquals(20,new ScoringService().score(t,new RiskRound("风险",30,2),question,"A",8));}
    @Test void timeoutIsTreatedAsWrong(){Team t=new Team(1,"甲队");assertEquals(-10,new ScoringService().score(t,new BuzzerRound("抢答",30),question,"A",31));}
}
