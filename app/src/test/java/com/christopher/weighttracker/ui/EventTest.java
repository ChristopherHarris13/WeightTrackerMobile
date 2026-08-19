package com.christopher.weighttracker.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * These tests run on a plain JVM with no Robolectric and no {@code InstantTaskExecutorRule},
 * because {@link Event} has no Android dependencies. That was one of the reasons for choosing it
 * over {@code SingleLiveEvent}.
 */
public class EventTest {

    @Test
    public void content_isReturnedOnFirstCall() {
        Event<String> event = new Event<>("payload");
        assertEquals("payload", event.getContentIfNotHandled());
    }

    /**
     * The whole point of the class. In production this second call is a rotated Activity
     * re-subscribing and receiving the replayed value; returning null is what stops a second SMS
     * from being sent.
     */
    @Test
    public void content_isNullOnEverySubsequentCall() {
        Event<String> event = new Event<>("payload");
        event.getContentIfNotHandled();

        assertNull(event.getContentIfNotHandled());
        assertNull(event.getContentIfNotHandled());
    }

    @Test
    public void peek_doesNotConsume() {
        Event<String> event = new Event<>("payload");

        assertEquals("payload", event.peekContent());
        assertFalse(event.isHandled());
        assertEquals("payload", event.getContentIfNotHandled());
    }

    @Test
    public void isHandled_reflectsConsumption() {
        Event<String> event = new Event<>("payload");
        assertFalse(event.isHandled());

        event.getContentIfNotHandled();
        assertTrue(event.isHandled());
    }
}
