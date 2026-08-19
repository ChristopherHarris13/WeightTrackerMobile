package com.christopher.weighttracker.ui.login;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.christopher.weighttracker.data.repository.UserRepository;

/**
 * Supplies {@link LoginViewModel} with its repository.
 *
 * <p>The framework insists on constructing ViewModels itself so it can survive them across
 * configuration changes, which rules out simply calling {@code new}. A factory is how a
 * constructor dependency gets through that. The result is that {@code LoginViewModel} declares
 * exactly what it needs and receives it, instead of reaching out to a static or a service
 * locator.
 *
 * <p>Deliberately one factory per ViewModel rather than one shared factory with an
 * {@code if/else} chain over {@code modelClass}. A combined factory would have to hold every
 * dependency any ViewModel might want, which means advertising dependencies that most of its
 * products do not use — a class that misrepresents itself. Two small honest classes beat one
 * larger dishonest one.
 */
public class LoginViewModelFactory implements ViewModelProvider.Factory {

    private final UserRepository repository;

    public LoginViewModelFactory(UserRepository repository) {
        this.repository = repository;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(LoginViewModel.class)) {
            return (T) new LoginViewModel(repository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
