package cn.edu.aviationquiz.service;

import cn.edu.aviationquiz.entity.Models.RankingEntry;

import java.util.List;

/** Ranking, promotion preview and atomic result archival. */
public interface ResultService {
    List<RankingEntry> ranking(String competitionId);

    List<RankingEntry> preview(String competitionId);

    void archive(String competitionId);
}
