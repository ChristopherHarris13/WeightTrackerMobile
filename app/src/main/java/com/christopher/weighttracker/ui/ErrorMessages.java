package com.christopher.weighttracker.ui;

import androidx.annotation.StringRes;

import com.christopher.weighttracker.R;
import com.christopher.weighttracker.domain.AppError;

/**
 * Translates an {@link AppError} into a string resource.
 *
 * <p>This class is the seam that keeps {@code domain} free of Android. Because errors are enum
 * constants rather than pre-formatted message strings, the domain layer never imports {@code R},
 * never hardcodes English, and stays testable on a plain JVM. All of the presentation knowledge
 * is concentrated in the switch below.
 *
 * <p>The switch is exhaustive over {@code AppError} and the {@code default} branch throws.
 * Java has no compile-time exhaustiveness checking for enum switches, so failing loudly is the
 * next best thing: adding a constant and forgetting to give it wording surfaces immediately in
 * testing rather than silently rendering a blank Toast.
 */
public final class ErrorMessages {

    private ErrorMessages() {
        // Static-only helper; not instantiable.
    }

    @StringRes
    public static int messageFor(AppError error) {
        switch (error) {
            case EMPTY_USERNAME:
                return R.string.error_empty_username;
            case USERNAME_TOO_SHORT:
                return R.string.error_username_too_short;
            case EMPTY_PASSWORD:
                return R.string.error_empty_password;
            case PASSWORD_TOO_SHORT:
                return R.string.error_password_too_short;
            case USERNAME_TAKEN:
                return R.string.error_username_taken;
            case INVALID_CREDENTIALS:
                return R.string.error_invalid_credentials;
            case EMPTY_DATE:
                return R.string.error_empty_date;
            case INVALID_DATE_FORMAT:
                return R.string.error_invalid_date_format;
            case IMPOSSIBLE_DATE:
                return R.string.error_impossible_date;
            case EMPTY_WEIGHT:
                return R.string.error_empty_weight;
            case INVALID_WEIGHT:
                return R.string.error_invalid_weight;
            case WEIGHT_OUT_OF_RANGE:
                return R.string.error_weight_out_of_range;
            case EMPTY_GOAL:
                return R.string.error_empty_goal;
            case INVALID_GOAL:
                return R.string.error_invalid_goal;
            case GOAL_OUT_OF_RANGE:
                return R.string.error_goal_out_of_range;
            case STORAGE_FAILURE:
                return R.string.error_storage_failure;
            default:
                throw new IllegalArgumentException("No message defined for AppError." + error);
        }
    }
}
