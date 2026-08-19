package com.christopher.weighttracker.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.SmsManager;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.christopher.weighttracker.R;
import com.christopher.weighttracker.ui.dashboard.GoalReachedEvent;
import com.christopher.weighttracker.util.WeightFormatter;

/**
 * Sends the goal-reached text message.
 *
 * <p>This was {@code SmsUtil}, and the move is itself part of the point: <b>it was never a
 * utility.</b> A "util" package is where things go when nobody has decided what layer they belong
 * to. This class wraps a platform service, needs a {@code Context}, and shows a {@code Toast} —
 * it is a UI-layer adapter, and putting it in {@code ui} says so.
 *
 * <p>The layering rule it enforces: the decision to notify is made in
 * {@code domain.GoalPolicy}, and the effect happens here. Neither knows about the other.
 */
public final class SmsNotifier {

    private SmsNotifier() {
        // Static-only helper; not instantiable.
    }

    /** Whether the app currently holds {@code SEND_SMS}. */
    public static boolean isGranted(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Sends the congratulations message, reporting success or failure with a Toast.
     *
     * <p>Callers must check {@link #isGranted} first; without the permission this throws a
     * {@code SecurityException}, which is caught below and surfaced rather than crashing.
     */
    public static void sendGoalReached(Context context, GoalReachedEvent event) {
        try {
            String message = context.getString(R.string.sms_goal_reached,
                    WeightFormatter.format(event.getGoalWeight()),
                    WeightFormatter.format(event.getCurrentWeight()));

            resolveSmsManager(context)
                    .sendTextMessage(event.getPhone(), null, message, null, null);

            Toast.makeText(context, R.string.sms_sent, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            // Sending can fail for reasons entirely outside the app's control: no SIM, no
            // service, a malformed number. None of those are bugs, and none should crash.
            Toast.makeText(context,
                    context.getString(R.string.sms_failed, String.valueOf(e.getMessage())),
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Obtains an {@code SmsManager}, avoiding the deprecated static accessor where possible.
     *
     * <p>{@code SmsManager.getDefault()} has been deprecated since API 31 in favour of
     * {@code getSystemService}. Since {@code minSdk} is 24, both paths have to exist.
     *
     * <p>This is the same shape as the PBKDF2 problem in
     * {@code data.security.Pbkdf2PasswordHasher}: a capability whose availability varies by API
     * level, handled with an explicit runtime branch rather than by pretending the minimum SDK is
     * higher than it is. Two instances of one pattern in an app this size is a good illustration
     * of how routine the situation is.
     */
    @SuppressWarnings("deprecation")
    private static SmsManager resolveSmsManager(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return context.getSystemService(SmsManager.class);
        }
        return SmsManager.getDefault();
    }
}
