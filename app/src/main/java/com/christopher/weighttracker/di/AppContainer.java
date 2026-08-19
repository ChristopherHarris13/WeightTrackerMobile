package com.christopher.weighttracker.di;

import android.content.Context;

import androidx.room.Room;

import com.christopher.weighttracker.data.local.AppDatabase;
import com.christopher.weighttracker.data.repository.UserRepository;
import com.christopher.weighttracker.data.repository.WeightRepository;
import com.christopher.weighttracker.data.security.PasswordHasher;
import com.christopher.weighttracker.data.security.Pbkdf2PasswordHasher;
import com.christopher.weighttracker.util.AppExecutors;

/**
 * The composition root: the one place where concrete implementations are chosen and wired
 * together.
 *
 * <p>Read the constructor top to bottom and you have the entire object graph of the application.
 * Everywhere else, dependencies arrive through constructors and nothing reaches out to fetch its
 * own collaborators. That single property is what makes the repositories testable with fakes and
 * what keeps the ViewModels ignorant of Room.
 *
 * <p><b>Why manual DI rather than Hilt or Dagger.</b> Those tools solve a problem this app does
 * not have: managing a graph too large and too deeply nested to hold in your head. Here the whole
 * graph is nine lines and completely legible, with no annotations, no generated code, and no
 * build-time processing to understand. Hilt would trade that legibility for scalability that is
 * not needed. At a few hundred users of this class the trade flips — knowing <i>where</i> it flips
 * is the useful part, and reaching for the framework before then is the classic over-engineering
 * mistake.
 *
 * <p><b>Why {@code AppDatabase} has no {@code getInstance()}.</b> Because this class exists.
 * Single-instance-ness is a property of how the object is <i>used</i>, so it is enforced at the
 * place that does the using rather than by every class policing itself with double-checked
 * locking.
 */
public class AppContainer {

    private final AppExecutors executors;
    private final AppDatabase database;
    private final UserRepository userRepository;
    private final WeightRepository weightRepository;

    /**
     * @param context any context; the application context is extracted from it. Holding an
     *                Activity context here would leak that Activity for the process lifetime,
     *                since this object lives as long as the app does.
     */
    public AppContainer(Context context) {
        this.executors = new AppExecutors();

        this.database = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        AppDatabase.NAME)
                // See AppDatabase's class comment: this is here to allow schema changes during
                // development without hand-written migrations, NOT to deal with the legacy
                // database file (which a different filename already handles).
                .fallbackToDestructiveMigration()
                .build();

        PasswordHasher passwordHasher = new Pbkdf2PasswordHasher();

        this.userRepository = new UserRepository(database.userDao(), passwordHasher, executors);
        this.weightRepository = new WeightRepository(
                database.weightDao(), database.goalDao(), executors);
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }

    public WeightRepository getWeightRepository() {
        return weightRepository;
    }
}
