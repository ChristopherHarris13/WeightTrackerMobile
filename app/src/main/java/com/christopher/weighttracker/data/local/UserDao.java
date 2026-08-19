package com.christopher.weighttracker.data.local;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

/**
 * Account storage.
 *
 * <p>Both methods return plain values rather than {@code LiveData}, following the rule used
 * throughout this codebase: <b>{@code LiveData} for state the UI continuously displays, plain
 * values for one-shot commands and for queries that feed a decision.</b> Nothing observes "the
 * current user row"; login asks a question once and acts on the answer.
 *
 * <p>Every method here blocks, so every call site must be on a background thread.
 * {@code UserRepository} is what guarantees that.
 */
@Dao
public interface UserDao {

    /**
     * Inserts a new account, returning the generated id, or {@code -1} if the username is taken.
     *
     * <p><b>{@code IGNORE} rather than the default {@code ABORT}, and this choice is worth
     * dwelling on.</b> With {@code ABORT}, a duplicate username throws
     * {@code SQLiteConstraintException} — turning something a user does routinely, by typing a
     * popular name, into an exception that has to be caught and translated somewhere.
     *
     * <p>With {@code IGNORE}, the UNIQUE index quietly declines the insert and returns {@code -1}.
     * That is:
     * <ul>
     *   <li><b>Race-free.</b> The index is the single source of truth. A "does this username
     *       exist?" query followed by an insert has a window between the two where another
     *       insert can land; this has no window at all.</li>
     *   <li><b>Exactly the legacy contract.</b> The original {@code DBHelper.registerUser}
     *       returned {@code -1} on collision, so behaviour is preserved precisely.</li>
     *   <li><b>Translatable at the boundary.</b> {@code UserRepository} turns {@code -1} into
     *       {@link com.christopher.weighttracker.domain.AppError#USERNAME_TAKEN}, converting a
     *       platform-level detail into a domain fact right where the layers meet. That
     *       conversion is what a repository is <i>for</i>.</li>
     * </ul>
     *
     * <p>Deliberately no {@code countByUsername} pre-check: it would be both redundant and racy.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(User user);

    /** Returns the matching account, or null if there is none. */
    @Nullable
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    User findByUsername(String username);
}
