package com.christopher.weighttracker.domain;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Validation and parsing for the values typed into the dashboard: dates, weights, and goals.
 *
 * <p>Pure functions with no I/O and no Android dependencies, so every rule below is directly
 * unit-testable on the JVM.
 */
public final class WeightValidator {

    /** Lowest accepted weight, exclusive. */
    static final double MIN_WEIGHT = 0.0;

    /** Highest accepted weight, inclusive. Above any recorded human body weight. */
    static final double MAX_WEIGHT = 1500.0;

    /**
     * Up to four digits, optionally followed by a decimal point and one or two digits.
     *
     * <p><b>This regex is load-bearing, not decoration.</b> {@link Double#parseDouble} is far more
     * permissive than it looks: it accepts a trailing type suffix, so {@code "183f"} parses
     * happily as {@code 183.0}. It also accepts {@code "NaN"}, {@code "Infinity"}, hex float
     * literals such as {@code "0x1p3"}, and leading {@code +}. A bare try/catch around
     * {@code parseDouble} is therefore <i>not</i> sufficient validation — the shape has to be
     * checked first.
     */
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^\\d{1,4}(\\.\\d{1,2})?$");

    /** Strict zero-padded ISO-8601 calendar date. */
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    private WeightValidator() {
        // Static-only helper; not instantiable.
    }

    /**
     * Validates a {@code yyyy-MM-dd} date string.
     *
     * <p><b>Why the format matters beyond looking tidy.</b> Entries are ordered with
     * {@code ORDER BY date_text DESC} — a lexicographic sort over text. That produces correct
     * chronological order <i>only</i> because the strings are zero-padded ISO-8601. Let a
     * {@code 2026-8-5} through and it sorts after {@code 2026-12-01}, silently. Enforcing the
     * format is therefore a data-integrity concern, not a cosmetic one.
     */
    public static Result<String> validateDate(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return Result.failure(AppError.EMPTY_DATE);
        }
        String trimmed = raw.trim();
        if (!DATE_PATTERN.matcher(trimmed).matches()) {
            return Result.failure(AppError.INVALID_DATE_FORMAT);
        }
        // Shape is right; now confirm the date actually exists. Non-lenient parsing rejects
        // 2025-02-30, which the regex alone would happily accept.
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        format.setLenient(false);
        try {
            format.parse(trimmed);
        } catch (ParseException e) {
            return Result.failure(AppError.IMPOSSIBLE_DATE);
        }
        return Result.success(trimmed);
    }

    /** Parses and range-checks a body weight. */
    public static Result<Double> parseWeight(String raw) {
        return parseNumber(raw, AppError.EMPTY_WEIGHT, AppError.INVALID_WEIGHT,
                AppError.WEIGHT_OUT_OF_RANGE);
    }

    /** Parses and range-checks a goal weight. Same rules as {@link #parseWeight}. */
    public static Result<Double> parseGoal(String raw) {
        return parseNumber(raw, AppError.EMPTY_GOAL, AppError.INVALID_GOAL,
                AppError.GOAL_OUT_OF_RANGE);
    }

    private static Result<Double> parseNumber(String raw, AppError empty, AppError invalid,
                                              AppError outOfRange) {
        if (raw == null || raw.trim().isEmpty()) {
            return Result.failure(empty);
        }
        String trimmed = raw.trim();
        if (!NUMBER_PATTERN.matcher(trimmed).matches()) {
            return Result.failure(invalid);
        }
        double parsed = Double.parseDouble(trimmed);
        if (parsed <= MIN_WEIGHT || parsed > MAX_WEIGHT) {
            return Result.failure(outOfRange);
        }
        return Result.success(parsed);
    }
}
