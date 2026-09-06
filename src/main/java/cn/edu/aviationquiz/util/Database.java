package cn.edu.aviationquiz.util;

import cn.edu.aviationquiz.exception.DataAccessException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Database {
    private static final Path DATA_DIR = Path.of("data");
    private static final String URL = "jdbc:sqlite:" + DATA_DIR.resolve("aviation_quiz.db");
    private Database() {}

    public static Connection connect() throws SQLException {
        Connection connection = DriverManager.getConnection(URL);
        connection.createStatement().execute("PRAGMA foreign_keys = ON");
        return connection;
    }

    public static void initialize() {
        try {
            Files.createDirectories(DATA_DIR);
            try (InputStream input = Database.class.getResourceAsStream("/database/schema.sql")) {
                if (input == null) throw new IOException("找不到数据库脚本");
                String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                try (Connection connection = connect()) {
                    for (String statement : sql.split(";")) {
                        if (!statement.isBlank()) connection.createStatement().execute(statement);
                    }
                }
            }
        } catch (IOException | SQLException exception) {
            throw new DataAccessException("数据库初始化失败", exception);
        }
    }
}
