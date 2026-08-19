package com.christopher.weighttracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity implements WeightAdapter.OnDataChanged {

    private int userId;
    private DBHelper db;
    private TextView tvWelcome, tvSummary;
    private EditText etDate, etWeight, etGoal, etPhone;
    private Button btnAdd, btnSaveGoal, btnEnableSms;
    private RecyclerView rv;
    private WeightAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        db = new DBHelper(this);

        userId = getIntent().getIntExtra("USER_ID", -1);
        String username = getIntent().getStringExtra("USERNAME");
        if (userId <= 0) finish();

        tvWelcome = findViewById(R.id.tvWelcome);
        tvSummary = findViewById(R.id.tvSummary);
        etDate = findViewById(R.id.etDate);
        etWeight = findViewById(R.id.etWeight);
        etGoal = findViewById(R.id.etGoal);
        etPhone = findViewById(R.id.etPhone);
        btnAdd = findViewById(R.id.btnAdd);
        btnSaveGoal = findViewById(R.id.btnSaveGoal);
        btnEnableSms = findViewById(R.id.btnEnableSms);
        rv = findViewById(R.id.rvWeights);

        tvWelcome.setText("Welcome, " + username);

        // Pre-fill today's date
        etDate.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()));

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WeightAdapter(this, db.getAllWeightsForUser(userId), db, this);
        rv.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> addWeight());
        btnSaveGoal.setOnClickListener(v -> saveGoal());
        btnEnableSms.setOnClickListener(v -> {
            if (!SmsUtil.hasSmsPermission(this)) {
                SmsUtil.requestSmsPermission(this);
            } else {
                Toast.makeText(this, "SMS permission already granted.", Toast.LENGTH_SHORT).show();
            }
        });

        // Load goal into fields if present
        Double g = db.getGoalForUser(userId);
        if (g != null) etGoal.setText(String.valueOf(g));
        String p = db.getPhoneForUser(userId);
        if (p != null) etPhone.setText(p);

        refreshSummary();
    }

    private void addWeight() {
        String date = etDate.getText().toString().trim();
        String w = etWeight.getText().toString().trim();
        if (date.isEmpty() || w.isEmpty()) {
            Toast.makeText(this, "Enter date and weight.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            double weight = Double.parseDouble(w);
            long id = db.addWeight(userId, date, weight);
            if (id > 0) {
                Toast.makeText(this, "Added", Toast.LENGTH_SHORT).show();
                refresh();
                // Check goal and maybe send SMS
                maybeSendSms(weight);
            } else {
                Toast.makeText(this, "Add failed", Toast.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid weight", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveGoal() {
        String gs = etGoal.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        if (gs.isEmpty()) {
            Toast.makeText(this, "Enter a goal weight.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            double g = Double.parseDouble(gs);
            db.upsertGoal(userId, g, phone.isEmpty() ? null : phone);
            Toast.makeText(this, "Goal saved", Toast.LENGTH_SHORT).show();
            refreshSummary();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid goal", Toast.LENGTH_SHORT).show();
        }
    }

    private void maybeSendSms(double latestWeight) {
        Double goal = db.getGoalForUser(userId);
        String phone = db.getPhoneForUser(userId);
        if (goal == null || phone == null || phone.trim().isEmpty()) return;

        // Trigger condition: at/under goal
        if (latestWeight <= goal) {
            if (SmsUtil.hasSmsPermission(this)) {
                SmsUtil.sendGoalReached(phone, goal, latestWeight, this);
            } else {
                Toast.makeText(this, "Grant SMS permission to send alerts.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void refreshSummary() {
        Double latest = db.getMostRecentWeight(userId);
        Double goal = db.getGoalForUser(userId);
        String text = "Latest: " + (latest == null ? "—" : latest) + "    Goal: " + (goal == null ? "—" : goal);
        tvSummary.setText(text);
    }

    @Override
    public void refresh() {
        adapter.swapCursor(db.getAllWeightsForUser(userId));
        refreshSummary();
    }

    // Handle runtime permission result cleanly
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SmsUtil.REQ_SEND_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS permission granted.", Toast.LENGTH_SHORT).show();
                // If user already met the goal, we can optionally send now:
                Double latest = db.getMostRecentWeight(userId);
                if (latest != null) maybeSendSms(latest);
            } else {
                Toast.makeText(this, "SMS permission denied. App continues without SMS.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
