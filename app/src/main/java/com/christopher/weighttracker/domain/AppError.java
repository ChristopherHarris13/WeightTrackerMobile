package com.christopher.weighttracker.domain;

/**
 * Every expected failure this application can produce.
 *
 * <p>This is an enum rather than a {@code String message} on purpose: <b>the error is a domain
 * fact, but the wording is a UI decision.</b> Keeping them separate means this package stays
 * free of {@code R} and free of English, which is exactly what allows it to be unit-tested on
 * a plain JVM with no Android runtime.
 *
 * <p>The mapping from these constants to user-facing text lives in
 * {@code com.christopher.weighttracker.ui.ErrorMessages}.
 */
public enum AppError {

    // Credentials
    EMPTY_USERNAME,
    USERNAME_TOO_SHORT,
    EMPTY_PASSWORD,
    PASSWORD_TOO_SHORT,
    USERNAME_TAKEN,
    INVALID_CREDENTIALS,

    // Dates
    EMPTY_DATE,
    INVALID_DATE_FORMAT,
    IMPOSSIBLE_DATE,

    // Weights
    EMPTY_WEIGHT,
    INVALID_WEIGHT,
    WEIGHT_OUT_OF_RANGE,

    // Goals
    EMPTY_GOAL,
    INVALID_GOAL,
    GOAL_OUT_OF_RANGE,

    // Persistence
    STORAGE_FAILURE
}
