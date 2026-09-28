package cn.edu.aviationquiz.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cn.edu.aviationquiz.dao.AccountDao;
import cn.edu.aviationquiz.entity.AccountRecord;
import cn.edu.aviationquiz.entity.Models.PlayerProfileInput;
import cn.edu.aviationquiz.entity.Models.PlayerProfileView;
import cn.edu.aviationquiz.exception.BusinessException;
import cn.edu.aviationquiz.service.impl.AccountManagementServiceImpl;

import org.junit.jupiter.api.Test;

import java.util.Optional;

class AccountManagementServiceTest {
    private static final PlayerProfileInput PROFILE =
            new PlayerProfileInput(
                    " 测试大学 ", "航空学院", "飞行专业", "20260001", "张三", "13800000001");

    @Test
    void validatesHashesAndPersistsAPlayerThroughTheDaoContract() {
        FakeAccountDao dao = new FakeAccountDao();
        AccountManagementService service = new AccountManagementServiceImpl(dao);

        service.registerPlayer(" user01 ", "secret12", PROFILE);

        assertEquals("user01", dao.insertedUsername);
        assertEquals("测试大学", dao.insertedProfile.school());
        assertTrue(Passwords.verify("secret12", dao.insertedPasswordHash));
    }

    @Test
    void authenticatesWithoutExposingPasswordChecksToTheController() {
        FakeAccountDao dao = new FakeAccountDao();
        dao.account =
                new AccountRecord(
                        "P1", "user01", Passwords.hash("secret12"), "张三", false);
        AccountManagementService service = new AccountManagementServiceImpl(dao);

        assertEquals("P1", service.authenticate(false, "user01", "secret12").id());
        assertThrows(
                BusinessException.class,
                () -> service.authenticate(false, "user01", "wrong-password"));
    }

    @Test
    void rejectsADuplicateSchoolAndStudentNumberBeforeUpdating() {
        FakeAccountDao dao = new FakeAccountDao();
        dao.identityExists = true;
        AccountManagementService service = new AccountManagementServiceImpl(dao);

        assertThrows(
                BusinessException.class,
                () -> service.updatePlayerProfile("P1", PROFILE));
        assertNull(dao.updatedProfile);
    }

    private static final class FakeAccountDao implements AccountDao, AccountDao.Transaction {
        private boolean staffPresent;
        private boolean identityExists;
        private AccountRecord account;
        private String insertedUsername;
        private String insertedPasswordHash;
        private PlayerProfileInput insertedProfile;
        private PlayerProfileInput updatedProfile;

        @Override
        public <T> T inTransaction(Work<T> work) {
            try {
                return work.run(this);
            } catch (RuntimeException runtime) {
                throw runtime;
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        }

        @Override
        public boolean hasStaff() {
            return staffPresent;
        }

        @Override
        public boolean usernameExists(boolean staff, String username) {
            return false;
        }

        @Override
        public boolean playerIdentityExists(
                String school, String studentNumber, String excludedPlayerId) {
            return identityExists;
        }

        @Override
        public void insertStaff(
                String id, String username, String passwordHash, String name) {
            staffPresent = true;
            insertedUsername = username;
            insertedPasswordHash = passwordHash;
        }

        @Override
        public void insertPlayer(
                String id,
                String username,
                String passwordHash,
                PlayerProfileInput profile) {
            insertedUsername = username;
            insertedPasswordHash = passwordHash;
            insertedProfile = profile;
        }

        @Override
        public Optional<AccountRecord> findAccount(boolean staff, String username) {
            return Optional.ofNullable(account);
        }

        @Override
        public boolean isPlayerProfileComplete(String playerId) {
            return true;
        }

        @Override
        public PlayerProfileView findPlayerProfile(String playerId) {
            return null;
        }

        @Override
        public void updatePlayerProfile(String playerId, PlayerProfileInput profile) {
            updatedProfile = profile;
        }
    }
}
