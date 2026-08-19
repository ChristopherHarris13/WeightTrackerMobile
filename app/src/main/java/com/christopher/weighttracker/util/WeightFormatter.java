package com.christopher.weighttracker.util;

import java.util.Locale;

/**
 * Formats weights for display.
 *
 * <p>Fixes a real defect: the original bound weights with {@code String.valueOf(double)}, so a
 * stored {@code 185.5183} rendered in the list exactly like that. Worse, the edit dialog
 * pre-filled from the same raw value, making the noise sticky.
 *
 * <p><b>Why {@code String.format} and not a static {@code DecimalFormat}.</b> {@code DecimalFormat}
 * is not thread-safe. A {@code static final} instance shared across threads is a classic source
 * of silent, intermittent corruption — and this project now genuinely has background threads.
 *
 * <p><b>Why the locale is pinned to US.</b> Formatting is paired with
 * {@code Double.parseDouble} on the way back in, and {@code parseDouble} only ever accepts
 * {@code .} as the decimal separator. Under a comma-decimal locale the app would render
 * {@code 185,5} and then be unable to read its own output. Pinning both ends keeps them
 * consistent. Full localisation would mean using {@code NumberFormat} symmetrically for parsing
 * too — out of scope here, but the asymmetry is the thing to notice.
 */
public final class WeightFormatter {

    /** Shown where a value is expected but none exists yet. */
    public static final String EMPTY_PLACEHOLDER = "—"; // em dash

    private WeightFormatter() {
        // Static-only helper; not instantiable.
    }

    /** Formats a weight to one decimal place, e.g. {@code 185.5}. */
    public static String format(double weight) {
        return String.format(Locale.US, "%.1f", weight);
    }

    /** Formats a weight, or returns an em dash if it is null. */
    public static String formatOrDash(Double weight) {
        return weight == null ? EMPTY_PLACEHOLDER : format(weight);
    }
}
