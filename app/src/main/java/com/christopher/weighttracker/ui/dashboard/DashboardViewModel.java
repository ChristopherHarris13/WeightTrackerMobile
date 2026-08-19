package com.christopher.weighttracker.ui.dashboard;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.christopher.weighttracker.data.local.Goal;
import com.christopher.weighttracker.data.local.WeightEntry;
import com.christopher.weighttracker.data.repository.WeightRepository;
import com.christopher.weighttracker.domain.AppError;
import com.christopher.weighttracker.domain.GoalPolicy;
import com.christopher.weighttracker.domain.Result;
import com.christopher.weighttracker.ui.Event;

import java.util.List;

/**
 * Holds the dashboard's state and its business rules.
 *
 * <p>Contains no {@code Context}, no views, and no SQL. It does not know that SMS exists — only
 * that a goal was reached and who should be told.
 *
 * <p>{@code onCleared()} is deliberately absent: there is nothing to tear down, because every
 * LiveData here is observed lifecycle-scoped by the Activity and {@code observeForever} is never
 * called. The lack of cleanup code follows from the design rather than being an omission.
 */
public class DashboardViewModel extends ViewModel {

    private final WeightRepository repository;
    private final int userId;

    private final LiveData<List<WeightEntry>> entries;
    private final LiveData<Goal> goal;
    private final LiveData<Double> latestWeight;

    private final MutableLiveData<Event<AppError>> error = new MutableLiveData<>();
    private final MutableLiveData<Event<Integer>> transientMessage = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> entrySaved = new MutableLiveData<>();
    private final MutableLiveData<Event<GoalReachedEvent>> goalReached = new MutableLiveData<>();

    public DashboardViewModel(WeightRepository repository, int userId) {
        this.repository = repository;
        this.userId = userId;

        this.entries = repository.observeEntries(userId);
        this.goal = repository.observeGoal(userId);

        // Derived rather than queried separately. The original ran a second SQL query for the
        // most recent weight, which meant "most recent" was defined twice and the two definitions
        // could disagree after a write. One query, one definition, and it updates for free.
        this.latestWeight = Transformations.map(entries,
                list -> (list == null || list.isEmpty()) ? null : list.get(0).getWeight());
    }

    public LiveData<List<WeightEntry>> getEntries() {
        return entries;
    }

    public LiveData<Goal> getGoal() {
        return goal;
    }

    public LiveData<Double> getLatestWeight() {
        return latestWeight;
    }

    public LiveData<Event<AppError>> getError() {
        return error;
    }

    /** Carries a string resource id for confirmations like "Added" or "Deleted". */
    public LiveData<Event<Integer>> getTransientMessage() {
        return transientMessage;
    }

    /** Signals that the Activity should clear the weight input field. */
    public LiveData<Event<Boolean>> getEntrySaved() {
        return entrySaved;
    }

    /** Signals that the user hit their goal and there is a number to text. */
    public LiveData<Event<GoalReachedEvent>> getGoalReached() {
        return goalReached;
    }

    // ---- Commands -----------------------------------------------------------------------

    public void addEntry(String rawDate, String rawWeight) {
        Result<String> date = com.christopher.weighttracker.domain.WeightValidator
                .validateDate(rawDate);
        if (date.isFailure()) {
            error.setValue(new Event<>(date.getError()));
            return;
        }
        Result<Double> weight = com.christopher.weighttracker.domain.WeightValidator
                .parseWeight(rawWeight);
        if (weight.isFailure()) {
            error.setValue(new Event<>(weight.getError()));
            return;
        }

        final double value = weight.getOrNull();
        repository.addEntry(userId, date.getOrNull(), value, result -> {
            if (result.isFailure()) {
                error.setValue(new Event<>(result.getError()));
                return;
            }
            transientMessage.setValue(
                    new Event<>(com.christopher.weighttracker.R.string.entry_added));
            // Tells the Activity to clear the weight field, fixing the concatenation bug where
            // the next entry appended to the leftover text.
            entrySaved.setValue(new Event<>(Boolean.TRUE));
            maybeSignalGoalReached(value);
        });
    }

