package com.christopher.weighttracker.domain;

/**
 * A value that is either a success carrying a {@code T}, or a failure carrying an
 * {@link AppError}.
 *
 * <p><b>Why this type exists.</b> A username already being taken, or a weight field containing
 * letters, are <i>expected outcomes</i> of normal use — not bugs. Modelling them as thrown
 * exceptions makes them easy to ignore (nothing forces a caller to catch a
 * {@code RuntimeException}) and expensive to construct (stack trace capture). In this codebase
 * exceptions are reserved for genuine programming errors, and everything a user can trigger by
 * typing comes back as a {@code Result}.
 *
 * <p><b>Design note.</b> This is a single final class with static factories rather than a
 * {@code Success}/{@code Failure} hierarchy. Java 11 has no sealed types, so a hierarchy would
 * force {@code instanceof} plus a cast at every call site while giving no exhaustiveness
 * checking in return. Two states are cheaply represented by one nullable field.
 *
 * <p><b>Contract.</b> Callers must branch on {@link #isSuccess()}. Do <i>not</i> branch on
 * {@code getOrNull() == null} — a success is permitted to carry a null value (see the
 * {@code Result<Void>} returned by save operations), so null does not imply failure.
 *
 * @param <T> the type carried on success
 */
public final class Result<T> {

    private final T value;
    private final AppError error;   // null if and only if this is a success

    private Result(T value, AppError error) {
        this.value = value;
        this.error = error;
    }

    /** Creates a success carrying {@code value}, which may be null. */
    public static <T> Result<T> success(T value) {
        return new Result<>(value, null);
    }

    /**
     * Creates a failure carrying {@code error}.
     *
     * @throws IllegalArgumentException if {@code error} is null. A failure with no error would be
     *         indistinguishable from a success, which is a programming error rather than an
     *         expected outcome — so this one genuinely does throw.
     */
    public static <T> Result<T> failure(AppError error) {
        if (error == null) {
            throw new IllegalArgumentException("failure() requires a non-null AppError");
        }
        return new Result<>(null, error);
    }

    public boolean isSuccess() {
        return error == null;
    }

    public boolean isFailure() {
        return error != null;
    }

    /** The value on success; null on failure, and possibly null on success. */
    public T getOrNull() {
        return value;
    }

    /** The error on failure; null on success. */
    public AppError getError() {
        return error;
    }

    @Override
    public String toString() {
        return isSuccess() ? "Success(" + value + ")" : "Failure(" + error + ")";
    }
}
