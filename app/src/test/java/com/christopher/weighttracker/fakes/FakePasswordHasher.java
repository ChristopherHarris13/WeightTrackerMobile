package com.christopher.weighttracker.fakes;

import com.christopher.weighttracker.data.security.PasswordHasher;

/**
 * A deliberately trivial {@link PasswordHasher} that just prefixes the password.
 *
 * <p>This is the reason {@code PasswordHasher} is an interface at all. The real implementation
 * takes hundreds of milliseconds per call by design, so running the repository's six tests
 * against it would add several seconds to every build. Cryptographic correctness is already
 * covered by {@code Pbkdf2PasswordHasherTest}; what the repository tests care about is only that
 * <i>something</i> is hashed and compared, not how.
 *
 * <p>The recognisable prefix also makes a useful assertion possible: a test can check that what
 * landed in the DAO is the hasher's output rather than the raw password.
 */
public class FakePasswordHasher implements PasswordHasher {

    public static final String PREFIX = "hashed:";

    @Override
    public String hash(String password) {
        return PREFIX + password;
    }

    @Override
    public boolean verify(String password, String storedHash) {
        return storedHash != null && storedHash.equals(PREFIX + password);
    }
}
