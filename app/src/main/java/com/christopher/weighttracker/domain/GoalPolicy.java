package com.christopher.weighttracker.domain;

/**
 * The single business rule behind the SMS alert: when should the user be congratulated?
 *
 * <p><b>Why a whole class for one boolean.</b> This logic started out inside
 * {@code DashboardActivity.maybeSendSms()}, tangled together with a permission check and a
 * {@code Toast}. That made an obvious question unanswerable: <i>does the alert fire when the
 * latest weight exactly equals the goal?</i> Reading the code, it does — but nothing proved it,
 * and nothing stopped a later edit from changing it.
 *
 * <p>Testing it in place would have required instantiating a ViewModel, which needs LiveData,
 * which needs {@code InstantTaskExecutorRule} from {@code androidx.arch.core:core-testing} — a
 * dependency this project deliberately does without. Pulling the rule out into a pure function
 * made it testable in six lines.
 *
 * <p>That sequence is the point, and it generalises: <b>the pressure to make something testable
 * revealed a seam that was worth having anyway.</b> The rule now has one home, one definition,
 * and a test pinning down the boundary case.
 */
public final class GoalPolicy {

    private GoalPolicy() {
        // Static-only helper; not instantiable.
    }

    /**
     * Decides whether a goal-reached notification is warranted.
     *
     * <p>Note what this method does <i>not</i> do: it does not check permissions, does not send
     * anything, and does not know what an SMS is. It answers a business question and nothing
     * else. Whether the app is actually able to act on the answer is a platform concern, decided
     * by the Activity.
     *
     * @param latestWeight the most recently recorded weight, or null if there are no entries
     * @param goalWeight   the user's goal, or null if none has been set
     * @param phone        the stored phone number, which is optional and may be null or blank
     * @return true if the user is at or under goal and there is somewhere to send the message
     */
    public static boolean shouldNotify(Double latestWeight, Double goalWeight, String phone) {
        if (latestWeight == null || goalWeight == null) {
            return false;
        }
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        // At-or-under, matching the original behaviour: hitting the goal exactly counts.
        return latestWeight <= goalWeight;
    }
}
