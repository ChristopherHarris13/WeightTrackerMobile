package com.christopher.weighttracker.data.repository;

import com.christopher.weighttracker.data.local.User;
import com.christopher.weighttracker.data.local.UserDao;
import com.christopher.weighttracker.data.security.PasswordHasher;
import com.christopher.weighttracker.domain.AppError;
import com.christopher.weighttracker.domain.Result;
import com.christopher.weighttracker.util.AppExecutors;
import com.christopher.weighttracker.util.ResultCallback;

/**
 * Owns account data. The only place in the app that knows passwords are hashed, or that Room
 * exists at all.
 *
 * <p>Both operations are {@code void} plus a callback rather than returning a value, because both
 * involve PBKDF2 — hundreds of milliseconds of deliberate CPU work that must not happen on the
 * main thread. The callback always arrives on the main thread.
 *
 * <p>Callers are expected to pass an already-validated, already-normalized username. Format rules
 * live in {@code CredentialsValidator} and are applied by the ViewModel; the rule this codebase
 * follows is that validation needing no I/O is a pure function, and only checks that genuinely
 * require the datastore belong here. {@link AppError#USERNAME_TAKEN} is the one such check.
 */
public class UserRepository {

    private final UserDao userDao;
    private final PasswordHasher passwordHasher;
    private final AppExecutors executors;

    public UserRepository(UserDao userDao, PasswordHasher passwordHasher, AppExecutors executors) {
        this.userDao = userDao;
        this.passwordHasher = passwordHasher;
        this.executors = executors;
    }

    /**
     * Creates an account, reporting the new user id on success.
     *
     * <p>The duplicate-username check is the UNIQUE index itself rather than a preceding lookup:
     * see {@link UserDao#insert} for why that matters.
     */
    public void register(String normalizedUsername, String password,
                         ResultCallback<Integer> callback) {
        executors.io().execute(() -> {
            String hash = passwordHasher.hash(password);
            long id = userDao.insert(User.forNewRow(normalizedUsername, hash));

            Result<Integer> result = (id == -1L)
                    ? Result.failure(AppError.USERNAME_TAKEN)
                    : Result.success((int) id);

            executors.main().execute(() -> callback.onResult(result));
        });
    }

    /**
     * Verifies credentials, reporting the user id on success.
     *
     * <p><b>An unknown username and a wrong password produce the identical error.</b> Telling them
     * apart would be friendlier and is a common instinct, but it hands an attacker a
     * username-enumeration oracle: they could confirm which accounts exist simply by watching
     * which message comes back. {@code UserRepositoryTest} pins this down as an executable
     * assertion, so a later "UX improvement" that splits the two messages fails the build rather
     * than quietly regressing security.
     *
     * <p><b>Considered and deliberately not done:</b> hashing a dummy password when no user is
     * found, to equalise response time. Right now an unknown username returns almost instantly
     * while a known one takes the full PBKDF2 cost, which is a genuine timing side channel. It is
     * about three lines to close. It is skipped because a local-only SQLite app with no network
     * surface has no realistic attacker positioned to measure it, and because unjustified
     * defences are their own kind of cost. Documenting the reasoning is worth more here than the
     * three lines — but if this app ever grew a remote API, this is the first thing to revisit.
     */
    public void login(String normalizedUsername, String password,
                      ResultCallback<Integer> callback) {
        executors.io().execute(() -> {
            User user = userDao.findByUsername(normalizedUsername);

            Result<Integer> result =
                    (user == null || !passwordHasher.verify(password, user.getPasswordHash()))
                            ? Result.failure(AppError.INVALID_CREDENTIALS)
                            : Result.success(user.getId());

            executors.main().execute(() -> callback.onResult(result));
        });
    }
}
