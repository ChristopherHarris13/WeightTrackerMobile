package com.christopher.weighttracker.data.security;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Locale;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Salted PBKDF2 password hashing, replacing the original app's plaintext storage.
 *
 * <p>Note the imports: {@code java.security} and {@code javax.crypto} only. <b>This class contains
 * no {@code android.*} references whatsoever</b>, which is what allows it to be tested on a plain
 * JVM. Several decisions below exist to preserve that property.
 *
 * <h2>Stored format</h2>
 * <pre>pbkdf2$&lt;algorithm&gt;$&lt;iterations&gt;$&lt;hex salt&gt;$&lt;hex hash&gt;</pre>
 * for example
 * <pre>pbkdf2$PBKDF2WithHmacSHA256$120000$3f1a...$9c4e...</pre>
 *
 * <p>Verification reads the algorithm and iteration count <i>out of the stored string</i> rather
 * than using the current defaults. That is the entire reason for the self-describing format, and
 * it buys two things:
 * <ul>
 *   <li>A hash written on an API 24 device using the SHA-1 fallback still verifies correctly on
 *       an API 33 device that now writes SHA-256.</li>
 *   <li>The iteration count can be raised later without invalidating a single existing account.</li>
 * </ul>
 * <b>Parameters travel with the data.</b> The same reasoning is why bcrypt hashes start with
 * {@code $2b$} and Argon2 hashes with {@code $argon2id$v=19$}.
 *
 * <h2>The API level constraint</h2>
 * This app supports {@code minSdk 24}, but Android only guarantees
 * {@code PBKDF2WithHmacSHA256} from API 26. On 24 and 25 only {@code PBKDF2WithHmacSHA1} is
 * available.
 *
 * <p>The code below <b>probes the provider</b> and falls back on {@link NoSuchAlgorithmException}
 * rather than branching on {@code Build.VERSION.SDK_INT}. That is more truthful — it asks the
 * question actually being asked, "is this algorithm present?", instead of a proxy for it — and it
 * keeps this class free of {@code android.*}.
 *
 * <p><b>The tradeoff, stated plainly:</b> on API 24–25 the underlying PRF is HMAC-SHA-1 rather
 * than HMAC-SHA-256. That is weaker in principle but not practically broken: SHA-1's published
 * weaknesses are <i>collision</i> attacks, whereas PBKDF2 depends on HMAC-SHA-1's <i>preimage</i>
 * and PRF properties, against which there is no practical break. The alternatives were raising
 * {@code minSdk} to 26 (dropping real devices for a class project) or bundling a third-party
 * crypto library (a new dependency, and rolling one's own is precisely what not to do). Falling
 * back is the right call, and thanks to the self-describing format an upgraded device
 * transparently starts writing stronger hashes for new accounts.
 *
 * <h2>Threading</h2>
 * 120,000 iterations is deliberately expensive — that cost is the defence against offline
 * brute-forcing. It takes anywhere from 200 ms to well over a second depending on the device, so
 * it must never run on the main thread. {@code UserRepository} schedules every call onto
 * {@code AppExecutors.io()}, which is why registration and login are callback-based rather than
 * returning a value directly.
 */
public class Pbkdf2PasswordHasher implements PasswordHasher {

    private static final String PREFIX = "pbkdf2";
    private static final String PREFERRED_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String FALLBACK_ALGORITHM = "PBKDF2WithHmacSHA1";

    /** OWASP-order magnitude for PBKDF2-HMAC-SHA256. */
    private static final int DEFAULT_ITERATIONS = 120_000;

    private static final int SALT_BYTES = 16;   // 128-bit salt
    private static final int KEY_BITS = 256;

    private final int iterations;
    private final SecureRandom random = new SecureRandom();

    public Pbkdf2PasswordHasher() {
        this(DEFAULT_ITERATIONS);
    }

    /**
     * Package-private so tests can run at a low iteration count.
     *
     * <p>Then notice how the pieces fit: because the iteration count is recorded <i>in the stored
     * string</i>, a hash produced by a 1,000-iteration test hasher still verifies correctly
     * against a hasher configured for 120,000. The requirement to keep tests fast and the
     * requirement to allow parameter rotation turned out to want exactly the same design.
     */
    Pbkdf2PasswordHasher(int iterations) {
        this.iterations = iterations;
    }

    @Override
    public String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);

        String algorithm = resolveAlgorithm();
        byte[] derived = derive(password, salt, iterations, algorithm);

        return PREFIX + "$" + algorithm + "$" + iterations + "$"
                + toHex(salt) + "$" + toHex(derived);
    }

    @Override
    public boolean verify(String password, String storedHash) {
        if (password == null || storedHash == null) {
            return false;
        }
        String[] parts = storedHash.split("\\$");
        if (parts.length != 5 || !PREFIX.equals(parts[0])) {
            return false;
        }

        final String algorithm;
        final int storedIterations;
        final byte[] salt;
        final byte[] expected;
        try {
            algorithm = parts[1];
            storedIterations = Integer.parseInt(parts[2]);
            salt = fromHex(parts[3]);
            expected = fromHex(parts[4]);
        } catch (IllegalArgumentException e) {
            // Malformed stored value: a failed login, not a crash.
            return false;
        }
        if (storedIterations <= 0 || salt.length == 0 || expected.length == 0) {
            return false;
        }

        final byte[] actual;
        try {
            // Deliberately the stored algorithm and iteration count, not the current defaults.
            actual = derive(password, salt, storedIterations, algorithm);
        } catch (IllegalStateException e) {
            // Stored algorithm is unavailable on this device.
            return false;
        }
        return constantTimeEquals(expected, actual);
    }

    /**
     * Prefers SHA-256 and falls back to SHA-1 when the platform does not offer it.
     *
     * <p>Asks the provider directly instead of inferring the answer from an SDK version.
     */
    private static String resolveAlgorithm() {
        try {
            SecretKeyFactory.getInstance(PREFERRED_ALGORITHM);
            return PREFERRED_ALGORITHM;
        } catch (NoSuchAlgorithmException e) {
            return FALLBACK_ALGORITHM;   // API 24-25
        }
    }

    private static byte[] derive(String password, byte[] salt, int iterations, String algorithm) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(algorithm).generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // Reaching here means the platform lacks an algorithm it previously reported, or the
            // key spec is malformed. Both are programming/environment faults rather than
            // expected outcomes, so this is one of the few places that legitimately throws.
            throw new IllegalStateException("PBKDF2 unavailable: " + algorithm, e);
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * Compares two byte arrays without leaking <i>where</i> they first differ through timing.
     *
     * <p>A naive loop that returns on the first mismatching byte takes measurably longer the more
     * leading bytes are correct, which is enough to reconstruct a hash one byte at a time. This
     * version always inspects every byte and makes a single decision at the end.
     *
     * <p>Comparing lengths first is fine: hash length is fixed and public, so it leaks nothing.
     *
     * <p>{@code MessageDigest.isEqual} does the same job in one line. It is written out here
     * because the property is worth being able to see — one visible implementation teaches more
     * than an opaque call, and duplicating it elsewhere would not.
     */
    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        int difference = 0;
        for (int i = 0; i < a.length; i++) {
            difference |= a[i] ^ b[i];
        }
        return difference == 0;
    }

    // ---- Hex encoding -------------------------------------------------------------------
    //
    // Hand-written rather than using a library, and this is the decision to understand.
    //
    //   - android.util.Base64 works on-device from API 1, but throws in JVM unit tests because
    //     the android.jar on the unit-test classpath is stubbed out. Using it would make this
    //     class untestable without Robolectric.
    //   - java.util.Base64 is API 26+ on Android. It would NoClassDefFoundError on exactly the
    //     API 24-25 devices the SHA-1 fallback above exists to support.
    //
    // Hex costs ten lines, behaves identically on Android and the desktop JVM, needs no
    // dependency, and is readable by eye in Device Explorer - you can literally point at the salt
    // and the hash sitting in the database. It doubles the stored length versus base64 (64 chars
    // instead of 44) for a handful of rows, which is irrelevant here.

    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    private static String toHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2] = HEX_DIGITS[v >>> 4];
            out[i * 2 + 1] = HEX_DIGITS[v & 0x0F];
        }
        return new String(out);
    }

    private static byte[] fromHex(String hex) {
        int length = hex.length();
        if (length % 2 != 0) {
            throw new IllegalArgumentException("hex string has odd length");
        }
        String lower = hex.toLowerCase(Locale.US);
        byte[] out = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            int high = Character.digit(lower.charAt(i), 16);
            int low = Character.digit(lower.charAt(i + 1), 16);
            if (high < 0 || low < 0) {
                throw new IllegalArgumentException("non-hex character in stored hash");
            }
            out[i / 2] = (byte) ((high << 4) | low);
        }
        return out;
    }
}
