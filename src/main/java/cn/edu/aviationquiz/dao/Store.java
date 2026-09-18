package cn.edu.aviationquiz.dao;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** JDBC boundary. A UnitOfWork owns exactly one transaction connection. */
public final class Store {
    private final String url;

    public Store(Path file) {
        try {
            if (file.toAbsolutePath().getParent() != null)
                Files.createDirectories(file.toAbsolutePath().getParent());
            url = "jdbc:sqlite:" + file.toAbsolutePath();
            transaction(
                    db -> {
                        long version = db.one("PRAGMA user_version").number("user_version");
                        if (version == 0) {
                            try (var stream =
                                    Store.class.getResourceAsStream("/database/v16.sql")) {
                                if (stream == null) throw new IllegalStateException("缺少数据库脚本");
                                for (String sql :
                                        new String(stream.readAllBytes(), StandardCharsets.UTF_8)
                                                .split(";")) if (!sql.isBlank()) db.execute(sql);
                            }
                            try (var stream =
                                    Store.class.getResourceAsStream(
                                            "/database/v16-integrity.sql")) {
                                if (stream == null) throw new IllegalStateException("缺少完整性约束脚本");
                                for (String sql :
                                        new String(stream.readAllBytes(), StandardCharsets.UTF_8)
                                                .split("(?m)^-- statement\\s*$"))
                                    if (!sql.isBlank()) db.execute(sql);
                            }
                        } else if (version == 14) {
                            db.command("ALTER TABLE player ADD COLUMN school TEXT NOT NULL DEFAULT ''");
                            db.command("ALTER TABLE player ADD COLUMN college TEXT NOT NULL DEFAULT ''");
                            db.command("ALTER TABLE player ADD COLUMN major TEXT NOT NULL DEFAULT ''");
                            db.command("ALTER TABLE player ADD COLUMN student_number TEXT NOT NULL DEFAULT ''");
                            db.command("ALTER TABLE player ADD COLUMN profile_complete INTEGER NOT NULL DEFAULT 0 CHECK(profile_complete IN (0,1))");
                            db.command("UPDATE player SET school='中国民航大学',college=CASE WHEN username IN ('player01','player02','player03') THEN '航空工程学院' ELSE '空中交通管理学院' END,major=CASE WHEN username IN ('player01','player02','player03') THEN '飞行器动力工程' ELSE '交通运输' END,student_number='202600'||substr(username,-1),profile_complete=1 WHERE id LIKE 'DEMO_%'");
                            db.command("CREATE UNIQUE INDEX player_school_student ON player(school,student_number) WHERE profile_complete=1");
                            db.command("CREATE TABLE competition_category(competition_id TEXT NOT NULL REFERENCES competition(id) ON DELETE CASCADE,category TEXT NOT NULL CHECK(category IN ('民航史','飞行原理','航空法规')),PRIMARY KEY(competition_id,category))");
                            db.command("INSERT INTO competition_category SELECT id,'民航史' FROM competition UNION ALL SELECT id,'飞行原理' FROM competition UNION ALL SELECT id,'航空法规' FROM competition");
                            db.command("PRAGMA user_version=16");
                        } else if (version == 15) {
                            db.command("CREATE TABLE competition_category(competition_id TEXT NOT NULL REFERENCES competition(id) ON DELETE CASCADE,category TEXT NOT NULL CHECK(category IN ('民航史','飞行原理','航空法规')),PRIMARY KEY(competition_id,category))");
                            db.command("INSERT INTO competition_category SELECT id,'民航史' FROM competition UNION ALL SELECT id,'飞行原理' FROM competition UNION ALL SELECT id,'航空法规' FROM competition");
                            db.command("PRAGMA user_version=16");
                        } else if (version != 16)
                            throw new IllegalStateException("数据库版本不兼容：" + version);
                        return null;
                    });
        } catch (Exception ex) {
            throw failure(ex);
        }
    }

    public interface Work<T> {
        T run(UnitOfWork db) throws Exception;
    }

    public synchronized <T> T transaction(Work<T> work) {
        try (Connection c = DriverManager.getConnection(url)) {
            try (Statement s = c.createStatement()) {
                s.execute("PRAGMA foreign_keys=ON");
                s.execute("PRAGMA busy_timeout=5000");
            }
            c.setAutoCommit(false);
            try {
                T value = work.run(new UnitOfWork(c));
                c.commit();
                return value;
            } catch (Exception ex) {
                c.rollback();
                throw failure(ex);
            }
        } catch (SQLException ex) {
            throw failure(ex);
        }
    }

    private static RuntimeException failure(Exception ex) {
        if (ex instanceof RuntimeException runtime) return runtime;
        if (ex instanceof SQLException sql && sql.getErrorCode() == 19)
            return new IllegalStateException("无法保存：存在重复数据、无效关联或状态冲突，请检查后重试", ex);
        return new IllegalStateException("数据库操作失败，请检查文件访问权限和磁盘空间后重试", ex);
    }

    public record Row(Map<String, Object> values) {
        public String text(String key) {
            Object v = values.get(key);
            return v == null ? "" : v.toString();
        }

        public long number(String key) {
            Object v = values.get(key);
            return v == null ? 0 : ((Number) v).longValue();
        }

        public boolean nil(String key) {
            return values.get(key) == null;
        }

        @Override
        public String toString() {
            return text("name").isEmpty() ? text("content") : text("name");
        }
    }

    public static final class UnitOfWork {
        private final Connection connection;

        private UnitOfWork(Connection connection) {
            this.connection = connection;
        }

        public int execute(String sql, Object... args) throws SQLException {
            try (PreparedStatement s = prepare(sql, args)) {
                return s.executeUpdate();
            }
        }

        public void command(String sql) throws SQLException {
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }
        }

        public List<Row> list(String sql, Object... args) throws SQLException {
            try (PreparedStatement s = prepare(sql, args);
                    ResultSet rs = s.executeQuery()) {
                List<Row> rows = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> values = new LinkedHashMap<>();
                    for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++)
                        values.put(rs.getMetaData().getColumnLabel(i), rs.getObject(i));
                    rows.add(new Row(Collections.unmodifiableMap(values)));
                }
                return List.copyOf(rows);
            }
        }

        public Row one(String sql, Object... args) throws SQLException {
            var rows = list(sql, args);
            if (rows.isEmpty()) throw new IllegalArgumentException("记录不存在或状态已变化，请刷新");
            return rows.getFirst();
        }

        public boolean exists(String sql, Object... args) throws SQLException {
            return !list(sql, args).isEmpty();
        }

        private PreparedStatement prepare(String sql, Object[] args) throws SQLException {
            PreparedStatement s = connection.prepareStatement(sql);
            for (int i = 0; i < args.length; i++) s.setObject(i + 1, args[i]);
            return s;
        }
    }
}
