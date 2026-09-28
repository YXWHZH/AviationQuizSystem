package cn.edu.aviationquiz.dao;

import cn.edu.aviationquiz.entity.AccountRecord;
import cn.edu.aviationquiz.entity.Models.PlayerProfileInput;
import cn.edu.aviationquiz.entity.Models.PlayerProfileView;

import java.util.Optional;

/** Data-access contract for staff accounts, player accounts and player profiles. */
public interface AccountDao {
    <T> T inTransaction(Work<T> work);

    @FunctionalInterface
    interface Work<T> {
        T run(Transaction transaction) throws Exception;
    }

    interface Transaction {
        boolean hasStaff() throws Exception;

        boolean usernameExists(boolean staff, String username) throws Exception;

        boolean playerIdentityExists(String school, String studentNumber, String excludedPlayerId)
                throws Exception;

        void insertStaff(String id, String username, String passwordHash, String name)
                throws Exception;

        void insertPlayer(
                String id,
                String username,
                String passwordHash,
                PlayerProfileInput profile)
                throws Exception;

        Optional<AccountRecord> findAccount(boolean staff, String username) throws Exception;

        boolean isPlayerProfileComplete(String playerId) throws Exception;

        PlayerProfileView findPlayerProfile(String playerId) throws Exception;

        void updatePlayerProfile(String playerId, PlayerProfileInput profile) throws Exception;
    }
}
