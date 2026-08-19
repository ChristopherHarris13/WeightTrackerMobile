package com.christopher.weighttracker.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CredentialsValidatorTest {

    @Test
    public void acceptsSimpleUsername() {
        Result<String> result = CredentialsValidator.validateUsername("chris");
        assertTrue(result.isSuccess());
        assertEquals("chris", result.getOrNull());
    }

    @Test
    public void trimsUsername() {
        assertEquals("chris", CredentialsValidator.validateUsername("  chris  ").getOrNull());
    }

    /**
     * Behaviour change from the original app, which let {@code Chris} and {@code chris} exist as
     * two separate accounts. Normalising here means it happens exactly once, at the boundary,
     * rather than being re-derived at every call site.
     */
    @Test
    public void lowercasesUsername() {
        assertEquals("chris", CredentialsValidator.validateUsername("ChRiS").getOrNull());
    }

    @Test
    public void rejectsEmptyUsername() {
        assertEquals(AppError.EMPTY_USERNAME, CredentialsValidator.validateUsername("").getError());
    }

    @Test
    public void rejectsWhitespaceOnlyUsername() {
        assertEquals(AppError.EMPTY_USERNAME,
                CredentialsValidator.validateUsername("   ").getError());
    }

    @Test
    public void rejectsNullUsername() {
        assertEquals(AppError.EMPTY_USERNAME,
                CredentialsValidator.validateUsername(null).getError());
    }

    @Test
    public void rejectsShortUsername() {
        assertEquals(AppError.USERNAME_TOO_SHORT,
                CredentialsValidator.validateUsername("ab").getError());
    }

    /** A username that is only long enough before trimming must still be rejected. */
    @Test
    public void rejectsUsernameThatIsShortAfterTrimming() {
        assertEquals(AppError.USERNAME_TOO_SHORT,
                CredentialsValidator.validateUsername("  ab  ").getError());
    }

    @Test
    public void acceptsPasswordAtMinimumLength() {
        assertTrue(CredentialsValidator.validatePassword("123456").isSuccess());
    }

    @Test
    public void rejectsEmptyPassword() {
        assertEquals(AppError.EMPTY_PASSWORD, CredentialsValidator.validatePassword("").getError());
    }

    @Test
    public void rejectsShortPassword() {
        assertEquals(AppError.PASSWORD_TOO_SHORT,
                CredentialsValidator.validatePassword("12345").getError());
    }

    /**
     * Passwords are deliberately not trimmed. Spaces are legitimate password characters, and
     * silently stripping them would lock a user out of an account they successfully created.
     */
    @Test
    public void preservesPasswordWhitespace() {
        Result<String> result = CredentialsValidator.validatePassword("  pass  ");
        assertTrue(result.isSuccess());
        assertEquals("  pass  ", result.getOrNull());
    }
}
