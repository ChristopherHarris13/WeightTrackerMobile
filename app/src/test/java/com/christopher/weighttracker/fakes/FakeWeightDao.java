package com.christopher.weighttracker.fakes;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.christopher.weighttracker.data.local.WeightEntry;
import com.christopher.weighttracker.data.local.WeightDao;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * In-memory {@link WeightDao}.
 *
 * <p><b>Note what {@link #observeForUser} does and does not do.</b> It returns a
 * {@code MutableLiveData} but never calls {@code setValue} on it. Constructing LiveData is
 * harmless on a plain JVM; <i>mutating</i> it is not, because {@code setValue} asserts it is on
 * the main thread, which routes through {@code Looper.getMainLooper()} — unmocked in unit tests,
 * so it throws. Fixing that properly needs {@code InstantTaskExecutorRule} from
 * {@code androidx.arch.core:core-testing}, a dependency this project deliberately avoids.
 *
 * <p>So the gap is stated rather than papered over: <b>these tests exercise the command paths;
 * observation correctness is verified by running the app.</b> That is an honest trade, and the
 * command paths are where the logic worth testing actually lives.
 */
public class FakeWeightDao implements WeightDao {

    private final List<WeightEntry> entries = new ArrayList<>();
    private int nextId = 1;

    @Override
    public long insert(WeightEntry entry) {
        int id = nextId++;
        entries.add(new WeightEntry(id, entry.getUserId(), entry.getDateText(), entry.getWeight()));
        return id;
    }

    @Override
    public int update(WeightEntry entry) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getId() == entry.getId()) {
                entries.set(i, entry);
                return 1;
            }
        }
        return 0;
    }

    @Override
    public int delete(WeightEntry entry) {
        return entries.removeIf(e -> e.getId() == entry.getId()) ? 1 : 0;
    }

    @Override
    public LiveData<List<WeightEntry>> observeForUser(int userId) {
        // See the class comment: never mutated, so never touches the main-thread assertion.
        return new MutableLiveData<>();
    }

    // ---- Test inspection ----------------------------------------------------------------

    /** The rows a real query would return, in the same order the DAO declares. */
    public List<WeightEntry> entriesFor(int userId) {
        List<WeightEntry> result = new ArrayList<>();
        for (WeightEntry entry : entries) {
            if (entry.getUserId() == userId) {
                result.add(entry);
            }
        }
        result.sort(Comparator.comparing(WeightEntry::getDateText)
                .thenComparingInt(WeightEntry::getId)
                .reversed());
        return result;
    }

    public int size() {
        return entries.size();
    }
}
