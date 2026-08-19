package com.christopher.weighttracker.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * A registered account.
 *
 * <p><b>Note the column names.</b> They match the original hand-written SQL — {@code users},
 * {@code username} — while the Java fields use camelCase, bridged by {@code @ColumnInfo}. The
 * mismatch is deliberate: it keeps visible that the storage schema and the in-memory model are
 * two separate things that merely happen to line up. Renaming a field should not require a
 * migration, and {@code @ColumnInfo} is what makes that true.
 *
 * <p>One column <i>is</i> renamed: {@code password} became {@code password_hash}, because the old
 * name no longer describes what is stored there. Names that lie about their contents are worse
 * than no names.
 *
 * <p><b>Why immutable.</b> Private final fields and no setters, which costs about a dozen extra
 * lines per entity and buys three things. Room's {@code DiffUtil} integration requires that items
 * in an already-submitted list are never mutated — immutability guarantees that by construction
 * rather than by everyone remembering. Edits become copy-on-write, which is exactly the shape
 * {@code submitList} expects. And a named factory reads better at the call site than a
 * constructor with a magic zero for the id.
 */
@Entity(tableName = "users",
        indices = {@Index(value = "username", unique = true)})
public class User {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private final int id;

    @NonNull
    @ColumnInfo(name = "username")
    private final String username;

    /** Opaque output of {@code PasswordHasher.hash}. Never a plaintext password. */
    @NonNull
    @ColumnInfo(name = "password_hash")
    private final String passwordHash;

    /** Room uses this constructor, matching parameter names to column names at compile time. */
    public User(int id, @NonNull String username, @NonNull String passwordHash) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    /** A user that has not been inserted yet; id 0 tells Room to autogenerate one. */
    public static User forNewRow(@NonNull String username, @NonNull String passwordHash) {
        return new User(0, username, passwordHash);
    }

    public int getId() {
        return id;
    }

    @NonNull
    public String getUsername() {
        return username;
    }

    @NonNull
    public String getPasswordHash() {
        return passwordHash;
    }
}
