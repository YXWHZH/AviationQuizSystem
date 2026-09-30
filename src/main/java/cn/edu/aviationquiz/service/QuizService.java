package cn.edu.aviationquiz.service;

import cn.edu.aviationquiz.entity.AccountRecord;
import cn.edu.aviationquiz.entity.Models.*;
import cn.edu.aviationquiz.entity.ParticipationType;
import cn.edu.aviationquiz.exception.BusinessException;

import java.time.Clock;
import java.util.*;

/** Application use cases. Each mutation checks authority and owns one transaction. */
public final class QuizService
        implements AccountUseCases,
                CompetitionManagementUseCases,
                CompetitionLiveUseCases,
                CompetitionRecovery,
                CompetitionRoomService,
                QuestionBankUseCases,
                RegistrationUseCases,
                ResultUseCases {
    private final Clock clock;
    private final AccountManagementService accountService;
    private final CompetitionManagementService competitionManagementService;
    private final CompetitionLiveService competitionLiveService;
    private final CompetitionExecutionService executionService;
    private final QuestionBankService questionBankService;
    private final RegistrationManagementService registrationService;
    private final ResultService resultService;
    private final Map<String, Session> sessions = new HashMap<>();

    public QuizService(
            Clock clock,
            AccountManagementService accountService,
            CompetitionManagementService competitionManagementService,
            CompetitionLiveService competitionLiveService,
            CompetitionExecutionService executionService,
            QuestionBankService questionBankService,
            RegistrationManagementService registrationService,
            ResultService resultService) {
        this.clock = clock;
        this.accountService = accountService;
        this.competitionManagementService = competitionManagementService;
        this.competitionLiveService = competitionLiveService;
        this.executionService = executionService;
        this.questionBankService = questionBankService;
        this.registrationService = registrationService;
        this.resultService = resultService;
        recover();
    }

    private static String id(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "");
    }

    private static void require(boolean valid, String message) {
        if (!valid) throw new BusinessException(message);
    }

    private long now() {
        return clock.millis();
    }

    private void auth(Session s, boolean staff) {
        require(
                s != null && s.equals(sessions.get(s.token())) && s.staff() == staff,
                "请使用对应身份重新登录");
    }

    private void signed(Session s) {
        require(s != null && s.equals(sessions.get(s.token())), "请重新登录");
    }

    private static String bounded(String text, int min, int max, String label) {
        String value = text == null ? "" : text.trim();
        require(value.length() >= min && value.length() <= max, label + "长度须为 " + min + "～" + max);
        return value;
    }

    @Override
    public synchronized boolean needsSetup() {
        return accountService.needsStaffSetup();
    }

    @Override
    public synchronized void setupStaff(String username, String password, String name) {
        accountService.setupStaff(username, password, name);
    }

    @Override
    public synchronized void register(String username, String password, PlayerProfileInput profile) {
        accountService.registerPlayer(username, password, profile);
    }

    @Override
    public synchronized boolean profileComplete(Session s) {
        auth(s, false);
        return accountService.profileComplete(s.id());
    }

    @Override
    public synchronized PlayerProfileView playerProfile(Session s) {
        auth(s, false);
        return accountService.playerProfile(s.id());
    }

    @Override
    public synchronized void updatePlayerProfile(Session s, PlayerProfileInput profile) {
        auth(s, false);
        accountService.updatePlayerProfile(s.id(), profile);
    }

    @Override
    public synchronized Session login(boolean staff, String username, String password) {
        AccountRecord account = accountService.authenticate(staff, username, password);
        for (Session session : sessions.values())
            if (session.staff() == account.staff() && session.id().equals(account.id()))
                return session;
        Session session =
                new Session(
                        id("SESSION"),
                        account.id(),
                        account.username(),
                        account.name(),
                        account.staff());
        sessions.put(session.token(), session);
        return session;
    }

    @Override
    public synchronized void logout(Session s) {
        if (s != null) sessions.remove(s.token());
    }

    @Override
    public synchronized List<CompetitionView> competitions() {
        return competitionManagementService.competitions();
    }

    @Override
    public synchronized List<CompetitionView> competitionsForPlayer(Session s) {
        auth(s, false);
        return competitionManagementService.competitionsForPlayer(s.id());
    }

    @Override
    public synchronized String saveCompetition(Session s, String existing, CompetitionInput input) {
        auth(s, true);
        return competitionManagementService.save(existing, input);
    }

    @Override
    public synchronized void registrationState(Session s, String cid, String state) {
        auth(s, true);
        competitionManagementService.changeRegistrationState(cid, state);
    }

    @Override
    public synchronized void join(Session s, String cid, boolean reservation) {
        auth(s, false);
        registrationService.join(
                s.id(),
                cid,
                reservation ? ParticipationType.RESERVATION : ParticipationType.REGISTRATION);
    }

    @Override
    public synchronized void cancelParticipation(Session s, String cid, boolean reservation) {
        auth(s, false);
        registrationService.cancel(
                s.id(),
                cid,
                reservation ? ParticipationType.RESERVATION : ParticipationType.REGISTRATION);
    }

    @Override
    public synchronized List<CompetitionView> participatedCompetitions(Session s) {
        auth(s, false);
        return competitionManagementService.participatedCompetitions(s.id());
    }

    @Override
    public synchronized List<ParticipantView> participants(
            Session s, String cid, boolean reserved) {
        auth(s, true);
        return registrationService.participants(cid, reserved);
    }

    @Override
    public synchronized List<GroupView> groups(Session s, String cid) {
        signed(s);
        return registrationService.groups(cid);
    }

    @Override
    public synchronized String addGroup(Session s, String cid, String name, int sequence) {
        auth(s, true);
        return registrationService.addGroup(cid, name, sequence);
    }

    @Override
    public synchronized void assign(Session s, String registration, String group) {
        auth(s, true);
        registrationService.assign(registration, group);
    }

    @Override
    public synchronized void deleteEmptyGroup(Session s, String group) {
        auth(s, true);
        registrationService.deleteEmptyGroup(group);
    }

    @Override
    public synchronized void updateRound(
            Session s, String rid, String name, String type, int sequence, int seconds) {
        auth(s, true);
        questionBankService.updateRound(rid, name, type, sequence, seconds);
    }

    @Override
    public synchronized void deleteEmptyRound(Session s, String rid) {
        auth(s, true);
        questionBankService.deleteEmptyRound(rid);
    }

    @Override
    public synchronized List<QuestionView> questions(Session s) {
        auth(s, true);
        return questionBankService.questions();
    }

    @Override
    public synchronized List<QuestionView> questionsForCompetition(Session s, String cid) {
        auth(s, true);
        return questionBankService.questionsForCompetition(cid);
    }

    @Override
    public synchronized String saveQuestion(Session s, String existing, QuestionInput q) {
        auth(s, true);
        return questionBankService.saveQuestion(existing, q);
    }

    @Override
    public synchronized void questionState(Session s, String qid, boolean active) {
        auth(s, true);
        questionBankService.setQuestionActive(qid, active);
    }

    @Override
    public synchronized void deleteQuestion(Session s, String qid) {
        auth(s, true);
        questionBankService.deleteQuestion(qid);
    }

    @Override
    public synchronized List<RoundView> rounds(Session s, String cid) {
        auth(s, true);
        return questionBankService.rounds(cid);
    }

    @Override
    public synchronized String addRound(
            Session s, String cid, String name, String type, int sequence, int seconds) {
        auth(s, true);
        return questionBankService.addRound(cid, name, type, sequence, seconds);
    }

    @Override
    public synchronized List<RoundQuestionView> roundQuestions(Session s, String rid) {
        auth(s, true);
        return questionBankService.roundQuestions(rid);
    }

    @Override
    public synchronized void addQuestionToRound(Session s, String rid, String qid, int sequence) {
        auth(s, true);
        questionBankService.addQuestionToRound(rid, qid, sequence);
    }

    @Override
    public synchronized void removeRoundQuestion(Session s, String rqid) {
        auth(s, true);
        questionBankService.removeRoundQuestion(rqid);
    }

    @Override
    public synchronized void startCompetition(Session s, String cid) {
        auth(s, true);
        competitionLiveService.startCompetition(cid);
    }

    @Override
    public synchronized CompetitionProgressView progress(Session s, String cid) {
        auth(s, true);
        return competitionLiveService.progress(cid);
    }

    @Override
    public synchronized int completedQuestions(Session s, String cid) {
        auth(s, true);
        return competitionLiveService.completedQuestions(cid);
    }

    @Override
    public synchronized void startRound(Session s, String cid) {
        auth(s, true);
        competitionLiveService.startRound(cid);
    }

    @Override
    public synchronized String publish(Session s, String cid) {
        auth(s, true);
        return competitionLiveService.publish(cid);
    }

    @Override
    public synchronized int submit(Session s, String release, String option) {
        auth(s, false);
        competitionLiveService.recover();
        return executionService.submitAnswer(s.id(), release, option);
    }

    public synchronized boolean recover() {
        return competitionLiveService.recover();
    }

    @Override
    public synchronized ActiveReleaseView activeRelease(Session s, String cid) {
        auth(s, true);
        return competitionLiveService.activeRelease(cid);
    }

    @Override
    public synchronized void closeQuestion(Session s, String cid) {
        auth(s, true);
        competitionLiveService.closeQuestion(cid);
    }

    @Override
    public synchronized void finishRound(Session s, String cid) {
        auth(s, true);
        competitionLiveService.finishRound(cid);
    }

    @Override
    public synchronized PublishedQuestionView room(Session s, String cid) {
        auth(s, false);
        return competitionLiveService.room(s.id(), cid);
    }

    @Override
    public synchronized List<PlayerSubmissionView> monitor(Session s, String cid) {
        auth(s, true);
        return competitionLiveService.monitor(cid);
    }

    @Override
    public synchronized List<RankingEntry> ranking(Session s, String cid) {
        signed(s);
        return resultService.ranking(cid);
    }

    @Override
    public synchronized List<RankingEntry> preview(Session s, String cid) {
        auth(s, true);
        return resultService.preview(cid);
    }

    @Override
    public synchronized void archive(Session s, String cid) {
        auth(s, true);
        resultService.archive(cid);
    }

    @Override
    public synchronized List<HistoryView> history(Session s) {
        signed(s);
        return resultService.history(s.id(), s.staff());
    }

    @Override
    public synchronized String exportCsv(Session s, String cid) {
        auth(s, true);
        return resultService.exportCsv(cid);
    }
}
