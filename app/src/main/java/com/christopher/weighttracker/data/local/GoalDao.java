package com.christopher.weighttracker.data.local;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

/**
 * Goal storage. At most one row per user.
 */
@Dao
public interface GoalDao {

    /**
     * Inserts the user's goal, replacing any existing one.
     *
     * <p><b>{@code REPLACE} is implemented as DELETE followed by INSERT</b>, which is a footgun
     * often enough that using it deserves an explicit justification. It is safe <i>here</i>,
     * for three specific reasons:
     * <ol>
     *   <li>Nothing references {@code goals.id}, so the fact that the row id churns on every save
     *       is invisible to the rest of the app.</li>
     *   <li>{@code goals} has no child tables, so the implicit DELETE cascades to nothing. (This
     *       is the one that bites people: a REPLACE on a parent row silently deletes its
     *       children.)</li>
     *   <li>Every column is written on every save, so the DELETE cannot wipe a field that the
     *       INSERT then fails to restore.</li>
     * </ol>
     * The moment any of those three stops being true, this must become a {@code @Transaction}
     * default method that tries UPDATE first and only INSERTs when it affects zero rows. One
     * line beats three <i>given</i> the reasoning is written down — the comment is the deliverable
     * here, not the annotation.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long upsert(Goal goal);

    /**
     * The user's goal, kept live. Emits null until a goal has been saved.
     *
     * <p>{@code LiveData} rather than a plain value because the dashboard displays the goal
     * continuously in its summary line, and it has to update the moment one is saved.
     */
    @Query("SELECT * FROM goals WHERE user_id = :userId LIMIT 1")
    LiveData<Goal> observeForUser(int userId);

    /** One-shot read used when re-evaluating the goal rule off the main thread. */
    @Nullable
    @Query("SELECT * FROM goals WHERE user_id = :userId LIMIT 1")
    Goal findForUser(int userId);
}
