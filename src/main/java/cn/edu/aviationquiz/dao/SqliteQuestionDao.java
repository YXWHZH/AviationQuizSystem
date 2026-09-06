package cn.edu.aviationquiz.dao;

import cn.edu.aviationquiz.entity.Question;
import cn.edu.aviationquiz.entity.QuestionCategory;
import cn.edu.aviationquiz.entity.QuestionType;
import cn.edu.aviationquiz.exception.DataAccessException;
import cn.edu.aviationquiz.util.Database;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class SqliteQuestionDao implements QuestionDao {
    @Override public List<Question> findAll() {
        String sql = "SELECT q.id,c.code,q.type,q.content,q.correct_answer,q.default_score,q.active " +
                "FROM question q JOIN question_category c ON c.id=q.category_id ORDER BY q.id";
        try (Connection c = Database.connect(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            List<Question> result = new ArrayList<>();
            while (rs.next()) result.add(readQuestion(c, rs));
            return result;
        } catch (SQLException e) { throw new DataAccessException("读取题库失败", e); }
    }

    @Override public Question save(Question q) {
        String insert = "INSERT INTO question(category_id,type,content,correct_answer,default_score,active) " +
                "VALUES((SELECT id FROM question_category WHERE code=?),?,?,?,?,?)";
        try (Connection c = Database.connect()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
                bind(ps, q); ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("未生成题目编号");
                    long id = keys.getLong(1);
                    saveOptions(c, id, q.options());
                    c.commit();
                    return new Question(id, q.category(), q.type(), q.content(), q.options(), q.correctAnswer(), q.defaultScore(), q.active());
                }
            } catch (SQLException e) { c.rollback(); throw e; }
        } catch (SQLException e) { throw new DataAccessException("保存题目失败", e); }
    }

    private void bind(PreparedStatement ps, Question q) throws SQLException {
        ps.setString(1, q.category().name()); ps.setString(2, q.type().name()); ps.setString(3, q.content());
        ps.setString(4, q.correctAnswer()); ps.setInt(5, q.defaultScore()); ps.setBoolean(6, q.active());
    }
    private void saveOptions(Connection c, long id, List<String> options) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO question_option(question_id,option_key,option_text,display_order) VALUES(?,?,?,?)")) {
            for (int i=0;i<options.size();i++) { ps.setLong(1,id); ps.setString(2,String.valueOf((char)('A'+i))); ps.setString(3,options.get(i)); ps.setInt(4,i); ps.addBatch(); }
            ps.executeBatch();
        }
    }
    private Question readQuestion(Connection c, ResultSet rs) throws SQLException {
        long id=rs.getLong("id"); List<String> options=new ArrayList<>();
        try (PreparedStatement ps=c.prepareStatement("SELECT option_text FROM question_option WHERE question_id=? ORDER BY display_order")) {
            ps.setLong(1,id); try(ResultSet or=ps.executeQuery()){while(or.next())options.add(or.getString(1));}
        }
        return new Question(id, QuestionCategory.valueOf(rs.getString("code")), QuestionType.valueOf(rs.getString("type")),
                rs.getString("content"), options, rs.getString("correct_answer"), rs.getInt("default_score"), rs.getBoolean("active"));
    }
    @Override public void delete(long id) {
        try(Connection c=Database.connect(); PreparedStatement ps=c.prepareStatement("DELETE FROM question WHERE id=?")){ps.setLong(1,id);ps.executeUpdate();}
        catch(SQLException e){throw new DataAccessException("删除题目失败",e);}
    }
    @Override public long count() {
        try(Connection c=Database.connect(); Statement s=c.createStatement(); ResultSet rs=s.executeQuery("SELECT COUNT(*) FROM question")){return rs.next()?rs.getLong(1):0;}
        catch(SQLException e){throw new DataAccessException("统计题目失败",e);}
    }
}
