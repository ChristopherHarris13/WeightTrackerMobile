package com.christopher.weighttracker.domain;

/**
 * Validation and normalization for usernames and passwords.
 *
 * <p><b>Why this lives here and not in the repository.</b> The rule this codebase follows is:
 * <i>validation that needs no I/O is a pure function; validation that requires the datastore
 * lives in the repository.</i> Whether a password is long enough can be answered from the string
 * alone, so it belongs here. Whether a username is already taken cannot — only the UNIQUE index
 * knows — so that check lives in {@code UserRepository} and comes back as
 * {@link AppError#USERNAME_TAKEN}.
 *
 * <p>These methods both validate <i>and</i> normalize: the success value is the cleaned-up string
 * the rest of the app should use. Normalization happens exactly once, at this boundary.
 */
public final class CredentialsValidator {

    static final int MIN_USERNAME_LENGTH = 3;
    static final int MIN_PASSWORD_LENGTH = 6;

    private CredentialsValidator() {
        // Static-only helper; not instantiable.
    }

    /**
     * Trims and lowercases a username.
     *
     * <p><b>Behaviour change from the original app,</b> which treated {@code Chris} and
     * {@code chris} as two different accounts. Case-insensitive usernames are what users
     * actually expect, and the rewrite starts from an empty database so nothing is broken by
     * the change.
     */
    public static Result<String> validateUsername(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return Result.failure(AppError.EMPTY_USERNAME);
        }
        String normalized = raw.trim().toLowerCase(java.util.Locale.US);
        if (normalized.length() < MIN_USERNAME_LENGTH) {
            return Result.failure(AppError.USERNAME_TOO_SHORT);
        }
        return Result.success(normalized);
    }

    /**
     * Checks password length. Deliberately not trimmed — leading and trailing spaces are
     * legitimate password characters, and silently stripping them would lock a user out of an
     * account they successfully created.
     */
    public static Result<String> validatePassword(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Result.failure(AppError.EMPTY_PASSWORD);
        }
        if (raw.length() < MIN_PASSWORD_LENGTH) {
            return Result.failure(AppError.PASSWORD_TOO_SHORT);
        }
        return Result.success(raw);
    }
}
