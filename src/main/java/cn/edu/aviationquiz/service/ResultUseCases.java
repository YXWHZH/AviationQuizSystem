package cn.edu.aviationquiz.service;

import cn.edu.aviationquiz.entity.Models.RankingEntry;
import cn.edu.aviationquiz.entity.Models.Session;

import java.util.List;

/** Authenticated result operations exposed to the result controller. */
public interface ResultUseCases {
    List<RankingEntry> ranking(Session session, String competitionId);

    List<RankingEntry> preview(Session session, String competitionId);

    void archive(Session session, String competitionId);

    String exportCsv(Session session, String competitionId);
}
