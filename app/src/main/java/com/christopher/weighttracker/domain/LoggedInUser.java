package com.christopher.weighttracker.domain;

/**
 * The identity of a signed-in user, as handed from the login screen to the dashboard.
 *
 * <p>Small but not pointless: it carries the <b>normalized</b> username. Without it,
 * {@code LoginActivity} would have to re-read the raw {@code EditText} to build its Intent and
 * would ship whatever the user typed — {@code "  Chris "} — to the dashboard, which would then
 * greet them differently from the name actually stored in the database.
 */
public final class LoggedInUser {

    private final int id;
    private final String username;

    public LoggedInUser(int id, String username) {
        this.id = id;
        this.username = username;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }
}
