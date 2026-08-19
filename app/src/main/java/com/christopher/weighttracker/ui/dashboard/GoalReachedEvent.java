package com.christopher.weighttracker.ui.dashboard;

/**
 * The payload of a "user reached their goal" signal: everything the Activity needs in order to
 * compose an SMS, and nothing more.
 *
 * <p>Read it as a <b>request</b> rather than a command. The ViewModel is stating a business fact —
 * this person hit their target, here is the relevant data — and is deliberately not saying
 * "send an SMS now". Whether the app is permitted to send, and what happens if it is not, is a
 * platform question that only the Activity can answer.
 */
public class GoalReachedEvent {

    private final String phone;
    private final double goalWeight;
    private final double currentWeight;

    public GoalReachedEvent(String phone, double goalWeight, double currentWeight) {
        this.phone = phone;
        this.goalWeight = goalWeight;
        this.currentWeight = currentWeight;
    }

    public String getPhone() {
        return phone;
    }

    public double getGoalWeight() {
        return goalWeight;
    }

    public double getCurrentWeight() {
        return currentWeight;
    }
}
