package com.christopher.weighttracker.data.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Runs on a plain JVM. {@link Pbkdf2PasswordHasher} uses only {@code java.security} and
 * {@code javax.crypto}, with no Android types anywhere — which is precisely why hex encoding was
 * chosen over either {@code android.util.Base64} (throws in unit tests) or
 * {@code java.util.Base64} (API 26+ on Android).
 *
 * <p>Almost every test runs at a low iteration count. The production cost is deliberate and
 * would turn this class into a multi-second drag on every build, which is how test suites start
 * getting skipped.
 */
public class Pbkdf2PasswordHasherTest {

    private static final int FAST_ITERATIONS = 1_000;

    private final Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher(FAST_ITERATIONS);

    @Test
    public void hashIsNotThePlaintext() {
        String stored = hasher.hash("hunter2");
        assertFalse(stored.contains("hunter2"));
    }

    /** A random salt per call is what stops two users with the same password sharing a hash. */
    @Test
    public void samePasswordHashedTwiceProducesDifferentValues() {
        assertNotEquals(hasher.hash("hunter2"), hasher.hash("hunter2"));
    }

    @Test
    public void verifyAcceptsCorrectPassword() {
        assertTrue(hasher.verify("hunter2", hasher.hash("hunter2")));
    }

    @Test
    public void verifyRejectsWrongPassword() {
        assertFalse(hasher.verify("hunter3", hasher.hash("hunter2")));
    }

    @Test
    public void verifyIsCaseSensitive() {
        assertFalse(hasher.verify("Hunter2", hasher.hash("hunter2")));
    }

    @Test
    public void verifyHandlesUnicodePasswords() {
        String password = "pässwörd-éè-😀";
        assertTrue(hasher.verify(password, hasher.hash(password)));
    }

    // ---- stored format ------------------------------------------------------------------

    @Test
    public void storedFormatRecordsAlgorithmAndIterations() {
        String[] parts = hasher.hash("hunter2").split("\\$");

        assertEquals(5, parts.length);
        assertEquals("pbkdf2", parts[0]);
        assertTrue(parts[1].startsWith("PBKDF2WithHmac"));
        assertEquals(String.valueOf(FAST_ITERATIONS), parts[2]);
        assertEquals(32, parts[3].length());    // 16-byte salt as hex
        assertEquals(64, parts[4].length());    // 256-bit key as hex
    }

    /**
     * The payoff of the self-describing format, and the reason tests can safely run cheap.
     *
     * <p>A hash written at 1,000 iterations still verifies against a hasher configured for
     * 120,000, because verification reads the cost out of the stored string rather than assuming
     * the current default. The same mechanism is what lets the iteration count be raised later
     * without invalidating existing accounts — and what lets an API 24 device's SHA-1 hash keep
     * working after the user upgrades.
     */
    @Test
    public void hashFromOneCostVerifiesAgainstHasherConfiguredForAnother() {
        String cheap = new Pbkdf2PasswordHasher(1_000).hash("hunter2");
        Pbkdf2PasswordHasher expensive = new Pbkdf2PasswordHasher(120_000);

        assertTrue(expensive.verify("hunter2", cheap));
    }

    // ---- malformed input ----------------------------------------------------------------

    @Test
    public void verifyRejectsMalformedStoredValueWithoutThrowing() {
        assertFalse(hasher.verify("hunter2", "not-a-hash"));
        assertFalse(hasher.verify("hunter2", ""));
        assertFalse(hasher.verify("hunter2", "pbkdf2$PBKDF2WithHmacSHA256$120000$abc"));
        assertFalse(hasher.verify("hunter2", "bcrypt$PBKDF2WithHmacSHA256$120000$aabb$ccdd"));
    }

    @Test
    public void verifyRejectsNonNumericIterationCount() {
        assertFalse(hasher.verify("hunter2", "pbkdf2$PBKDF2WithHmacSHA256$lots$aabb$ccdd"));
    }

    @Test
    public void verifyRejectsNonHexPayload() {
        assertFalse(hasher.verify("hunter2", "pbkdf2$PBKDF2WithHmacSHA256$1000$zzzz$ccdd"));
    }

    @Test
    public void verifyRejectsOddLengthHex() {
        assertFalse(hasher.verify("hunter2", "pbkdf2$PBKDF2WithHmacSHA256$1000$abc$ccdd"));
    }

    @Test
    public void verifyRejectsUnknownAlgorithm() {
        assertFalse(hasher.verify("hunter2", "pbkdf2$PBKDF2WithHmacNONSENSE$1000$aabb$ccdd"));
    }

    @Test
    public void verifyRejectsNullInputs() {
        assertFalse(hasher.verify(null, hasher.hash("hunter2")));
        assertFalse(hasher.verify("hunter2", null));
    }

    /**
     * A smoke test at the real cost. Deliberately loose: this asserts the production
     * configuration is not accidentally so slow as to be unusable, not that it hits a
     * benchmark. The lower bound matters more than the upper one — if 120,000 iterations
     * finished instantly, something would be very wrong.
     */
    @Test
    public void productionIterationsCompleteInReasonableTime() {
        Pbkdf2PasswordHasher production = new Pbkdf2PasswordHasher();

        long start = System.nanoTime();
        String stored = production.hash("hunter2");
        assertTrue(production.verify("hunter2", stored));
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertTrue("hash + verify took " + elapsedMs + " ms", elapsedMs < 15_000);
    }
}
