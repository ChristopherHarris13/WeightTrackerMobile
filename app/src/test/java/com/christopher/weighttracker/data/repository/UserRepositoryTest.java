package com.christopher.weighttracker.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.christopher.weighttracker.data.local.User;
import com.christopher.weighttracker.domain.AppError;
import com.christopher.weighttracker.domain.Result;
import com.christopher.weighttracker.fakes.FakePasswordHasher;
import com.christopher.weighttracker.fakes.FakeUserDao;
import com.christopher.weighttracker.util.AppExecutors;

import org.junit.Before;
import org.junit.Test;

/**
 * Runs entirely synchronously on the JVM.
 *
 * <p>The trick is in {@link #setUp}: {@code AppExecutors} is constructed with
 * {@code Runnable::run} for both executors, so every block the repository schedules runs inline
 * on the calling thread. No background threads, no waiting, no flakiness, and no
 * {@code InstantTaskExecutorRule}.
 *
 * <p>That is the concrete return on making threading a constructor parameter rather than a
 * static utility. Had {@code AppExecutors} exposed a static singleton, none of this would be
 * reachable without a mocking framework.
 */
public class UserRepositoryTest {

    private FakeUserDao userDao;
    private UserRepository repository;
    private Result<Integer> captured;

    @Before
    public void setUp() {
        userDao = new FakeUserDao();
        AppExecutors synchronousExecutors = new AppExecutors(Runnable::run, Runnable::run);
        repository = new UserRepository(userDao, new FakePasswordHasher(), synchronousExecutors);
        captured = null;
    }

    /** Because the executors run inline, the callback has already fired by the time this returns. */
    private Result<Integer> register(String username, String password) {
        repository.register(username, password, result -> captured = result);
        return captured;
    }

    private Result<Integer> login(String username, String password) {
        repository.login(username, password, result -> captured = result);
        return captured;
    }

    // ---- register -----------------------------------------------------------------------

    @Test
    public void registerReturnsNewUserId() {
        Result<Integer> result = register("chris", "hunter2");

        assertTrue(result.isSuccess());
        assertEquals(Integer.valueOf(1), result.getOrNull());
    }

    @Test
    public void registerDuplicateUsernameReturnsUsernameTaken() {
        register("chris", "hunter2");
        Result<Integer> second = register("chris", "different");

        assertTrue(second.isFailure());
        assertEquals(AppError.USERNAME_TAKEN, second.getError());
        assertEquals("no second row should have been created", 1, userDao.size());
    }

    /**
     * The security property that motivated this entire layer, asserted against what was actually
     * persisted rather than against how it was persisted.
     *
     * <p>No amount of code review guarantees this permanently; a test does.
     */
    @Test
    public void registerStoresHashNotPlaintext() {
        register("chris", "hunter2");

        User stored = userDao.storedUser("chris");
        assertNotNull(stored);
        assertFalse("the raw password must not appear in the stored value",
                stored.getPasswordHash().contains("hunter2") && !stored.getPasswordHash()
                        .startsWith(FakePasswordHasher.PREFIX));
        assertEquals(FakePasswordHasher.PREFIX + "hunter2", stored.getPasswordHash());
    }

    @Test
    public void differentUsernamesGetDifferentIds() {
        Result<Integer> first = register("chris", "hunter2");
        Result<Integer> second = register("alex", "hunter2");

        assertEquals(Integer.valueOf(1), first.getOrNull());
        assertEquals(Integer.valueOf(2), second.getOrNull());
    }

    // ---- login --------------------------------------------------------------------------

    @Test
    public void loginWithCorrectPasswordReturnsUserId() {
        register("chris", "hunter2");
        Result<Integer> result = login("chris", "hunter2");

        assertTrue(result.isSuccess());
        assertEquals(Integer.valueOf(1), result.getOrNull());
    }

    @Test
    public void loginWithWrongPasswordReturnsInvalidCredentials() {
        register("chris", "hunter2");
        Result<Integer> result = login("chris", "wrong");

        assertTrue(result.isFailure());
        assertEquals(AppError.INVALID_CREDENTIALS, result.getError());
    }

    /**
     * Encodes a security requirement as an executable assertion.
     *
     * <p>Returning a distinct "no such user" error would be friendlier and is a natural instinct,
     * but it lets an attacker enumerate which accounts exist. If someone later "improves" the UX
     * by splitting these two messages, this test fails and explains why — which is a great deal
     * more durable than a comment.
     */
    @Test
    public void loginWithUnknownUserReturnsSameErrorAsWrongPassword() {
        register("chris", "hunter2");

        AppError wrongPassword = login("chris", "wrong").getError();
        AppError unknownUser = login("nobody", "hunter2").getError();

        assertEquals(wrongPassword, unknownUser);
        assertEquals(AppError.INVALID_CREDENTIALS, unknownUser);
    }

    @Test
    public void loginOnEmptyDatabaseFailsCleanly() {
        Result<Integer> result = login("chris", "hunter2");

        assertTrue(result.isFailure());
        assertEquals(AppError.INVALID_CREDENTIALS, result.getError());
    }
}
