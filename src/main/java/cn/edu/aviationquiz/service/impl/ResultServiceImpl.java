package cn.edu.aviationquiz.service.impl;

import cn.edu.aviationquiz.dao.ResultDao;
import cn.edu.aviationquiz.entity.CompetitionResultContext;
import cn.edu.aviationquiz.entity.Models.RankingEntry;
import cn.edu.aviationquiz.entity.Models.HistoryView;
import cn.edu.aviationquiz.entity.RankingSnapshot;
import cn.edu.aviationquiz.exception.BusinessException;
import cn.edu.aviationquiz.service.ResultService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Collection-based ranking and archival rules, independent of JavaFX and JDBC. */
public final class ResultServiceImpl implements ResultService {
    private final ResultDao resultDao;

    public ResultServiceImpl(ResultDao resultDao) {
        this.resultDao = resultDao;
    }

    @Override
    public List<HistoryView> history(String playerId, boolean staff) {
        return resultDao.inTransaction(
                db -> staff ? db.listStaffHistory() : db.listPlayerHistory(playerId));
    }

    @Override
    public List<RankingEntry> ranking(String competitionId) {
        return resultDao.inTransaction(
                db -> calculate(db, db.findCompetition(competitionId), false));
    }

    @Override
    public List<RankingEntry> preview(String competitionId) {
        return resultDao.inTransaction(
                db -> {
                    CompetitionResultContext competition = db.findCompetition(competitionId);
                    require(competition.status().equals("比赛中"), "竞赛不在比赛中");
                    require(
                            !db.hasUnfinishedRounds(competitionId),
                            "所有小组完成全部轮次后才能判定晋级");
                    return calculate(db, competition, true);
                });
    }

    @Override
    public void archive(String competitionId) {
        resultDao.inTransaction(
                db -> {
                    CompetitionResultContext competition = db.findCompetition(competitionId);
                    if (competition.status().equals("已结束")) return null;
                    require(competition.status().equals("比赛中"), "竞赛不在比赛中");
                    require(!db.hasUnfinishedRounds(competitionId), "所有轮次尚未完成");
                    for (RankingEntry entry : calculate(db, competition, true))
                        db.insertResult(
                                id(),
                                competitionId,
                                entry.playerId(),
                                entry.score(),
                                entry.rank(),
                                entry.rank() <= competition.advanceCount());
                    db.markCompetitionArchived(competitionId);
                    return null;
                });
    }

    @Override
    public String exportCsv(String competitionId) {
        return resultDao.inTransaction(
                db -> {
                    CompetitionResultContext competition = db.findCompetition(competitionId);
                    require(competition.status().equals("已结束"), "只能导出已归档成绩");
                    StringBuilder csv =
                            new StringBuilder(
                                    "\uFEFF名次,选手编号,姓名,小组,最终成绩,晋级结果\r\n");
                    for (RankingEntry entry : calculate(db, competition, false))
                        csv.append(entry.rank())
                                .append(',')
                                .append(cell(entry.playerId()))
                                .append(',')
                                .append(cell(entry.name()))
                                .append(',')
                                .append(cell(entry.group()))
                                .append(',')
                                .append(entry.score())
                                .append(',')
                                .append(cell(entry.promotion()))
                                .append("\r\n");
                    return csv.toString();
                });
    }

    private static List<RankingEntry> calculate(
            ResultDao.Transaction db,
            CompetitionResultContext competition,
            boolean preview)
            throws Exception {
        List<RankingSnapshot> sorted =
                new ArrayList<>(db.listRankingSnapshots(competition.id()));
        boolean archived = competition.status().equals("已结束");
        sorted.sort(
                archived
                        ? Comparator.comparingInt(RankingSnapshot::archivedRank)
                        : Comparator.comparingInt(RankingSnapshot::totalScore)
                                .reversed()
                                .thenComparing(
                                        Comparator.comparingInt(RankingSnapshot::correctCount)
                                                .reversed())
                                .thenComparingLong(RankingSnapshot::elapsedMillis)
                                .thenComparing(RankingSnapshot::playerId));
        List<RankingEntry> result = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            RankingSnapshot row = sorted.get(i);
            int rank = i + 1;
            String promotion =
                    archived
                            ? (row.promoted() ? "晋级" : "未晋级")
                            : preview
                                    ? (rank <= competition.advanceCount()
                                            ? "晋级（预览）"
                                            : "未晋级（预览）")
                                    : "待判定";
            result.add(
                    new RankingEntry(
                            row.playerId(),
                            row.name(),
                            row.groupName(),
                            archived ? row.finalScore() : row.totalScore(),
                            row.correctCount(),
                            row.elapsedMillis(),
                            rank,
                            promotion));
        }
        return List.copyOf(result);
    }

    private static void require(boolean valid, String message) {
        if (!valid) throw new BusinessException(message);
    }

    private static String cell(String value) {
        if (!value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0) value = "'" + value;
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private static String id() {
        return "RS" + UUID.randomUUID().toString().replace("-", "");
    }
}
