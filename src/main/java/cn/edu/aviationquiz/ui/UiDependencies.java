package cn.edu.aviationquiz.ui;

import cn.edu.aviationquiz.controller.AccountController;
import cn.edu.aviationquiz.controller.CompetitionLiveController;
import cn.edu.aviationquiz.controller.CompetitionManagementController;
import cn.edu.aviationquiz.controller.CompetitionRoomController;
import cn.edu.aviationquiz.controller.QuestionBankController;
import cn.edu.aviationquiz.controller.RegistrationController;
import cn.edu.aviationquiz.controller.ResultController;
import cn.edu.aviationquiz.service.CompetitionRecovery;

/** Presentation-layer dependencies assembled by the application composition root. */
public record UiDependencies(
        AccountController accounts,
        CompetitionLiveController liveCompetition,
        CompetitionManagementController competitions,
        CompetitionRoomController competitionRoom,
        QuestionBankController questionBank,
        RegistrationController registrations,
        ResultController results,
        CompetitionRecovery recovery) {}
