CREATE TRIGGER assignment_same_competition BEFORE INSERT ON group_assignment
BEGIN
 SELECT CASE WHEN NOT EXISTS (
   SELECT 1 FROM registration r JOIN competition_group g ON g.competition_id=r.competition_id
   WHERE r.id=NEW.registration_id AND g.id=NEW.group_id AND r.status='有效'
 ) THEN RAISE(ABORT,'报名与小组不属于同一竞赛') END;
END;
-- statement
CREATE TRIGGER group_round_same_competition BEFORE INSERT ON group_round
BEGIN
 SELECT CASE WHEN NOT EXISTS (
   SELECT 1 FROM competition_group g JOIN competition_round r ON r.competition_id=g.competition_id
   WHERE g.id=NEW.group_id AND r.id=NEW.round_id
 ) THEN RAISE(ABORT,'小组与轮次不属于同一竞赛') END;
END;
-- statement
CREATE TRIGGER question_once_per_competition BEFORE INSERT ON round_question
BEGIN
 SELECT CASE WHEN EXISTS (
   SELECT 1 FROM round_question rq JOIN competition_round r ON r.id=rq.round_id
   JOIN competition_round target ON target.competition_id=r.competition_id
   WHERE target.id=NEW.round_id AND rq.question_id=NEW.question_id
 ) THEN RAISE(ABORT,'同场竞赛不能重复用题') END;
END;
-- statement
CREATE TRIGGER release_correct_round BEFORE INSERT ON question_release
BEGIN
 SELECT CASE WHEN NOT EXISTS (
   SELECT 1 FROM group_round gr JOIN round_question rq ON rq.round_id=gr.round_id
   JOIN competition_round r ON r.id=gr.round_id JOIN competition c ON c.id=r.competition_id
   WHERE gr.id=NEW.group_round_id AND rq.id=NEW.round_question_id AND gr.status='进行中' AND c.status='比赛中'
 ) THEN RAISE(ABORT,'题目发布与当前轮次不匹配') END;
END;
-- statement
CREATE TRIGGER answer_eligible BEFORE INSERT ON answer_record
BEGIN
 SELECT CASE WHEN EXISTS (SELECT 1 FROM timeout_record WHERE release_id=NEW.release_id AND player_id=NEW.player_id)
 THEN RAISE(ABORT,'该题已超时结算') END;
 SELECT CASE WHEN NOT EXISTS (
   SELECT 1 FROM question_release qr JOIN group_round gr ON gr.id=qr.group_round_id
   JOIN competition_round r ON r.id=gr.round_id JOIN competition c ON c.id=r.competition_id
   JOIN group_assignment ga ON ga.group_id=gr.group_id JOIN registration reg ON reg.id=ga.registration_id
   WHERE qr.id=NEW.release_id AND reg.player_id=NEW.player_id AND reg.status='有效'
     AND c.status='比赛中' AND qr.closed_at IS NULL
     AND NEW.submitted_at>=qr.started_at AND NEW.submitted_at<qr.deadline
 ) THEN RAISE(ABORT,'无效答题资格或提交时间') END;
END;
-- statement
CREATE TRIGGER timeout_eligible BEFORE INSERT ON timeout_record
BEGIN
 SELECT CASE WHEN EXISTS (SELECT 1 FROM answer_record WHERE release_id=NEW.release_id AND player_id=NEW.player_id)
 THEN RAISE(ABORT,'该题已有效提交') END;
 SELECT CASE WHEN NOT EXISTS (
   SELECT 1 FROM question_release qr JOIN group_round gr ON gr.id=qr.group_round_id
   JOIN group_assignment ga ON ga.group_id=gr.group_id JOIN registration reg ON reg.id=ga.registration_id
   WHERE qr.id=NEW.release_id AND reg.player_id=NEW.player_id AND NEW.settled_at>=qr.deadline
 ) THEN RAISE(ABORT,'无效超时记录') END;
END;
