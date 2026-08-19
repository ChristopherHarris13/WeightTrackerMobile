package com.christopher.weighttracker.ui.login;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.christopher.weighttracker.R;
import com.christopher.weighttracker.WeightTrackerApp;
import com.christopher.weighttracker.domain.AppError;
import com.christopher.weighttracker.domain.LoggedInUser;
import com.christopher.weighttracker.ui.ErrorMessages;
import com.christopher.weighttracker.ui.dashboard.DashboardActivity;

/**
 * Sign-in and registration.
 *
 * <p>Renamed from {@code MainActivity}: the manifest had to change anyway, and the old name said
 * nothing about what the class does.
 *
 * <p>What remains here is only the work that genuinely requires an Activity — inflating views,
 * reading text out of them, observing, and navigating. Every rule about what makes a username
 * valid or a login successful now lives behind {@link LoginViewModel}.
 */
public class LoginActivity extends AppCompatActivity {

    private LoginViewModel viewModel;

    private EditText etUsername;
    private EditText etPassword;
    private TextView tvStatus;
    private Button btnLogin;
    private Button btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        tvStatus = findViewById(R.id.tvStatus);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        WeightTrackerApp app = (WeightTrackerApp) getApplication();
        viewModel = new ViewModelProvider(this,
                new LoginViewModelFactory(app.getContainer().getUserRepository()))
                .get(LoginViewModel.class);

        btnRegister.setOnClickListener(v -> viewModel.register(
                etUsername.getText().toString(), etPassword.getText().toString()));

        btnLogin.setOnClickListener(v -> viewModel.login(
                etUsername.getText().toString(), etPassword.getText().toString()));

        observeViewModel();
    }

    private void observeViewModel() {
        // Second half of the double-tap guard described on LoginViewModel.isBusy(). The ViewModel
        // early-returns regardless; disabling the buttons is what makes that visible to the user
        // rather than a silently ignored press.
        viewModel.isBusy().observe(this, busy -> {
            boolean enabled = !Boolean.TRUE.equals(busy);
            btnLogin.setEnabled(enabled);
            btnRegister.setEnabled(enabled);
        });

        viewModel.getError().observe(this, event -> {
            AppError error = event.getContentIfNotHandled();
            if (error == null) {
                return;   // already shown; this is a configuration-change replay
            }
            tvStatus.setText(ErrorMessages.messageFor(error));
        });

        viewModel.getRegistered().observe(this, event -> {
            if (event.getContentIfNotHandled() == null) {
                return;
            }
            tvStatus.setText(R.string.registered_success);
        });

        viewModel.getLoginSuccess().observe(this, event -> {
            LoggedInUser user = event.getContentIfNotHandled();
            if (user == null) {
                return;
            }
            // The username here is the normalized one from the ViewModel, not the raw field
            // contents -- so the dashboard greets the user by the name actually stored.
            startActivity(DashboardActivity.createIntent(this, user.getId(), user.getUsername()));
            finish();
        });
    }
}
