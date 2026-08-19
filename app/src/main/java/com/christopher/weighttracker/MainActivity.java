package com.christopher.weighttracker;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

public class MainActivity extends AppCompatActivity {
    private EditText etUsername, etPassword;
    private TextView tvStatus;
    private DBHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = new DBHelper(this);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        tvStatus   = findViewById(R.id.tvStatus);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> {
            String u = etUsername.getText().toString();
            String p = etPassword.getText().toString();
            if (u.isEmpty() || p.isEmpty()) {
                tvStatus.setText("Please enter username and password.");
                return;
            }
            long id = db.registerUser(u, p);
            if (id == -1) {
                tvStatus.setText("Username already exists.");
            } else {
                tvStatus.setText("Registered! You can log in now.");
            }
        });

        btnLogin.setOnClickListener(v -> {
            String u = etUsername.getText().toString();
            String p = etPassword.getText().toString();
            if (u.isEmpty() || p.isEmpty()) {
                tvStatus.setText("Please enter username and password.");
                return;
            }
            int userId = db.loginUserId(u, p);
            if (userId > 0) {
                Intent i = new Intent(MainActivity.this, DashboardActivity.class);
                i.putExtra("USER_ID", userId);
                i.putExtra("USERNAME", u);
                startActivity(i);
                finish();
            } else {
                tvStatus.setText("Invalid credentials. Register if you're new.");
            }
        });
    }
}
