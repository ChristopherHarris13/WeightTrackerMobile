package com.christopher.weighttracker.fakes;

import com.christopher.weighttracker.data.local.User;
import com.christopher.weighttracker.data.local.UserDao;

import java.util.HashMap;
import java.util.Map;

/**
 * In-memory {@link UserDao}.
 *
 * <p><b>Why a hand-written fake instead of Mockito.</b> Partly dependencies — Mockito is not in
 * the local Gradle cache and would drag in byte-buddy and objenesis. Mostly, though, it is that
 * writing this class <i>forces</i> the DAO's real contract to be spelled out: that a duplicate
 * username returns {@code -1} rather than throwing. A mock lets you write
 * {@code when(dao.insert(any())).thenReturn(-1L)} without ever understanding why {@code -1}
 * is the right answer.
 *
 * <p><b>A fake is executable documentation of a contract; a mock is an assertion about a method
 * call.</b> That distinction stops mattering when an interface is wide enough that faking it is a
 * chore, or when the thing being verified is that an interaction happened rather than that state
 * changed — neither applies to a two-method DAO.
 */
public class FakeUserDao implements UserDao {

    private final Map<String, User> usersByUsername = new HashMap<>();
    private int nextId = 1;

    @Override
    public long insert(User user) {
        // Mirrors OnConflictStrategy.IGNORE against the UNIQUE index on username.
        if (usersByUsername.containsKey(user.getUsername())) {
            return -1L;
        }
        int id = nextId++;
        usersByUsername.put(user.getUsername(),
                new User(id, user.getUsername(), user.getPasswordHash()));
        return id;
    }

    @Override
    public User findByUsername(String username) {
        return usersByUsername.get(username);
    }

    // ---- Test inspection ----------------------------------------------------------------

    /** Lets a test assert on what was actually persisted, e.g. that it is not a plaintext password. */
    public User storedUser(String username) {
        return usersByUsername.get(username);
    }

    public int size() {
        return usersByUsername.size();
    }
}
