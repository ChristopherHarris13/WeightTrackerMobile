package com.christopher.weighttracker.ui.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.christopher.weighttracker.R;
import com.christopher.weighttracker.data.local.WeightEntry;
import com.christopher.weighttracker.util.WeightFormatter;

/**
 * Renders weight entries.
 *
 * <p>Compare this against the original: that version held a {@code DBHelper}, a {@code Context},
 * built an {@code AlertDialog}, and called {@code db.deleteWeight()} directly from a long-press
 * handler. An adapter that writes to the database is the clearest possible illustration of
 * missing layers — it had no way to be tested, and no way to be reused.
 *
 * <p>What is left is an adapter that renders a list and reports taps. Both callbacks go out
 * through {@link OnEntryInteraction} to the Activity, which decides what a tap <i>means</i>.
 */
public class WeightAdapter extends ListAdapter<WeightEntry, WeightAdapter.WeightViewHolder> {

    /** How the adapter reports user intent without knowing what will be done about it. */
    public interface OnEntryInteraction {
        void onEntryClicked(WeightEntry entry);

        void onEntryLongClicked(WeightEntry entry);
    }

    private final OnEntryInteraction listener;

    public WeightAdapter(OnEntryInteraction listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    /**
     * Replaces {@code notifyDataSetChanged()}, which redrew every row on any change.
     *
     * <p>Two methods that are easy to conflate and worth stating precisely:
     * <ul>
     *   <li>{@link DiffUtil.ItemCallback#areItemsTheSame} asks about <b>identity</b> — are these
     *       two objects the same row? Compare ids.</li>
     *   <li>{@link DiffUtil.ItemCallback#areContentsTheSame} asks about <b>visual equality</b> —
     *       does this row need redrawing? Compare displayed fields.</li>
     * </ul>
     * Swap them and an edited row animates as a delete followed by an insert instead of updating
     * in place, or nothing animates at all.
     *
     * <p>This whole mechanism depends on a guarantee made three files away: <b>the previously
     * submitted list must never be mutated.</b> Room returns a fresh list of fresh objects on
     * every query and {@link WeightEntry} is immutable, so that holds by construction rather than
     * by everyone being careful. That is the payoff for the entity design.
     */
    static final DiffUtil.ItemCallback<WeightEntry> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<WeightEntry>() {
                @Override
                public boolean areItemsTheSame(@NonNull WeightEntry oldItem,
                                               @NonNull WeightEntry newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull WeightEntry oldItem,
                                                  @NonNull WeightEntry newItem) {
                    // Double.compare rather than ==: it handles NaN and -0.0 correctly, and it is
                    // the habit worth having even where those cannot occur.
                    return oldItem.getDateText().equals(newItem.getDateText())
                            && Double.compare(oldItem.getWeight(), newItem.getWeight()) == 0;
                }
            };

    @NonNull
    @Override
    public WeightViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_weight, parent, false);
        return new WeightViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WeightViewHolder holder, int position) {
        WeightEntry entry = getItem(position);
        holder.tvDate.setText(entry.getDateText());
        // Fixes the raw-precision display bug: 185.5183 rendered verbatim in the original.
        holder.tvWeight.setText(WeightFormatter.format(entry.getWeight()));
    }

    class WeightViewHolder extends RecyclerView.ViewHolder {

        final TextView tvDate;
        final TextView tvWeight;

        WeightViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvWeight = itemView.findViewById(R.id.tvWeight);

            // Listeners are bound once per view holder, not once per bind.
            //
            // The original captured `position` inside onBindViewHolder, which goes stale the
            // moment a row is inserted or removed above it -- so a tap could edit the wrong
            // entry. Reading getBindingAdapterPosition() at click time always reflects the
            // current list, and binding here allocates two lambdas per holder instead of two
            // per bind.
            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onEntryClicked(getItem(position));
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onEntryLongClicked(getItem(position));
                }
                return true;
            });
        }
    }
}
