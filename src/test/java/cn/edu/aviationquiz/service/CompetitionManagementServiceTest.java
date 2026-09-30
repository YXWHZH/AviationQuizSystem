package cn.edu.aviationquiz.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cn.edu.aviationquiz.dao.CompetitionDao;
import cn.edu.aviationquiz.entity.CompetitionSetupContext;
import cn.edu.aviationquiz.entity.Models.CompetitionInput;
import cn.edu.aviationquiz.entity.Models.CompetitionView;
import cn.edu.aviationquiz.exception.BusinessException;
import cn.edu.aviationquiz.service.impl.CompetitionManagementServiceImpl;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

class CompetitionManagementServiceTest {
    private static final Clock CLOCK =
            Clock.fixed(Instant.ofEpochMilli(1_500), ZoneOffset.UTC);

    @Test
    void validatesNormalizesAndSavesThroughTheDaoContract() {
        FakeCompetitionDao dao = new FakeCompetitionDao();
        CompetitionManagementService service =
                new CompetitionManagementServiceImpl(dao, CLOCK);

        String id =
                service.save(
                        null,
                        new CompetitionInput(
                                " 航空知识赛 ",
                                " 简介 ",
                                1_000,
                                2_000,
                                3_000,
                                2,
                                Set.of("民航史")));

        assertNotNull(id);
        assertEquals("航空知识赛", dao.inserted.name());
        assertEquals("简介", dao.inserted.description());
        assertEquals(Set.of("民航史"), dao.categories);
    }

    @Test
    void preventsRemovingACategoryUsedByTheExistingQuestionList() {
        FakeCompetitionDao dao = new FakeCompetitionDao();
        dao.context = new CompetitionSetupContext("C1", "未开放", 1_000, 2_000);
        dao.usedCategories = Set.of("航空法规");
        CompetitionManagementService service =
                new CompetitionManagementServiceImpl(dao, CLOCK);

        assertThrows(
                BusinessException.class,
                () ->
                        service.save(
                                "C1",
                                new CompetitionInput(
                                        "航空知识赛",
                                        "简介",
                                        1_000,
                                        2_000,
                                        3_000,
                                        2,
                                        Set.of("民航史"))));
        assertNull(dao.updated);
    }

    @Test
    void opensRegistrationOnlyInsideTheConfiguredTimeWindow() {
        FakeCompetitionDao dao = new FakeCompetitionDao();
        dao.context = new CompetitionSetupContext("C1", "未开放", 1_000, 2_000);
        CompetitionManagementService service =
                new CompetitionManagementServiceImpl(dao, CLOCK);

        service.changeRegistrationState("C1", "报名中");

        assertEquals("报名中", dao.updatedStatus);
    }

    private static final class FakeCompetitionDao
            implements CompetitionDao, CompetitionDao.Transaction {
        private CompetitionSetupContext context;
        private Set<String> usedCategories = Set.of();
        private CompetitionInput inserted;
        private CompetitionInput updated;
        private Set<String> categories;
        private String updatedStatus;

        @Override
        public List<CompetitionView> findAll() {
            return List.of();
        }

        @Override
        public List<CompetitionView> findAllForPlayer(String playerId) {
            return List.of();
        }

        @Override
        public List<CompetitionView> findParticipatedByPlayer(String playerId) {
            return List.of();
        }

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
        public CompetitionSetupContext findCompetition(String competitionId) {
            return context;
        }

        @Override
        public Set<String> findUsedQuestionCategories(String competitionId) {
            return usedCategories;
        }

        @Override
        public void insertCompetition(String competitionId, CompetitionInput input) {
            inserted = input;
        }

        @Override
        public void updateCompetition(String competitionId, CompetitionInput input) {
            updated = input;
        }

        @Override
        public void replaceCategories(String competitionId, Set<String> categories) {
            this.categories = categories;
        }

        @Override
        public void updateStatus(String competitionId, String status) {
            updatedStatus = status;
        }
    }
}
