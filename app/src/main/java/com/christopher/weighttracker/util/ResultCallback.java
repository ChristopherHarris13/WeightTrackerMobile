package com.christopher.weighttracker.util;

import com.christopher.weighttracker.domain.Result;

/**
 * Receives the outcome of a repository command.
 *
 * <p>Repository queries return {@code LiveData} and are observed; repository <i>commands</i> are
 * void and report back through this interface. The asymmetry is deliberate and worth naming:
 * <b>reads are a stream you subscribe to, writes are a request you get one answer to.</b>
 *
 * <p>Implementations are always invoked on the main thread, so it is safe to touch LiveData from
 * inside {@link #onResult}.
 *
 * @param <T> the value carried on success
 */
public interface ResultCallback<T> {
    void onResult(Result<T> result);
}
