package com.christopher.weighttracker.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * A single recorded weight on a given day.
 *
 * <p>The {@code @Index} on {@code user_id} is not decoration. Room emits a build warning for an
 * un-indexed foreign key, and it is right to: every query in the app filters by {@code user_id},
 * and a full table scan per query is a real cost as history accumulates.
 *
 * <p>Dates are stored as {@code yyyy-MM-dd} text rather than an epoch integer. That is inherited
 * from the original schema, and it works only because ordering relies on the strings being
 * zero-padded ISO-8601 — a lexicographic sort then coincides with a chronological one. That
 * invariant is enforced by {@code WeightValidator.validateDate}, which is why format validation
 * counts as data integrity here rather than presentation.
 */
@Entity(tableName = "weights",
        foreignKeys = @ForeignKey(entity = User.class,
                parentColumns = "id",
                childColumns = "user_id",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("user_id")})
public class WeightEntry {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private final int id;

    @ColumnInfo(name = "user_id")
    private final int userId;

    /** Zero-padded {@code yyyy-MM-dd}. The padding is what makes the sort correct. */
    @NonNull
    @ColumnInfo(name = "date_text")
    private final String dateText;

    @ColumnInfo(name = "weight")
    private final double weight;

    public WeightEntry(int id, int userId, @NonNull String dateText, double weight) {
        this.id = id;
        this.userId = userId;
        this.dateText = dateText;
        this.weight = weight;
    }

    /** An entry that has not been inserted yet; id 0 tells Room to autogenerate one. */
    public static WeightEntry forNewRow(int userId, @NonNull String dateText, double weight) {
        return new WeightEntry(0, userId, dateText, weight);
    }

    /**
     * Returns a copy with a new date and weight, keeping the same id and owner.
     *
     * <p>This is what replaces a setter. The edit dialog produces a new object rather than
     * mutating the one currently held by the adapter — which is precisely the guarantee
     * {@code DiffUtil} needs in order to compare old against new.
     */
    public WeightEntry withDateAndWeight(@NonNull String newDateText, double newWeight) {
        return new WeightEntry(id, userId, newDateText, newWeight);
    }

    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    @NonNull
    public String getDateText() {
        return dateText;
    }

    public double getWeight() {
        return weight;
    }
}
