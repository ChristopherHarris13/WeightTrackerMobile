package com.christopher.weighttracker.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class ResultTest {

    @Test
    public void success_carriesValue() {
        Result<String> result = Result.success("ok");
        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
        assertEquals("ok", result.getOrNull());
        assertNull(result.getError());
    }

    @Test
    public void failure_carriesError() {
        Result<String> result = Result.failure(AppError.USERNAME_TAKEN);
        assertTrue(result.isFailure());
        assertFalse(result.isSuccess());
        assertEquals(AppError.USERNAME_TAKEN, result.getError());
        assertNull(result.getOrNull());
    }

    /**
     * Pins the contract documented on {@link Result}: null is not a failure signal. Code that
     * branches on {@code getOrNull() == null} instead of {@code isSuccess()} would get this wrong.
     */
    @Test
    public void success_mayCarryNullValue_andIsStillSuccess() {
        Result<Void> result = Result.success(null);
        assertTrue(result.isSuccess());
        assertNull(result.getOrNull());
        assertNull(result.getError());
    }

    /**
     * A failure with no error would be indistinguishable from a success. That is a programming
     * error rather than an expected outcome, so it is one of the few things that throws.
     */
    @Test
    public void failure_withNullError_throws() {
        try {
            Result.failure(null);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // pass
        }
    }
}
