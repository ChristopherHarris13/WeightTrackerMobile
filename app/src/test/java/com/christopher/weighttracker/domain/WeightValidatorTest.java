package com.christopher.weighttracker.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WeightValidatorTest {

    // ---- weights ------------------------------------------------------------------------

    @Test
    public void parsesDecimalWeight() {
        Result<Double> result = WeightValidator.parseWeight("183.5");
        assertTrue(result.isSuccess());
        assertEquals(183.5, result.getOrNull(), 0.0001);
    }

    @Test
    public void parsesWholeWeight() {
        Result<Double> result = WeightValidator.parseWeight("183");
        assertTrue(result.isSuccess());
        assertEquals(183.0, result.getOrNull(), 0.0001);
    }

    @Test
    public void trimsSurroundingWhitespace() {
        assertTrue(WeightValidator.parseWeight("  183.5  ").isSuccess());
    }

    @Test
    public void rejectsEmpty() {
        assertEquals(AppError.EMPTY_WEIGHT, WeightValidator.parseWeight("").getError());
    }

    @Test
    public void rejectsWhitespaceOnly() {
        assertEquals(AppError.EMPTY_WEIGHT, WeightValidator.parseWeight("   ").getError());
    }

    @Test
    public void rejectsNull() {
        assertEquals(AppError.EMPTY_WEIGHT, WeightValidator.parseWeight(null).getError());
    }

    /**
     * The reason a bare try/catch around {@code Double.parseDouble} is not enough validation:
     * Java accepts a trailing type suffix, so this string parses successfully as 183.0. Without
     * the shape check first, a typo would be silently stored as a real measurement.
     */
    @Test
    public void rejects_183f_becauseParseDoubleWouldSilentlyAcceptIt() {
        assertEquals(183.0, Double.parseDouble("183f"), 0.0001);   // demonstrates the hazard
        assertEquals(AppError.INVALID_WEIGHT, WeightValidator.parseWeight("183f").getError());
    }

    @Test
    public void rejects_183d_forTheSameReason() {
        assertEquals(AppError.INVALID_WEIGHT, WeightValidator.parseWeight("183d").getError());
    }

    /** {@code Double.parseDouble("NaN")} succeeds. Storing NaN would poison every comparison. */
    @Test
    public void rejectsNaN() {
        assertEquals(AppError.INVALID_WEIGHT, WeightValidator.parseWeight("NaN").getError());
    }

    @Test
    public void rejectsInfinity() {
        assertEquals(AppError.INVALID_WEIGHT, WeightValidator.parseWeight("Infinity").getError());
    }

    @Test
    public void rejectsHexFloatLiteral() {
        assertEquals(AppError.INVALID_WEIGHT, WeightValidator.parseWeight("0x1p3").getError());
    }

    @Test
    public void rejectsLetters() {
        assertEquals(AppError.INVALID_WEIGHT, WeightValidator.parseWeight("abc").getError());
    }

    @Test
    public void rejectsZero() {
        assertEquals(AppError.WEIGHT_OUT_OF_RANGE, WeightValidator.parseWeight("0").getError());
    }

    @Test
    public void rejectsNegative() {
        // The minus sign fails the shape check before the range check is reached.
        assertEquals(AppError.INVALID_WEIGHT, WeightValidator.parseWeight("-5").getError());
    }

    @Test
    public void rejectsAboveMaximum() {
        assertEquals(AppError.WEIGHT_OUT_OF_RANGE, WeightValidator.parseWeight("1501").getError());
    }

    @Test
    public void acceptsMaximum() {
        assertTrue(WeightValidator.parseWeight("1500").isSuccess());
    }

    /**
     * The artifact of the original app's uncleared-input bug: typing 183.0 after 185.5 left
     * "185.51830" in the field. It parses fine as a double, which is exactly why a range check is
     * needed on top of a format check.
     */
    @Test
    public void rejects_theConcatenationBugArtifact() {
        assertEquals(AppError.INVALID_WEIGHT, WeightValidator.parseWeight("185.51830").getError());
    }

    @Test
    public void rejectsMoreThanTwoDecimalPlaces() {
        assertEquals(AppError.INVALID_WEIGHT, WeightValidator.parseWeight("183.456").getError());
    }

    // ---- goals --------------------------------------------------------------------------

    @Test
    public void parsesGoal() {
        Result<Double> result = WeightValidator.parseGoal("175");
        assertTrue(result.isSuccess());
        assertEquals(175.0, result.getOrNull(), 0.0001);
    }

    @Test
    public void goalReportsGoalSpecificErrors() {
        assertEquals(AppError.EMPTY_GOAL, WeightValidator.parseGoal("").getError());
        assertEquals(AppError.INVALID_GOAL, WeightValidator.parseGoal("abc").getError());
        assertEquals(AppError.GOAL_OUT_OF_RANGE, WeightValidator.parseGoal("0").getError());
    }

    // ---- dates --------------------------------------------------------------------------

    @Test
    public void acceptsIsoDate() {
        Result<String> result = WeightValidator.validateDate("2026-08-19");
        assertTrue(result.isSuccess());
        assertEquals("2026-08-19", result.getOrNull());
    }

    @Test
    public void acceptsLeapDay() {
        assertTrue(WeightValidator.validateDate("2024-02-29").isSuccess());
    }

    @Test
    public void rejectsEmptyDate() {
        assertEquals(AppError.EMPTY_DATE, WeightValidator.validateDate("").getError());
    }

    /**
     * Not cosmetic. Entries are ordered by {@code ORDER BY date_text DESC}, a lexicographic sort
     * that only yields chronological order while every string is zero-padded. An unpadded date
     * would sort into the wrong position and stay there.
     */
    @Test
    public void rejectsUnpaddedDate() {
        assertEquals(AppError.INVALID_DATE_FORMAT,
                WeightValidator.validateDate("2026-8-5").getError());
    }

    @Test
    public void rejectsSlashSeparators() {
        assertEquals(AppError.INVALID_DATE_FORMAT,
                WeightValidator.validateDate("2026/08/19").getError());
    }

    /** Right shape, impossible calendar date. Caught by non-lenient parsing, not by the regex. */
    @Test
    public void rejectsNonexistentDate() {
        assertEquals(AppError.IMPOSSIBLE_DATE,
                WeightValidator.validateDate("2025-02-30").getError());
    }

    @Test
    public void rejectsMonthThirteen() {
        assertEquals(AppError.IMPOSSIBLE_DATE,
                WeightValidator.validateDate("2025-13-01").getError());
    }

    @Test
    public void rejectsNonLeapFebruary29() {
        assertEquals(AppError.IMPOSSIBLE_DATE,
                WeightValidator.validateDate("2025-02-29").getError());
    }
}
