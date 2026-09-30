package cn.edu.aviationquiz.service;

import cn.edu.aviationquiz.entity.Models.RankingEntry;
import cn.edu.aviationquiz.entity.Models.HistoryView;

import java.util.List;

/** Ranking, promotion preview and atomic result archival. */
public interface ResultService {
    List<HistoryView> history(String playerId, boolean staff);

    List<RankingEntry> ranking(String competitionId);

    List<RankingEntry> preview(String competitionId);

    void archive(String competitionId);

    String exportCsv(String competitionId);
}
