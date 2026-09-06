package cn.edu.aviationquiz.service;

import cn.edu.aviationquiz.entity.Team;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RankingServiceTest {
    @Test void ranksByScoreThenCorrectCountThenTime(){
        Team a=new Team(1,"甲队");a.applyScore(20,true,15);a.applyScore(0,true,15);
        Team b=new Team(2,"乙队");b.applyScore(20,true,8);
        assertEquals(List.of(a,b),new RankingService().rank(List.of(b,a)));
    }
    @Test void selectsPromotionQuota(){Team a=new Team(1,"甲队"),b=new Team(2,"乙队");a.applyScore(10,true,5);assertEquals(List.of(a),new RankingService().promoted(List.of(b,a),1));}
}
