package com.christopher.weighttracker;

import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.content.ContentValues;
import android.database.Cursor;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "weighttracker.db";
    private static final int DB_VERSION = 1;

    // Tables
    public static final String T_USERS = "users";
    public static final String T_WEIGHTS = "weights";
    public static final String T_GOALS = "goals";

    public DBHelper(Context context) { super(context, DB_NAME, null, DB_VERSION); }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Users
        db.execSQL("CREATE TABLE " + T_USERS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT UNIQUE NOT NULL," +
                "password TEXT NOT NULL)");

        // Weights
        db.execSQL("CREATE TABLE " + T_WEIGHTS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER NOT NULL," +
                "date_text TEXT NOT NULL," +       // e.g., 2025-10-24
                "weight REAL NOT NULL," +
                "FOREIGN KEY(user_id) REFERENCES " + T_USERS + "(id) ON DELETE CASCADE)");

        // Goal (one per user)
        db.execSQL("CREATE TABLE " + T_GOALS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER UNIQUE NOT NULL," +
                "goal REAL NOT NULL," +
                "phone TEXT," +                    // optional for SMS
                "FOREIGN KEY(user_id) REFERENCES " + T_USERS + "(id) ON DELETE CASCADE)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS " + T_GOALS);
        db.execSQL("DROP TABLE IF EXISTS " + T_WEIGHTS);
        db.execSQL("DROP TABLE IF EXISTS " + T_USERS);
        onCreate(db);
    }

    // ---------- Users ----------
    public long registerUser(String username, String password) {
        ContentValues cv = new ContentValues();
        cv.put("username", username.trim());
        cv.put("password", password); // For class project. (In real apps, store a hash.)
        return getWritableDatabase().insert(T_USERS, null, cv);
    }

    public int loginUserId(String username, String password) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id FROM " + T_USERS + " WHERE username=? AND password=?",
                new String[]{username.trim(), password});
        int uid = -1;
        if (c.moveToFirst()) uid = c.getInt(0);
        c.close();
        return uid;
    }

    // ---------- Weights (CRUD) ----------
    public long addWeight(int userId, String dateText, double weight) {
        ContentValues cv = new ContentValues();
        cv.put("user_id", userId);
        cv.put("date_text", dateText);
        cv.put("weight", weight);
        return getWritableDatabase().insert(T_WEIGHTS, null, cv);
    }

    public Cursor getAllWeightsForUser(int userId) {
        return getReadableDatabase().rawQuery(
                "SELECT id, date_text, weight FROM " + T_WEIGHTS +
                        " WHERE user_id=? ORDER BY date_text DESC",
                new String[]{String.valueOf(userId)});
    }

    public int updateWeight(int id, String newDate, double newWeight) {
        ContentValues cv = new ContentValues();
        cv.put("date_text", newDate);
        cv.put("weight", newWeight);
        return getWritableDatabase().update(T_WEIGHTS, cv, "id=?", new String[]{String.valueOf(id)});
    }

    public int deleteWeight(int id) {
        return getWritableDatabase().delete(T_WEIGHTS, "id=?", new String[]{String.valueOf(id)});
    }

    // ---------- Goal ----------
    public void upsertGoal(int userId, double goal, String phone) {
        // Try update
        ContentValues cv = new ContentValues();
        cv.put("goal", goal);
        cv.put("phone", phone);
        int rows = getWritableDatabase().update(T_GOALS, cv, "user_id=?", new String[]{String.valueOf(userId)});
        if (rows == 0) {
            cv.put("user_id", userId);
            getWritableDatabase().insert(T_GOALS, null, cv);
        }
    }

    public Double getGoalForUser(int userId) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT goal FROM " + T_GOALS + " WHERE user_id=?",
                new String[]{String.valueOf(userId)});
        Double g = null;
        if (c.moveToFirst()) g = c.getDouble(0);
        c.close();
        return g;
    }

    public String getPhoneForUser(int userId) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT phone FROM " + T_GOALS + " WHERE user_id=?",
                new String[]{String.valueOf(userId)});
        String p = null;
        if (c.moveToFirst()) p = c.getString(0);
        c.close();
        return p;
    }

    public Double getMostRecentWeight(int userId) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT weight FROM " + T_WEIGHTS + " WHERE user_id=? ORDER BY date_text DESC LIMIT 1",
                new String[]{String.valueOf(userId)});
        Double w = null;
        if (c.moveToFirst()) w = c.getDouble(0);
        c.close();
        return w;
    }
}
