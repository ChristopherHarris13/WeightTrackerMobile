package com.christopher.weighttracker.data.local;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * A user's target weight, and optionally a phone number to text when they reach it.
 *
 * <p>At most one row per user, enforced by the unique index on {@code user_id}. That constraint is
 * what makes {@code GoalDao.upsert} work — see the reasoning there.
 *
 * <p>The Java field is {@code goalWeight} while the column stays {@code goal}: the column name is
 * inherited from the original schema, but {@code goal.getGoal()} reads poorly at call sites.
 * {@code @ColumnInfo} absorbs the difference.
 */
@Entity(tableName = "goals",
        foreignKeys = @ForeignKey(entity = User.class,
                parentColumns = "id",
                childColumns = "user_id",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index(value = "user_id", unique = true)})
public class Goal {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private final int id;

    @ColumnInfo(name = "user_id")
    private final int userId;

    @ColumnInfo(name = "goal")
    private final double goalWeight;

    /** Optional. Null or blank means goal alerts are effectively disabled. */
    @Nullable
    @ColumnInfo(name = "phone")
    private final String phone;

    public Goal(int id, int userId, double goalWeight, @Nullable String phone) {
        this.id = id;
        this.userId = userId;
        this.goalWeight = goalWeight;
        this.phone = phone;
    }

    /** A goal that has not been inserted yet; id 0 tells Room to autogenerate one. */
    public static Goal forNewRow(int userId, double goalWeight, @Nullable String phone) {
        return new Goal(0, userId, goalWeight, phone);
    }

    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public double getGoalWeight() {
        return goalWeight;
    }

    @Nullable
    public String getPhone() {
        return phone;
    }
}
