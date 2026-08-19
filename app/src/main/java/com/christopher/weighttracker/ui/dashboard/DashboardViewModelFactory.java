package com.christopher.weighttracker.ui.dashboard;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.christopher.weighttracker.data.repository.WeightRepository;

/**
 * Supplies {@link DashboardViewModel} with its repository and the id of the signed-in user.
 *
 * <p>Constructing it as {@code new DashboardViewModelFactory(repository, userId)} states the
 * ViewModel's complete dependency list at the one place that knows both values. That visibility
 * is the entire objective of doing dependency injection by hand.
 *
 * <p><b>Deliberately not using {@code SavedStateHandle}.</b> For an Activity, the handle is seeded
 * from the Intent extras, so it would supply {@code userId} automatically and survive process
 * death as well as configuration change. It is genuinely the more capable option and worth
 * learning next.
 *
 * <p>It is skipped here for two reasons. It hides the wiring behind a magic string key, where an
 * explicit constructor parameter shows it. And more decisively, this app has no session
 * persistence at all — killing the process returns the user to the login screen regardless of
 * what is retained. Making {@code userId} survive process death in an app whose <i>session</i>
 * does not would be solving half a problem and calling it done.
 */
public class DashboardViewModelFactory implements ViewModelProvider.Factory {

    private final WeightRepository repository;
    private final int userId;

    public DashboardViewModelFactory(WeightRepository repository, int userId) {
        this.repository = repository;
        this.userId = userId;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(DashboardViewModel.class)) {
            return (T) new DashboardViewModel(repository, userId);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
