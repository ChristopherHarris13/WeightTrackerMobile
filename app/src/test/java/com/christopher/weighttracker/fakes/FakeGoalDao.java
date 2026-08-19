package com.christopher.weighttracker.fakes;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.christopher.weighttracker.data.local.Goal;
import com.christopher.weighttracker.data.local.GoalDao;

import java.util.HashMap;
import java.util.Map;

/**
 * In-memory {@link GoalDao}. Keyed by user id, which is how the real table's unique index behaves.
 *
 * <p>{@link #upsert} replaces rather than merges, matching {@code OnConflictStrategy.REPLACE}.
 * Modelling that faithfully is what lets a test assert the invariant that matters: saving twice
 * leaves exactly one row, not two.
 */
public class FakeGoalDao implements GoalDao {

    private final Map<Integer, Goal> goalsByUserId = new HashMap<>();
    private int nextId = 1;

    @Override
    public long upsert(Goal goal) {
        int id = nextId++;
        goalsByUserId.put(goal.getUserId(),
                new Goal(id, goal.getUserId(), goal.getGoalWeight(), goal.getPhone()));
        return id;
    }

    @Override
    public LiveData<Goal> observeForUser(int userId) {
        // Never mutated -- see FakeWeightDao for the reasoning.
        return new MutableLiveData<>();
    }

    @Override
    public Goal findForUser(int userId) {
        return goalsByUserId.get(userId);
    }

    // ---- Test inspection ----------------------------------------------------------------

    public int size() {
        return goalsByUserId.size();
    }
}
