package com.christopher.weighttracker;

import android.app.Application;

import com.christopher.weighttracker.di.AppContainer;

/**
 * Application entry point. Its only job is to own the {@link AppContainer} for the lifetime of
 * the process.
 *
 * <p>Activities reach the container through
 * {@code ((WeightTrackerApp) getApplication()).getContainer()}. That cast is the one piece of
 * ceremony manual dependency injection costs, and it buys a graph with no annotations, no
 * generated code, and no framework to learn.
 *
 * <p>Requires {@code android:name=".WeightTrackerApp"} on the {@code <application>} tag in the
 * manifest — without it Android instantiates the default {@code Application} and the container
 * is never created.
 */
public class WeightTrackerApp extends Application {

    private AppContainer container;

    @Override
    public void onCreate() {
        super.onCreate();
        container = new AppContainer(this);
    }

    public AppContainer getContainer() {
        return container;
    }
}
