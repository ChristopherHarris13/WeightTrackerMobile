package com.christopher.weighttracker.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * The application's threading policy, in one place and passed explicitly.
 *
 * <p>The original app had no threading policy at all: every database call ran on the main thread
 * and simply happened to be fast enough not to trip the ANR watchdog. That stops being true the
 * moment password hashing enters the picture, since PBKDF2 at 120,000 iterations takes hundreds
 * of milliseconds by design.
 *
 * <p><b>Why a single thread rather than a pool.</b> Serialising every write makes ordering
 * deterministic — an insert always completes before the query that observes it — and removes an
 * entire category of concurrency reasoning. Nothing here is throughput-bound: a pool would be
 * faster at nothing and harder to reason about.
 *
 * <p><b>What this class does not control.</b> It governs this codebase's own imperative calls
 * only. Room's {@code LiveData} queries run on Room's internal query executor and dispatch
 * through {@code ArchTaskExecutor}; those were never scheduled here and are not ours to
 * schedule. Believing otherwise is the usual misconception about "explicit threading" in Room.
 */
public class AppExecutors {

    private final Executor io;
    private final Executor main;

    /** Production configuration: one background thread, plus the Android main looper. */
    public AppExecutors() {
        this(Executors.newSingleThreadExecutor(), new MainThreadExecutor());
    }

    /**
     * Test seam. Passing {@code Runnable::run} for both arguments makes every scheduled block run
     * inline on the calling thread, which turns the repositories into ordinary synchronous code.
     *
     * <p>This is the concrete payoff for making threading a constructor parameter instead of a
     * static utility: repository tests need no {@code InstantTaskExecutorRule}, and therefore no
     * {@code androidx.arch.core:core-testing} dependency, and they cannot flake.
     */
    public AppExecutors(Executor io, Executor main) {
        this.io = io;
        this.main = main;
    }

    /** Disk I/O, database access, and password hashing. Never the main thread. */
    public Executor io() {
        return io;
    }

    /** Delivering results back to the UI. */
    public Executor main() {
        return main;
    }

    private static class MainThreadExecutor implements Executor {
        private final Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(Runnable command) {
            handler.post(command);
        }
    }
}
