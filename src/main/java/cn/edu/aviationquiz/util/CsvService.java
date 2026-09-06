package cn.edu.aviationquiz.util;

import cn.edu.aviationquiz.entity.Team;
import cn.edu.aviationquiz.service.RankingService;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class CsvService {
    public void exportRanking(Path path, List<Team> teams) throws IOException {
        try (BufferedWriter writer=Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write('\ufeff');
            writer.write("排名,队伍,得分,答对数,累计用时（秒）\n");
            List<Team> ranked=new RankingService().rank(teams);
            for(int i=0;i<ranked.size();i++){
                Team t=ranked.get(i);
                writer.write((i+1)+","+quote(t.getName())+","+t.getScore()+","+t.getCorrectAnswers()+","+t.getElapsedSeconds()+"\n");
            }
        }
    }
    private String quote(String value){return "\""+value.replace("\"","\"\"")+"\"";}
}
