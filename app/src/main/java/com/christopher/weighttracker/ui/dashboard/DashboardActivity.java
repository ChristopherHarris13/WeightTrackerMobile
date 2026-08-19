package com.christopher.weighttracker.ui.dashboard;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.christopher.weighttracker.R;
import com.christopher.weighttracker.WeightTrackerApp;
import com.christopher.weighttracker.data.local.Goal;
import com.christopher.weighttracker.data.local.WeightEntry;
import com.christopher.weighttracker.domain.AppError;
import com.christopher.weighttracker.ui.ErrorMessages;
import com.christopher.weighttracker.ui.SmsNotifier;
import com.christopher.weighttracker.util.WeightFormatter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * The weight tracking screen.
 *
 * <p>What survives from the original is only what genuinely needs an Activity: view lookup,
 * dialogs, permissions, and Toasts. Everything else moved behind {@link DashboardViewModel}.
 *
 * <p>Most visibly absent is any notion of refreshing. The original called {@code refresh()} and
 * {@code refreshSummary()} after every write — hand-rolled cache invalidation. Now the list and
 * the summary are observers of Room-backed LiveData, so a write updates them as a side effect of
 * being a write.
 */
public class DashboardActivity extends AppCompatActivity
        implements WeightAdapter.OnEntryInteraction {

    private static final String EXTRA_USER_ID = "com.christopher.weighttracker.USER_ID";
    private static final String EXTRA_USERNAME = "com.christopher.weighttracker.USERNAME";

    private DashboardViewModel viewModel;
    private WeightAdapter adapter;

    private TextView tvWelcome;
    private TextView tvSummary;
    private EditText etDate;
    private EditText etWeight;
    private EditText etGoal;
    private EditText etPhone;

    /**
     * Guards against the reactive-UI hazard described in {@link #observeViewModel()}: the goal
     * fields are populated from the first emission only, never from later ones.
     */
    private boolean goalFieldsPrefilled = false;

    /**
     * Replaces the stringly-typed {@code "USER_ID"} / {@code "USERNAME"} extras the original used.
     * The keys are private, and callers get a signature the compiler can check.
     */
    public static Intent createIntent(Context context, int userId, String username) {
        Intent intent = new Intent(context, DashboardActivity.class);
        intent.putExtra(EXTRA_USER_ID, userId);
        intent.putExtra(EXTRA_USERNAME, username);
        return intent;
    }

    /**
     * Registered as a field initializer, not lazily: the Activity Result API requires
     * registration to happen before {@code onStart}. This replaces
     * {@code onRequestPermissionsResult} and the {@code REQ_SEND_SMS = 1001} magic number, both
     * of which are deprecated.
     */
    private final ActivityResultLauncher<String> requestSmsPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    // Report a platform fact; let the ViewModel re-apply the business rule.
                    viewModel.onSmsPermissionGranted();
                } else {
                    Toast.makeText(this, R.string.sms_permission_denied, Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        int userId = getIntent().getIntExtra(EXTRA_USER_ID, -1);
        String username = getIntent().getStringExtra(EXTRA_USERNAME);

        // The original wrote `if (userId <= 0) finish();` with no `return`, so onCreate carried
        // on and ran every query against an invalid id. Validating before the ViewModel is even
        // constructed means a bad id cannot reach the rest of the app at all.
        if (userId <= 0) {
            finish();
            return;
        }

        bindViews();

        WeightTrackerApp app = (WeightTrackerApp) getApplication();
        viewModel = new ViewModelProvider(this,
                new DashboardViewModelFactory(app.getContainer().getWeightRepository(), userId))
                .get(DashboardViewModel.class);

        tvWelcome.setText(getString(R.string.welcome_format, username));
        etDate.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()));

        RecyclerView rvWeights = findViewById(R.id.rvWeights);
        rvWeights.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WeightAdapter(this);
        rvWeights.setAdapter(adapter);

        findViewById(R.id.btnAdd).setOnClickListener(v -> viewModel.addEntry(
                etDate.getText().toString(), etWeight.getText().toString()));

        findViewById(R.id.btnSaveGoal).setOnClickListener(v -> viewModel.saveGoal(
                etGoal.getText().toString(), etPhone.getText().toString()));

        Button btnEnableSms = findViewById(R.id.btnEnableSms);
        btnEnableSms.setOnClickListener(v -> {
            if (SmsNotifier.isGranted(this)) {
                Toast.makeText(this, R.string.sms_permission_granted, Toast.LENGTH_SHORT).show();
            } else {
                requestSmsPermission.launch(Manifest.permission.SEND_SMS);
            }
        });

        observeViewModel();
    }

    private void bindViews() {
        tvWelcome = findViewById(R.id.tvWelcome);
        tvSummary = findViewById(R.id.tvSummary);
        etDate = findViewById(R.id.etDate);
        etWeight = findViewById(R.id.etWeight);
        etGoal = findViewById(R.id.etGoal);
        etPhone = findViewById(R.id.etPhone);
    }

    private void observeViewModel() {
        viewModel.getEntries().observe(this, entries -> adapter.submitList(entries));

        viewModel.getLatestWeight().observe(this, latest -> renderSummary());

        viewModel.getGoal().observe(this, goal -> {
            renderSummary();

            // The reactive-UI hazard.
            //
            // The original set these fields once in onCreate. Naively writing them on every
            // emission looks equivalent but is not: saving a goal causes Room to re-emit, which
            // would overwrite whatever the user is typing at that moment -- moving their cursor
            // and discarding their edit. Prefilling from the first emission only preserves the
            // original behaviour while still picking up the stored values asynchronously.
            if (goal != null && !goalFieldsPrefilled) {
                goalFieldsPrefilled = true;
                etGoal.setText(WeightFormatter.format(goal.getGoalWeight()));
                if (goal.getPhone() != null) {
                    etPhone.setText(goal.getPhone());
                }
            }
        });

        viewModel.getError().observe(this, event -> {
            AppError error = event.getContentIfNotHandled();
            if (error == null) {
                return;
            }
            Toast.makeText(this, ErrorMessages.messageFor(error), Toast.LENGTH_SHORT).show();
        });

        viewModel.getTransientMessage().observe(this, event -> {
            Integer messageRes = event.getContentIfNotHandled();
            if (messageRes == null) {
                return;
            }
            Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show();
        });

        // Fixes the concatenation bug: without this the next weight typed appends to the old one.
        viewModel.getEntrySaved().observe(this, event -> {
            if (event.getContentIfNotHandled() == null) {
                return;
            }
            etWeight.setText("");
        });

        viewModel.getGoalReached().observe(this, event -> {
            GoalReachedEvent goalReached = event.getContentIfNotHandled();
            if (goalReached == null) {
                // Already sent. This is a configuration-change replay, and returning here is
                // exactly what stops a second SMS going out on every rotation.
                return;
            }
            if (SmsNotifier.isGranted(this)) {
                SmsNotifier.sendGoalReached(this, goalReached);
            } else {
                Toast.makeText(this, R.string.sms_needs_permission, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void renderSummary() {
        tvSummary.setText(getString(R.string.summary_format,
                WeightFormatter.formatOrDash(viewModel.getLatestWeight().getValue()),
                formatGoal(viewModel.getGoal().getValue())));
    }

    private String formatGoal(Goal goal) {
        return goal == null
                ? WeightFormatter.EMPTY_PLACEHOLDER
                : WeightFormatter.format(goal.getGoalWeight());
    }

    // ---- WeightAdapter.OnEntryInteraction ------------------------------------------------

    @Override
    public void onEntryClicked(WeightEntry entry) {
        showEditDialog(entry);
    }

    /**
     * Confirms before deleting.
     *
     * <p>The original deleted immediately on long-press, with no confirmation and no undo — a
     * destructive action one accidental press away, on rows the user is invited to tap.
     */
    @Override
    public void onEntryLongClicked(WeightEntry entry) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_title)
                .setMessage(getString(R.string.dialog_delete_message,
                        WeightFormatter.format(entry.getWeight()), entry.getDateText()))
                .setPositiveButton(R.string.action_delete,
                        (dialog, which) -> viewModel.deleteEntry(entry))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    /**
     * Collects an edit and hands the raw strings to the ViewModel.
     *
     * <p>Note that nothing is validated or parsed here. In the original this dialog called
     * {@code Double.parseDouble} itself and wrote to the database from inside the adapter.
     */
    private void showEditDialog(WeightEntry entry) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_weight, null);
        EditText etEditDate = view.findViewById(R.id.etEditDate);
        EditText etEditWeight = view.findViewById(R.id.etEditWeight);

        etEditDate.setText(entry.getDateText());
        etEditWeight.setText(WeightFormatter.format(entry.getWeight()));

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_edit_title)
                .setView(view)
                .setPositiveButton(R.string.action_save, (dialog, which) -> viewModel.updateEntry(
                        entry,
                        etEditDate.getText().toString(),
                        etEditWeight.getText().toString()))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }
}
