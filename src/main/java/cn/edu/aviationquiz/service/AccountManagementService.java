package cn.edu.aviationquiz.service;

import cn.edu.aviationquiz.entity.AccountRecord;
import cn.edu.aviationquiz.entity.Models.PlayerProfileInput;
import cn.edu.aviationquiz.entity.Models.PlayerProfileView;

/** Business rules for account creation, authentication and player profiles. */
public interface AccountManagementService {
    boolean needsStaffSetup();

    void setupStaff(String username, String password, String name);

    void registerPlayer(String username, String password, PlayerProfileInput profile);

    AccountRecord authenticate(boolean staff, String username, String password);

    boolean profileComplete(String playerId);

    PlayerProfileView playerProfile(String playerId);

    void updatePlayerProfile(String playerId, PlayerProfileInput profile);
}
