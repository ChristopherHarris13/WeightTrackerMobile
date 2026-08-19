package com.christopher.weighttracker.data.security;

/**
 * Turns a plaintext password into a storable string, and checks a candidate against one.
 *
 * <p><b>Why this is an interface.</b> Not for the sake of abstraction — there is exactly one
 * production implementation and no plan for a second. It exists so that {@code UserRepositoryTest}
 * can inject a trivial fake. The real implementation deliberately takes hundreds of milliseconds
 * per call; six repository tests running against it would add several seconds to every build,
 * which is how test suites start getting skipped. <b>"This abstraction exists so the tests stay
 * fast" is a better justification than "abstractions are good."</b>
 */
public interface PasswordHasher {

    /**
     * Hashes {@code password} with a freshly generated random salt.
     *
     * <p>Callers must treat the result as opaque and store it verbatim. Never call this on the
     * main thread — see {@link Pbkdf2PasswordHasher} for why.
     */
    String hash(String password);

    /**
     * Checks {@code password} against a previously stored value.
     *
     * <p>Returns false rather than throwing if {@code storedHash} is malformed: a corrupted row is
     * a failed login, not a crash.
     */
    boolean verify(String password, String storedHash);
}
