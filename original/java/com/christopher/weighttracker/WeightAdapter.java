package com.christopher.weighttracker;

import android.content.Context;
import android.database.Cursor;
import android.view.*;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

public class WeightAdapter extends RecyclerView.Adapter<WeightAdapter.VH> {
    private final Context ctx;
    private Cursor cursor;
    private final DBHelper db;

    public interface OnDataChanged { void refresh(); }
    private final OnDataChanged onDataChanged;

    public WeightAdapter(Context ctx, Cursor cursor, DBHelper db, OnDataChanged onDataChanged) {
        this.ctx = ctx;
        this.cursor = cursor;
        this.db = db;
        this.onDataChanged = onDataChanged;
    }

    public static class VH extends RecyclerView.ViewHolder {
        TextView tvDate, tvWeight;
        public VH(View v) {
            super(v);
            tvDate = v.findViewById(R.id.tvDate);
            tvWeight = v.findViewById(R.id.tvWeight);
        }
    }

    @Override public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_weight, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(VH holder, int position) {
        if (!cursor.moveToPosition(position)) return;
        int id = cursor.getInt(0);
        String date = cursor.getString(1);
        double w = cursor.getDouble(2);

        holder.tvDate.setText(date);
        holder.tvWeight.setText(String.valueOf(w));

        holder.itemView.setOnClickListener(v -> showEditDialog(id, date, w));
        holder.itemView.setOnLongClickListener(v -> {
            int deleted = db.deleteWeight(id);
            Toast.makeText(ctx, deleted > 0 ? "Deleted" : "Delete failed", Toast.LENGTH_SHORT).show();
            onDataChanged.refresh();
            return true;
        });
    }

    @Override public int getItemCount() { return (cursor == null) ? 0 : cursor.getCount(); }

    public void swapCursor(Cursor newCursor) {
        if (cursor != null) cursor.close();
        cursor = newCursor;
        notifyDataSetChanged();
    }

    private void showEditDialog(int id, String oldDate, double oldWeight) {
        View view = LayoutInflater.from(ctx).inflate(R.layout.dialog_edit_weight, null);
        EditText etDate = view.findViewById(R.id.etEditDate);
        EditText etWeight = view.findViewById(R.id.etEditWeight);
        etDate.setText(oldDate);
        etWeight.setText(String.valueOf(oldWeight));

        new AlertDialog.Builder(ctx)
                .setTitle("Edit Weight")
                .setView(view)
                .setPositiveButton("Save", (d, wBtn) -> {
                    String newDate = etDate.getText().toString().trim();
                    String ws = etWeight.getText().toString().trim();
                    if (newDate.isEmpty() || ws.isEmpty()) {
                        Toast.makeText(ctx, "Both fields required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        double newW = Double.parseDouble(ws);
                        int updated = db.updateWeight(id, newDate, newW);
                        Toast.makeText(ctx, updated > 0 ? "Updated" : "Update failed", Toast.LENGTH_SHORT).show();
                        onDataChanged.refresh();
                    } catch (NumberFormatException e) {
                        Toast.makeText(ctx, "Invalid weight", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
