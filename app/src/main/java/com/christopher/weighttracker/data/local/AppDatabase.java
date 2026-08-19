package com.christopher.weighttracker.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;

/**
 * The Room database, replacing the hand-written {@code SQLiteOpenHelper}.
 *
 * <h2>Why the file is called {@code weight_tracker.db} and not {@code weighttracker.db}</h2>
 *
 * There is a trap here that is worth understanding, because the obvious approach fails in a
 * confusing way.
 *
 * <p>The original database file is {@code weighttracker.db} at version 1, created by
 * {@code SQLiteOpenHelper}, and it contains no {@code room_master_table} — Room's private
 * bookkeeping table holding a hash of the schema it expects. Point Room at that same filename,
 * also at version 1, and here is what happens: the versions match, so {@code onCreate} and
 * {@code onUpgrade} are both skipped and {@code onOpen} runs. {@code onOpen} calls
 * {@code checkIdentity}, which finds no master table, and so falls back to validating the live
 * schema directly against the entities. That comparison fails — the {@code users} table has a
 * {@code password} column where {@code User} declares {@code password_hash} — and Room throws
 * {@code IllegalStateException}.
 *
 * <p><b>{@code fallbackToDestructiveMigration()} does not save you.</b> It only affects the
 * {@code onUpgrade} and {@code onDowngrade} paths, and neither of those ever runs when the
 * version numbers already agree. This is the part that surprises people: the option that sounds
 * like "wipe it if anything is wrong" does nothing at all in this particular failure.
 *
 * <p>Declaring {@code version = 2} on the old filename <i>would</i> work: SQLite would see 1 → 2,
 * call {@code onUpgrade} before {@code onOpen}, and destructive fallback would drop and recreate
 * everything, writing a valid identity in the process. That is a correct fix, and understanding
 * why is the transferable part. It is not the fix used here, because it depends on a subtle
 * ordering guarantee to work.
 *
 * <p>A new filename needs no such reasoning. Room creates a fresh file, the old one is orphaned
 * on disk and harmless, and the behaviour is obvious to the next reader. It is also the more
 * honest description of what is happening: passwords are now salted hashes, so <i>every</i>
 * legacy user row is unusable regardless. This genuinely is a new datastore rather than a
 * migration of the old one.
 *
 * <h2>Why {@code fallbackToDestructiveMigration()} is still enabled</h2>
 *
 * Not for the legacy file, which is no longer involved. It is there for its real purpose during
 * ongoing development: it allows entities to change freely without hand-writing a
 * {@code Migration} for every schema tweak. Shipping to real users with data worth keeping would
 * mean replacing it with explicit migrations plus {@code MigrationTestHelper} tests — which is
 * exactly what the exported {@code app/schemas/} directory exists to enable.
 *
 * <h2>No singleton here</h2>
 *
 * There is deliberately no {@code getInstance()} with double-checked locking, despite that being
 * the shape almost every Room tutorial shows. {@code AppContainer} constructs exactly one
 * instance and hands it out, so this class has no reason to police its own lifetime. Deleting
 * boilerplate that a composition root already makes unnecessary is itself the lesson.
 */
@Database(
        entities = {User.class, WeightEntry.class, Goal.class},
        version = 1,
        exportSchema = true)
public abstract class AppDatabase extends RoomDatabase {

    /** Filename. Intentionally distinct from the legacy {@code weighttracker.db}. */
    public static final String NAME = "weight_tracker.db";

    public abstract UserDao userDao();

    public abstract WeightDao weightDao();

    public abstract GoalDao goalDao();
}
