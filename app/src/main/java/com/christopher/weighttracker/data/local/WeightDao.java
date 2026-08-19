package com.christopher.weighttracker.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * Weight entry storage.
 *
 * <p>{@link #observeForUser} is the one method here that returns {@code LiveData}, and it is
 * responsible for deleting a whole category of code from the original app. Room re-runs a
 * {@code LiveData} query automatically whenever any write touches a table it reads from, so the
 * list and the summary refresh themselves after an insert, update, or delete. Every
 * {@code swapCursor()}, {@code notifyDataSetChanged()}, and {@code refreshSummary()} call in the
 * previous version was hand-rolled cache invalidation, and all of it goes away.
 */
@Dao
public interface WeightDao {

    /** Returns the generated row id. */
    @Insert
    long insert(WeightEntry entry);

    /** Returns the number of rows changed — 0 if the id no longer exists. */
    @Update
    int update(WeightEntry entry);

    /** Returns the number of rows removed. Matches on primary key. */
    @Delete
    int delete(WeightEntry entry);

    /**
     * All of a user's entries, newest first, kept live.
     *
     * <p><b>The {@code id DESC} tiebreaker is a fix, not a flourish.</b> The original ordered by
     * {@code date_text} alone, so two entries recorded on the same day had no defined relative
     * order — SQLite was free to return them differently between queries. With {@code DiffUtil}
     * now diffing consecutive lists, that instability would show up as rows visibly swapping
     * places for no reason.
     */
    @Query("SELECT * FROM weights WHERE user_id = :userId ORDER BY date_text DESC, id DESC")
    LiveData<List<WeightEntry>> observeForUser(int userId);

    // Deliberately absent: a getMostRecentWeight query.
    //
    // The original had one, which meant "most recent" was defined in two places -- once in the
    // list query and once in the summary query -- and the two could disagree. The ViewModel now
    // derives it with Transformations.map over the head of this list: one query, one definition,
    // and it cannot drift.
}
