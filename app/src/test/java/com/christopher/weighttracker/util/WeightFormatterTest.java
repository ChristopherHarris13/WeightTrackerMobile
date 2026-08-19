package com.christopher.weighttracker.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WeightFormatterTest {

    @Test
    public void formatsToOneDecimalPlace() {
        assertEquals("185.5", WeightFormatter.format(185.5));
    }

    @Test
    public void formatsWholeNumberWithTrailingZero() {
        assertEquals("183.0", WeightFormatter.format(183.0));
    }

    /** The defect this class fixes: the original bound this value straight to the TextView. */
    @Test
    public void roundsAwayExcessPrecision() {
        assertEquals("185.5", WeightFormatter.format(185.5183));
    }

    @Test
    public void roundsHalfUp() {
        assertEquals("185.6", WeightFormatter.format(185.55));
    }

    @Test
    public void formatOrDash_returnsDashForNull() {
        assertEquals(WeightFormatter.EMPTY_PLACEHOLDER, WeightFormatter.formatOrDash(null));
    }

    @Test
    public void formatOrDash_formatsNonNull() {
        assertEquals("183.0", WeightFormatter.formatOrDash(183.0));
    }
}
