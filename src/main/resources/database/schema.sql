CREATE TABLE IF NOT EXISTS question_category (
 id INTEGER PRIMARY KEY AUTOINCREMENT, code TEXT NOT NULL UNIQUE, name TEXT NOT NULL UNIQUE
);
INSERT OR IGNORE INTO question_category(code,name) VALUES
 ('CIVIL_AVIATION_HISTORY','民航史'),('FLIGHT_PRINCIPLE','飞行原理'),('AVIATION_REGULATION','航空法规');
CREATE TABLE IF NOT EXISTS question (
 id INTEGER PRIMARY KEY AUTOINCREMENT, category_id INTEGER NOT NULL, type TEXT NOT NULL,
 content TEXT NOT NULL, correct_answer TEXT NOT NULL, default_score INTEGER NOT NULL CHECK(default_score>0),
 active INTEGER NOT NULL DEFAULT 1, FOREIGN KEY(category_id) REFERENCES question_category(id)
);
CREATE TABLE IF NOT EXISTS question_option (
 id INTEGER PRIMARY KEY AUTOINCREMENT, question_id INTEGER NOT NULL, option_key TEXT NOT NULL,
 option_text TEXT NOT NULL, display_order INTEGER NOT NULL,
 UNIQUE(question_id,option_key), FOREIGN KEY(question_id) REFERENCES question(id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS competition (
 id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, competition_date TEXT NOT NULL,
 promotion_quota INTEGER NOT NULL CHECK(promotion_quota>0), status TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS player (
 id INTEGER PRIMARY KEY AUTOINCREMENT, student_number TEXT NOT NULL UNIQUE, name TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS team (
 id INTEGER PRIMARY KEY AUTOINCREMENT, competition_id INTEGER NOT NULL, name TEXT NOT NULL,
 UNIQUE(competition_id,name), FOREIGN KEY(competition_id) REFERENCES competition(id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS team_member (
 team_id INTEGER NOT NULL, player_id INTEGER NOT NULL, PRIMARY KEY(team_id,player_id),
 FOREIGN KEY(team_id) REFERENCES team(id) ON DELETE CASCADE, FOREIGN KEY(player_id) REFERENCES player(id)
);
CREATE TABLE IF NOT EXISTS competition_round (
 id INTEGER PRIMARY KEY AUTOINCREMENT, competition_id INTEGER NOT NULL, name TEXT NOT NULL,
 round_type TEXT NOT NULL, sequence_no INTEGER NOT NULL, time_limit_seconds INTEGER NOT NULL,
 risk_multiplier INTEGER, UNIQUE(competition_id,sequence_no),
 FOREIGN KEY(competition_id) REFERENCES competition(id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS round_question (
 round_id INTEGER NOT NULL, question_id INTEGER NOT NULL, sequence_no INTEGER NOT NULL,
 PRIMARY KEY(round_id,question_id), UNIQUE(round_id,sequence_no),
 FOREIGN KEY(round_id) REFERENCES competition_round(id), FOREIGN KEY(question_id) REFERENCES question(id)
);
CREATE TABLE IF NOT EXISTS answer_record (
 id INTEGER PRIMARY KEY AUTOINCREMENT, round_id INTEGER NOT NULL, question_id INTEGER NOT NULL,
 team_id INTEGER NOT NULL, submitted_answer TEXT, correct INTEGER NOT NULL, timeout INTEGER NOT NULL,
 score_change INTEGER NOT NULL, elapsed_seconds INTEGER NOT NULL, answered_at TEXT NOT NULL,
 FOREIGN KEY(round_id) REFERENCES competition_round(id), FOREIGN KEY(question_id) REFERENCES question(id),
 FOREIGN KEY(team_id) REFERENCES team(id)
);
CREATE TABLE IF NOT EXISTS competition_result (
 competition_id INTEGER NOT NULL, team_id INTEGER NOT NULL, total_score INTEGER NOT NULL,
 correct_count INTEGER NOT NULL, elapsed_seconds INTEGER NOT NULL, final_rank INTEGER NOT NULL,
 promoted INTEGER NOT NULL, PRIMARY KEY(competition_id,team_id),
 FOREIGN KEY(competition_id) REFERENCES competition(id), FOREIGN KEY(team_id) REFERENCES team(id)
);
