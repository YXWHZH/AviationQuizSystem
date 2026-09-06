package cn.edu.aviationquiz.dao;

import cn.edu.aviationquiz.entity.Question;
import java.util.List;

public interface QuestionDao {
    List<Question> findAll();
    Question save(Question question);
    void delete(long id);
    long count();
}
