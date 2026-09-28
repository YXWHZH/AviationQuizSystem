package cn.edu.aviationquiz.service;

import cn.edu.aviationquiz.entity.Models.PlayerProfileInput;
import cn.edu.aviationquiz.entity.Models.PlayerProfileView;
import cn.edu.aviationquiz.entity.Models.Session;

/** Account and profile operations exposed to the account controller. */
public interface AccountUseCases {
    boolean needsSetup();

    void setupStaff(String username, String password, String name);

    void register(String username, String password, PlayerProfileInput profile);

    Session login(boolean staff, String username, String password);

    void logout(Session session);

    boolean profileComplete(Session session);

    PlayerProfileView playerProfile(Session session);

    void updatePlayerProfile(Session session, PlayerProfileInput profile);
}
