package com.christopher.weighttracker.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.christopher.weighttracker.data.local.Goal;
import com.christopher.weighttracker.data.local.WeightEntry;
import com.christopher.weighttracker.domain.Result;
import com.christopher.weighttracker.fakes.FakeGoalDao;
import com.christopher.weighttracker.fakes.FakeWeightDao;
import com.christopher.weighttracker.util.AppExecutors;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

/** Synchronous, like {@link UserRepositoryTest} — see the note there on the executor seam. */
public class WeightRepositoryTest {

    private static final int USER_ID = 1;

    private FakeWeightDao weightDao;
    private FakeGoalDao goalDao;
    private WeightRepository repository;

    @Before
    public void setUp() {
        weightDao = new FakeWeightDao();
        goalDao = new FakeGoalDao();
        repository = new WeightRepository(weightDao, goalDao,
                new AppExecutors(Runnable::run, Runnable::run));
    }

    // ---- entries ------------------------------------------------------------------------

    @Test
    public void addEntryReturnsGeneratedId() {
        final Result<Integer>[] captured = new Result[1];
        repository.addEntry(USER_ID, "2026-08-19", 185.5, r -> captured[0] = r);

        assertTrue(captured[0].isSuccess());
        assertEquals(Integer.valueOf(1), captured[0].getOrNull());
        assertEquals(1, weightDao.size());
    }

    @Test
    public void addEntryStoresTheGivenValues() {
        repository.addEntry(USER_ID, "2026-08-19", 185.5, r -> { });

        WeightEntry stored = weightDao.entriesFor(USER_ID).get(0);
        assertEquals("2026-08-19", stored.getDateText());
        assertEquals(185.5, stored.getWeight(), 0.0001);
        assertEquals(USER_ID, stored.getUserId());
    }

    /** Newest first, with the id tiebreaker the DAO query declares. */
    @Test
    public void entriesAreOrderedNewestFirst() {
        repository.addEntry(USER_ID, "2026-08-19", 185.5, r -> { });
        repository.addEntry(USER_ID, "2026-08-21", 183.0, r -> { });
        repository.addEntry(USER_ID, "2026-08-20", 184.0, r -> { });

        List<WeightEntry> entries = weightDao.entriesFor(USER_ID);
        assertEquals("2026-08-21", entries.get(0).getDateText());
        assertEquals("2026-08-20", entries.get(1).getDateText());
        assertEquals("2026-08-19", entries.get(2).getDateText());
    }

    @Test
    public void entriesAreScopedToTheirUser() {
        repository.addEntry(USER_ID, "2026-08-19", 185.5, r -> { });
        repository.addEntry(2, "2026-08-19", 200.0, r -> { });

        assertEquals(1, weightDao.entriesFor(USER_ID).size());
        assertEquals(1, weightDao.entriesFor(2).size());
    }

    @Test
    public void updateEntryAppliesTheChange() {
        repository.addEntry(USER_ID, "2026-08-19", 185.5, r -> { });
        WeightEntry original = weightDao.entriesFor(USER_ID).get(0);

        final Result<Integer>[] captured = new Result[1];
        repository.updateEntry(original.withDateAndWeight("2026-08-20", 183.0),
                r -> captured[0] = r);

        assertTrue(captured[0].isSuccess());
        WeightEntry updated = weightDao.entriesFor(USER_ID).get(0);
        assertEquals("2026-08-20", updated.getDateText());
        assertEquals(183.0, updated.getWeight(), 0.0001);
        assertEquals("update must not create a second row", 1, weightDao.size());
    }

    @Test
    public void updateOfMissingRowReportsFailure() {
        Result<Integer>[] captured = new Result[1];
        repository.updateEntry(new WeightEntry(999, USER_ID, "2026-08-19", 185.5),
                r -> captured[0] = r);

        assertTrue(captured[0].isFailure());
    }

    @Test
    public void deleteEntryRemovesTheRow() {
        repository.addEntry(USER_ID, "2026-08-19", 185.5, r -> { });
        WeightEntry entry = weightDao.entriesFor(USER_ID).get(0);

        final Result<Integer>[] captured = new Result[1];
        repository.deleteEntry(entry, r -> captured[0] = r);

        assertTrue(captured[0].isSuccess());
        assertEquals(0, weightDao.size());
    }

    @Test
    public void deleteOfMissingRowReportsFailure() {
        Result<Integer>[] captured = new Result[1];
        repository.deleteEntry(new WeightEntry(999, USER_ID, "2026-08-19", 185.5),
                r -> captured[0] = r);

        assertTrue(captured[0].isFailure());
    }

    // ---- goals --------------------------------------------------------------------------

    @Test
    public void saveGoalStoresIt() {
        final Result<Void>[] captured = new Result[1];
        repository.saveGoal(USER_ID, 175.0, "5551234567", r -> captured[0] = r);

        assertTrue(captured[0].isSuccess());
        Goal stored = goalDao.findForUser(USER_ID);
        assertNotNull(stored);
        assertEquals(175.0, stored.getGoalWeight(), 0.0001);
        assertEquals("5551234567", stored.getPhone());
    }

    /**
     * The invariant behind {@code OnConflictStrategy.REPLACE} plus the unique index: a user has
     * at most one goal, however many times they press Save.
     */
    @Test
    public void saveGoalTwiceLeavesExactlyOneRow() {
        repository.saveGoal(USER_ID, 175.0, "5551234567", r -> { });
        repository.saveGoal(USER_ID, 170.0, "5559999999", r -> { });

        assertEquals(1, goalDao.size());
        Goal stored = goalDao.findForUser(USER_ID);
        assertEquals(170.0, stored.getGoalWeight(), 0.0001);
        assertEquals("5559999999", stored.getPhone());
    }

    /** A null phone is how a user turns alerts off; it must not be rejected. */
    @Test
    public void saveGoalAcceptsNullPhone() {
        final Result<Void>[] captured = new Result[1];
        repository.saveGoal(USER_ID, 175.0, null, r -> captured[0] = r);

        assertTrue(captured[0].isSuccess());
        assertEquals(null, goalDao.findForUser(USER_ID).getPhone());
    }

    @Test
    public void loadGoalReturnsSuccessWithNullWhenNoneSet() {
        final Result<Goal>[] captured = new Result[1];
        repository.loadGoal(USER_ID, r -> captured[0] = r);

        // Absence of a goal is a legitimate state, not an error.
        assertTrue(captured[0].isSuccess());
        assertEquals(null, captured[0].getOrNull());
    }
}