    public void updateEntry(WeightEntry existing, String rawDate, String rawWeight) {
        Result<String> date = com.christopher.weighttracker.domain.WeightValidator
                .validateDate(rawDate);
        if (date.isFailure()) {
            error.setValue(new Event<>(date.getError()));
            return;
        }
        Result<Double> weight = com.christopher.weighttracker.domain.WeightValidator
                .parseWeight(rawWeight);
        if (weight.isFailure()) {
            error.setValue(new Event<>(weight.getError()));
            return;
        }

        final double value = weight.getOrNull();
        // Copy-on-write rather than mutation: DiffUtil requires the old object stay untouched.
        WeightEntry updated = existing.withDateAndWeight(date.getOrNull(), value);

        repository.updateEntry(updated, result -> {
            if (result.isFailure()) {
                error.setValue(new Event<>(result.getError()));
                return;
            }
            transientMessage.setValue(
                    new Event<>(com.christopher.weighttracker.R.string.entry_updated));
            maybeSignalGoalReached(value);
        });
    }

    public void deleteEntry(WeightEntry entry) {
        repository.deleteEntry(entry, result -> {
            if (result.isFailure()) {
                error.setValue(new Event<>(result.getError()));
                return;
            }
            transientMessage.setValue(
                    new Event<>(com.christopher.weighttracker.R.string.entry_deleted));
        });
    }

    public void saveGoal(String rawGoal, String rawPhone) {
        Result<Double> goalWeight = com.christopher.weighttracker.domain.WeightValidator
                .parseGoal(rawGoal);
        if (goalWeight.isFailure()) {
            error.setValue(new Event<>(goalWeight.getError()));
            return;
        }

        String phone = (rawPhone == null || rawPhone.trim().isEmpty()) ? null : rawPhone.trim();
        repository.saveGoal(userId, goalWeight.getOrNull(), phone, result -> {
            if (result.isFailure()) {
                error.setValue(new Event<>(result.getError()));
                return;
            }
            transientMessage.setValue(
                    new Event<>(com.christopher.weighttracker.R.string.goal_saved));
        });
    }

    /**
     * Re-applies the goal rule after the user grants SMS permission.
     *
     * <p>The inversion here is the interesting part. The Activity reports a <b>platform fact</b>
     * ("permission was granted"); the ViewModel re-evaluates the <b>business rule</b> and, if it
     * still holds, asks again. Neither side reaches into the other's territory.
     *
     * <p>Reads the goal through the repository rather than trusting {@code goal.getValue()},
     * because a just-completed save may not have propagated to the observed LiveData yet.
     */
    public void onSmsPermissionGranted() {
        Double latest = latestWeight.getValue();
        if (latest == null) {
            return;
        }
        final double value = latest;
        repository.loadGoal(userId, result -> {
            Goal currentGoal = result.getOrNull();
            if (currentGoal == null) {
                return;
            }
            signalIfGoalReached(value, currentGoal);
        });
    }

    /**
     * Decides whether to emit a goal-reached signal.
     *
     * <p><b>Called from the write callback, not from a {@code MediatorLiveData} over entries and
     * goal.</b> That distinction matters: a Mediator would re-evaluate on every emission of
     * either source, which includes the replay that happens when a rotated Activity re-subscribes
     * — so the alert would fire again on every rotation. <b>One-shot events are triggered by
     * commands, not by state emissions.</b>
     *
     * <p>Reading {@code goal.getValue()} is safe here because the Activity is actively observing
     * it; Room-backed LiveData holds null until something subscribes.
     */
    private void maybeSignalGoalReached(double latest) {
        Goal currentGoal = goal.getValue();
        if (currentGoal == null) {
            return;
        }
        signalIfGoalReached(latest, currentGoal);
    }

    private void signalIfGoalReached(double latest, Goal currentGoal) {
        // The rule itself lives in the domain layer, where it is unit-tested including the
        // exactly-at-goal boundary.
        if (!GoalPolicy.shouldNotify(latest, currentGoal.getGoalWeight(), currentGoal.getPhone())) {
            return;
        }
        goalReached.setValue(new Event<>(new GoalReachedEvent(
                currentGoal.getPhone(), currentGoal.getGoalWeight(), latest)));
    }
}
