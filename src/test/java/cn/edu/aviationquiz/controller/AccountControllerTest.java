package cn.edu.aviationquiz.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cn.edu.aviationquiz.entity.Models.PlayerProfileInput;
import cn.edu.aviationquiz.entity.Models.PlayerProfileView;
import cn.edu.aviationquiz.entity.Models.Session;
import cn.edu.aviationquiz.exception.BusinessException;
import cn.edu.aviationquiz.service.AccountUseCases;

import org.junit.jupiter.api.Test;

class AccountControllerTest {
    private static final PlayerProfileInput PROFILE =
            new PlayerProfileInput(
                    "测试大学", "航空学院", "飞行专业", "20260001", "张三", "13800000001");

    @Test
    void delegatesValidPlayerRegistrationToTheUseCaseInterface() {
        FakeAccountService service = new FakeAccountService();
        AccountController controller = new AccountController(service);

        controller.registerPlayer("user01", "secret12", "secret12", PROFILE);

        assertEquals("user01", service.username);
        assertEquals(PROFILE, service.profile);
    }

    @Test
    void rejectsMismatchedPasswordsBeforeCallingTheService() {
        FakeAccountService service = new FakeAccountService();
        AccountController controller = new AccountController(service);

        assertThrows(
                BusinessException.class,
                () -> controller.registerPlayer("user01", "secret12", "different", PROFILE));
        assertNull(service.username);
    }

    @Test
    void delegatesLoginAndReturnsTheSession() {
        FakeAccountService service = new FakeAccountService();
        AccountController controller = new AccountController(service);

        Session session = controller.login(false, "user01", "secret12");

        assertEquals(FakeAccountService.SESSION, session);
        assertEquals("user01", service.username);
    }

    private static final class FakeAccountService implements AccountUseCases {
        private static final Session SESSION =
                new Session("token", "player-1", "user01", "张三", false);
        private String username;
        private PlayerProfileInput profile;

        @Override
        public boolean needsSetup() {
            return false;
        }

        @Override
        public void setupStaff(String username, String password, String name) {
            this.username = username;
        }

        @Override
        public void register(
                String username, String password, PlayerProfileInput profile) {
            this.username = username;
            this.profile = profile;
        }

        @Override
        public Session login(boolean staff, String username, String password) {
            this.username = username;
            return SESSION;
        }

        @Override
        public void logout(Session session) {}

        @Override
        public boolean profileComplete(Session session) {
            return true;
        }

        @Override
        public PlayerProfileView playerProfile(Session session) {
            return null;
        }

        @Override
        public void updatePlayerProfile(Session session, PlayerProfileInput profile) {}
    }
}
