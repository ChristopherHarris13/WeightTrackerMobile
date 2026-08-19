package com.christopher.weighttracker.ui;

/**
 * Wraps a value so that it is consumed <b>at most once</b>, however many times LiveData
 * re-delivers it.
 *
 * <p><b>The bug this exists to prevent.</b> {@code LiveData} is a state holder, not an event
 * stream: it re-delivers its most recent value to every newly attached observer. That is exactly
 * right for "the current list of weights" and exactly wrong for "send an SMS now". Rotate the
 * device after hitting your goal and the Activity is destroyed, recreated, re-subscribes,
 * immediately receives the stale value, and sends a second text message. Rotate five times, send
 * five texts. It is reproducible, user-visible, and it costs the user money.
 *
 * <p><b>Why not {@code SingleLiveEvent}.</b> The well-known alternative subclasses
 * {@code MutableLiveData} and uses an {@code AtomicBoolean} to hand each value to exactly one
 * observer. It works, but it quietly breaks the LiveData contract: attach two observers and one
 * of them nondeterministically never fires. Google's architecture samples deprecated it for that
 * reason. Its failure mode is remote and silent — the bug shows up in a class nobody edited, on
 * the day somebody adds a second observer.
 *
 * <p><b>What this does instead.</b> It leaves the delivery mechanism completely alone — every
 * observer still receives every emission, as LiveData promises. What changes is that the
 * <i>content</i> can only be taken once. The ambiguity ("has this already been dealt with?") is a
 * property of the event, so it is stored on the event. The failure mode is local and visible: a
 * misuse is someone calling {@link #peekContent()} where they meant
 * {@link #getContentIfNotHandled()}, right there in the observer body.
 *
 * <p>A third benefit falls out of that design: this class has no Android imports at all, so it is
 * unit-testable on a plain JVM. Testing {@code SingleLiveEvent} would require
 * {@code InstantTaskExecutorRule}. <b>The pattern that is easier to test is also the one that is
 * semantically honest — that correlation is not a coincidence.</b>
 *
 * <p><b>Convention:</b> the payload must be non-null, because {@link #getContentIfNotHandled()}
 * signals "already handled" by returning null. For signal-only events, carry
 * {@link Boolean#TRUE}.
 *
 * <p>Not thread-safe by design: instances are only ever touched on the main thread.
 *
 * <p>Where this idea goes next: current guidance models one-shot events as ordinary state plus an
 * explicit {@code onEventConsumed()} call back into the ViewModel (or, in Kotlin, a
 * {@code Channel}). This class is the compact Java/LiveData ancestor of that idea — same core
 * insight that consumption must be explicit, less machinery.
 *
 * @param <T> the payload type
 */
public class Event<T> {

    private final T content;
    private boolean handled = false;

    public Event(T content) {
        this.content = content;
    }

    /**
     * Returns the payload the first time it is called, and null on every call after that.
     *
     * <p>An observer that receives null should do nothing: it is seeing a replay of an event that
     * has already been acted upon.
     */
    public T getContentIfNotHandled() {
        if (handled) {
            return null;
        }
        handled = true;
        return content;
    }

    /**
     * Reads the payload without consuming it. Intended for logging and debugging only — using
     * this to drive behaviour reintroduces exactly the bug this class prevents.
     */
    public T peekContent() {
        return content;
    }

    public boolean isHandled() {
        return handled;
    }
}
