package com.christopher.weighttracker.domain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The rule these tests pin down used to live inside {@code DashboardActivity.maybeSendSms()},
 * where none of it could be verified without an emulator.
 */
public class GoalPolicyTest {

    @Test
    public void notifiesWhenBelowGoal() {
        assertTrue(GoalPolicy.shouldNotify(180.0, 190.0, "5551234567"));
    }

    /**
     * The boundary case, and the reason this class was extracted in the first place. "At or under
     * goal" was the original behaviour; this test is what keeps it from drifting to "under goal"
     * during a later edit.
     */
    @Test
    public void notifiesWhenExactlyAtGoal() {
        assertTrue(GoalPolicy.shouldNotify(190.0, 190.0, "5551234567"));
    }

    @Test
    public void doesNotNotifyWhenAboveGoal() {
        assertFalse(GoalPolicy.shouldNotify(195.0, 190.0, "5551234567"));
    }

    @Test
    public void doesNotNotifyWhenNoEntriesYet() {
        assertFalse(GoalPolicy.shouldNotify(null, 190.0, "5551234567"));
    }

    @Test
    public void doesNotNotifyWhenNoGoalSet() {
        assertFalse(GoalPolicy.shouldNotify(180.0, null, "5551234567"));
    }

    @Test
    public void doesNotNotifyWhenPhoneMissing() {
        assertFalse(GoalPolicy.shouldNotify(180.0, 190.0, null));
    }

    @Test
    public void doesNotNotifyWhenPhoneBlank() {
        assertFalse(GoalPolicy.shouldNotify(180.0, 190.0, "   "));
    }
}
