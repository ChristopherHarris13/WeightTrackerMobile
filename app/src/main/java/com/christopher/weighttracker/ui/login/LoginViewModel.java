package com.christopher.weighttracker.ui.login;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.christopher.weighttracker.data.repository.UserRepository;
import com.christopher.weighttracker.domain.AppError;
import com.christopher.weighttracker.domain.CredentialsValidator;
import com.christopher.weighttracker.domain.LoggedInUser;
import com.christopher.weighttracker.domain.Result;
import com.christopher.weighttracker.ui.Event;

/**
 * Holds the login screen's state and orchestrates registration and sign-in.
 *
 * <p>Its job is to translate UI text into domain commands and domain outcomes back into
 * observable state. Notice what it does not contain: no SQL, no hashing, no {@code Context}, no
 * views. It also has no {@code onCleared()} body, because nothing here needs cleaning up —
 * everything is observed lifecycle-scoped by the Activity, and {@code observeForever} is never
 * used. The absence of teardown code is a consequence of that choice, not an oversight.
 */
public class LoginViewModel extends ViewModel {

    private final UserRepository repository;

    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    private final MutableLiveData<Event<AppError>> error = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> registered = new MutableLiveData<>();
    private final MutableLiveData<Event<LoggedInUser>> loginSuccess = new MutableLiveData<>();

    public LoginViewModel(UserRepository repository) {
        this.repository = repository;
    }

    /**
     * True while a registration or login is in flight.
     *
     * <p><b>This flag exists because the rewrite created a bug that the original could not
     * have had.</b> Previously {@code db.loginUserId()} blocked the main thread, so the UI was
     * physically unable to respond to a second tap while a query ran. Now that hashing takes
     * 200 ms or more on a background thread, the buttons stay live and responsive — and a
     * double-tap fires two registrations, or two logins, concurrently.
     *
     * <p>Moving work off the main thread is unambiguously the right change, but it removed an
     * accidental safeguard, and the replacement has to be deliberate. Guarded at both ends: the
     * methods below early-return, and the Activity disables the buttons.
     */
    public LiveData<Boolean> isBusy() {
        return busy;
    }

    public LiveData<Event<AppError>> getError() {
        return error;
    }

    /** Fires once after a successful registration. */
    public LiveData<Event<Boolean>> getRegistered() {
        return registered;
    }

    /** Fires once after a successful login, carrying the identity to hand to the dashboard. */
    public LiveData<Event<LoggedInUser>> getLoginSuccess() {
        return loginSuccess;
    }

    /**
     * Validates input and creates an account.
     *
     * <p>Format rules run here as pure functions; the only check delegated to the repository is
     * whether the username is already taken, because that is the one question that genuinely
     * requires the datastore to answer.
     */
    public void register(String rawUsername, String rawPassword) {
        if (Boolean.TRUE.equals(busy.getValue())) {
            return;
        }

        Result<String> username = CredentialsValidator.validateUsername(rawUsername);
        if (username.isFailure()) {
            error.setValue(new Event<>(username.getError()));
            return;
        }
        Result<String> password = CredentialsValidator.validatePassword(rawPassword);
        if (password.isFailure()) {
            error.setValue(new Event<>(password.getError()));
            return;
        }

        busy.setValue(true);
        repository.register(username.getOrNull(), password.getOrNull(), result -> {
            busy.setValue(false);
            if (result.isFailure()) {
                error.setValue(new Event<>(result.getError()));
            } else {
                registered.setValue(new Event<>(Boolean.TRUE));
            }
        });
    }

    /**
     * Validates input and signs in.
     *
     * <p>Only emptiness is checked on the password here, deliberately — not the minimum length.
     * Telling someone at the login screen that their password is "too short" would be both
     * useless (they cannot change what they already typed into something valid by learning this)
     * and a small information leak about the password policy. Sign-in has exactly one failure
     * message.
     */
    public void login(String rawUsername, String rawPassword) {
        if (Boolean.TRUE.equals(busy.getValue())) {
            return;
        }

        Result<String> username = CredentialsValidator.validateUsername(rawUsername);
        if (username.isFailure()) {
            error.setValue(new Event<>(username.getError()));
            return;
        }
        if (rawPassword == null || rawPassword.isEmpty()) {
            error.setValue(new Event<>(AppError.EMPTY_PASSWORD));
            return;
        }

        final String normalized = username.getOrNull();
        busy.setValue(true);
        repository.login(normalized, rawPassword, result -> {
            busy.setValue(false);
            if (result.isFailure()) {
                error.setValue(new Event<>(result.getError()));
            } else {
                loginSuccess.setValue(
                        new Event<>(new LoggedInUser(result.getOrNull(), normalized)));
            }
        });
    }
}
