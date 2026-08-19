package com.christopher.weighttracker.data.repository;

import androidx.lifecycle.LiveData;

import com.christopher.weighttracker.data.local.Goal;
import com.christopher.weighttracker.data.local.GoalDao;
import com.christopher.weighttracker.data.local.WeightEntry;
import com.christopher.weighttracker.data.local.WeightDao;
import com.christopher.weighttracker.domain.AppError;
import com.christopher.weighttracker.domain.Result;
import com.christopher.weighttracker.util.AppExecutors;
import com.christopher.weighttracker.util.ResultCallback;

import java.util.List;

/**
 * Owns weight entries and goals, and is the only thing in the app that talks to their DAOs.
 *
 * <h2>The shape of this class, and why it is asymmetric</h2>
 *
 * Queries return {@code LiveData} immediately and do no threading of their own — Room refreshes
 * them on its own executor. Commands return nothing and report through a {@link ResultCallback},
 * doing their work on {@code executors.io()} and delivering the answer on
 * {@code executors.main()}.
 *
 * <p>That asymmetry is worth naming rather than glossing over: <b>reads are a stream you
 * subscribe to; writes are a request you get exactly one answer to.</b> Trying to force both into
 * the same signature is what produces repositories that are awkward to use from both ends.
 */
public class WeightRepository {

    private final WeightDao weightDao;
    private final GoalDao goalDao;
    private final AppExecutors executors;

    public WeightRepository(WeightDao weightDao, GoalDao goalDao, AppExecutors executors) {
        this.weightDao = weightDao;
        this.goalDao = goalDao;
        this.executors = executors;
    }

    // ---- Queries ------------------------------------------------------------------------

    /** All of a user's entries, newest first. Re-emits automatically after any write. */
    public LiveData<List<WeightEntry>> observeEntries(int userId) {
        return weightDao.observeForUser(userId);
    }

    /** The user's goal, or null until one is saved. Re-emits automatically after any write. */
    public LiveData<Goal> observeGoal(int userId) {
        return goalDao.observeForUser(userId);
    }

    // ---- Commands -----------------------------------------------------------------------

    /**
     * Stores a new entry, reporting the generated id.
     *
     * <p>No explicit refresh follows this call and none is needed: the {@code LiveData} returned
     * by {@link #observeEntries} reads from the table just written, so Room re-runs it and every
     * observer updates. This is what replaced {@code swapCursor} plus {@code refreshSummary} in
     * the original.
     */
    public void addEntry(int userId, String dateText, double weight,
                         ResultCallback<Integer> callback) {
        executors.io().execute(() -> {
            long id = weightDao.insert(WeightEntry.forNewRow(userId, dateText, weight));
            Result<Integer> result = (id <= 0)
                    ? Result.failure(AppError.STORAGE_FAILURE)
                    : Result.success((int) id);
            executors.main().execute(() -> callback.onResult(result));
        });
    }

    /** Applies an edit, reporting the number of rows changed. */
    public void updateEntry(WeightEntry updated, ResultCallback<Integer> callback) {
        executors.io().execute(() -> {
            int rows = weightDao.update(updated);
            Result<Integer> result = (rows == 0)
                    ? Result.failure(AppError.STORAGE_FAILURE)
                    : Result.success(rows);
            executors.main().execute(() -> callback.onResult(result));
        });
    }

    /** Removes an entry, reporting the number of rows removed. */
    public void deleteEntry(WeightEntry entry, ResultCallback<Integer> callback) {
        executors.io().execute(() -> {
            int rows = weightDao.delete(entry);
            Result<Integer> result = (rows == 0)
                    ? Result.failure(AppError.STORAGE_FAILURE)
                    : Result.success(rows);
            executors.main().execute(() -> callback.onResult(result));
        });
    }

    /**
     * Saves or replaces the user's goal.
     *
     * <p>{@code phone} may be null, which is how a user turns goal alerts off.
     */
    public void saveGoal(int userId, double goalWeight, String phone,
                         ResultCallback<Void> callback) {
        executors.io().execute(() -> {
            long id = goalDao.upsert(Goal.forNewRow(userId, goalWeight, phone));
            Result<Void> result = (id <= 0)
                    ? Result.failure(AppError.STORAGE_FAILURE)
                    : Result.success(null);
            executors.main().execute(() -> callback.onResult(result));
        });
    }

    /**
     * Reads the current goal once, off the main thread.
     *
     * <p>Used when re-evaluating the goal rule after the user grants SMS permission, where the
     * observed {@code LiveData} value may not yet reflect a just-completed write.
     */
    public void loadGoal(int userId, ResultCallback<Goal> callback) {
        executors.io().execute(() -> {
            Goal goal = goalDao.findForUser(userId);
            Result<Goal> result = Result.success(goal);   // absence is not a failure
            executors.main().execute(() -> callback.onResult(result));
        });
    }
}
