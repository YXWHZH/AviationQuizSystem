package cn.edu.aviationquiz.controller;

import cn.edu.aviationquiz.entity.Models.PlayerProfileInput;
import cn.edu.aviationquiz.entity.Models.PlayerProfileView;
import cn.edu.aviationquiz.entity.Models.Session;
import cn.edu.aviationquiz.exception.BusinessException;
import cn.edu.aviationquiz.service.AccountUseCases;

/** Handles account-page input and delegates account rules to the use-case interface. */
public final class AccountController {
    private final AccountUseCases service;

    public AccountController(AccountUseCases service) {
        this.service = service;
    }

    public boolean needsStaffSetup() {
        return service.needsSetup();
    }

    public void setupStaff(String username, String password, String name) {
        requireCredentials(username, password);
        service.setupStaff(username, password, name);
    }

    public void registerPlayer(
            String username,
            String password,
            String confirmedPassword,
            PlayerProfileInput profile) {
        requireCredentials(username, password);
        if (!password.equals(confirmedPassword))
            throw new BusinessException("两次输入的密码不一致");
        service.register(username, password, profile);
    }

    public Session login(boolean staff, String username, String password) {
        requireCredentials(username, password);
        return service.login(staff, username, password);
    }

    public void logout(Session session) {
        service.logout(session);
    }

    public boolean profileComplete(Session session) {
        return service.profileComplete(session);
    }

    public PlayerProfileView playerProfile(Session session) {
        return service.playerProfile(session);
    }

    public void updatePlayerProfile(Session session, PlayerProfileInput profile) {
        service.updatePlayerProfile(session, profile);
    }

    private static void requireCredentials(String username, String password) {
        if (username == null || username.isBlank()) throw new BusinessException("账号不能为空");
        if (password == null || password.isBlank()) throw new BusinessException("密码不能为空");
    }
}
