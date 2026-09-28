package cn.edu.aviationquiz.service.impl;

import cn.edu.aviationquiz.dao.AccountDao;
import cn.edu.aviationquiz.entity.AccountRecord;
import cn.edu.aviationquiz.entity.Models.PlayerProfileInput;
import cn.edu.aviationquiz.entity.Models.PlayerProfileView;
import cn.edu.aviationquiz.exception.BusinessException;
import cn.edu.aviationquiz.service.AccountManagementService;
import cn.edu.aviationquiz.service.Passwords;

import java.util.UUID;

/** Default account rules, independent of JavaFX and JDBC. */
public final class AccountManagementServiceImpl implements AccountManagementService {
    private final AccountDao accountDao;

    public AccountManagementServiceImpl(AccountDao accountDao) {
        this.accountDao = accountDao;
    }

    @Override
    public boolean needsStaffSetup() {
        return accountDao.inTransaction(db -> !db.hasStaff());
    }

    @Override
    public void setupStaff(String username, String password, String name) {
        accountDao.inTransaction(
                db -> {
                    require(!db.hasStaff(), "工作人员已初始化");
                    String validUsername = bounded(username, 4, 20, "账号");
                    String validName = bounded(name, 2, 20, "姓名");
                    validatePassword(password);
                    require(!db.usernameExists(true, validUsername), "账号已存在");
                    db.insertStaff(
                            id("S"),
                            validUsername,
                            Passwords.hash(password),
                            validName);
                    return null;
                });
    }

    @Override
    public void registerPlayer(
            String username, String password, PlayerProfileInput profile) {
        accountDao.inTransaction(
                db -> {
                    PlayerProfileInput validProfile = validProfile(profile);
                    String validUsername = bounded(username, 4, 20, "账号");
                    validatePassword(password);
                    require(!db.usernameExists(false, validUsername), "账号已存在");
                    require(
                            !db.playerIdentityExists(
                                    validProfile.school(),
                                    validProfile.studentNumber(),
                                    null),
                            "该院校学号已注册");
                    db.insertPlayer(
                            id("P"),
                            validUsername,
                            Passwords.hash(password),
                            validProfile);
                    return null;
                });
    }

    @Override
    public AccountRecord authenticate(boolean staff, String username, String password) {
        return accountDao.inTransaction(
                db -> {
                    String candidate = username == null ? "" : username.trim();
                    var account = db.findAccount(staff, candidate);
                    require(
                            account.isPresent()
                                    && password != null
                                    && Passwords.verify(
                                            password, account.get().passwordHash()),
                            "账号或密码错误");
                    return account.get();
                });
    }

    @Override
    public boolean profileComplete(String playerId) {
        return accountDao.inTransaction(db -> db.isPlayerProfileComplete(playerId));
    }

    @Override
    public PlayerProfileView playerProfile(String playerId) {
        return accountDao.inTransaction(db -> db.findPlayerProfile(playerId));
    }

    @Override
    public void updatePlayerProfile(String playerId, PlayerProfileInput profile) {
        accountDao.inTransaction(
                db -> {
                    PlayerProfileInput validProfile = validProfile(profile);
                    require(
                            !db.playerIdentityExists(
                                    validProfile.school(),
                                    validProfile.studentNumber(),
                                    playerId),
                            "该院校学号已注册");
                    db.updatePlayerProfile(playerId, validProfile);
                    return null;
                });
    }

    private static PlayerProfileInput validProfile(PlayerProfileInput profile) {
        require(profile != null, "请填写选手资料");
        String school = bounded(profile.school(), 2, 50, "院校");
        String college = bounded(profile.college(), 2, 50, "学院");
        String major = bounded(profile.major(), 2, 50, "专业");
        String studentNumber = bounded(profile.studentNumber(), 4, 30, "学号");
        require(
                studentNumber.matches("[A-Za-z0-9_-]+"),
                "学号只能包含字母、数字、连字符或下划线");
        String name = bounded(profile.name(), 2, 20, "姓名");
        String phone = profile.phone() == null ? "" : profile.phone().trim();
        require(phone.matches("[0-9]{11}"), "手机号必须为 11 位数字");
        return new PlayerProfileInput(
                school, college, major, studentNumber, name, phone);
    }

    private static void validatePassword(String password) {
        require(
                password != null && password.length() >= 6 && password.length() <= 20,
                "密码长度须为 6～20");
    }

    private static String bounded(String text, int min, int max, String label) {
        String value = text == null ? "" : text.trim();
        require(
                value.length() >= min && value.length() <= max,
                label + "长度须为 " + min + "～" + max);
        return value;
    }

    private static void require(boolean valid, String message) {
        if (!valid) throw new BusinessException(message);
    }

    private static String id(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "");
    }
}
