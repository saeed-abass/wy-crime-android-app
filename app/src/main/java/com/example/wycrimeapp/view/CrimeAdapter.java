package com.example.wycrimeapp.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wycrimeapp.R;
import com.example.wycrimeapp.model.Crime;

import java.util.ArrayList;
import java.util.List;

public class CrimeAdapter extends RecyclerView.Adapter<CrimeAdapter.CrimeViewHolder> {

    private List<Crime> crimeList = new ArrayList<>();
    private boolean isAdmin = false;
    private OnCrimeLongClickListener longClickListener;
    private OnCrimeActionListener actionListener;
    private OnCrimeClickListener clickListener;

    public interface OnCrimeLongClickListener {
        void onCrimeLongClick(Crime crime);
    }

    /**
     * Standard tap/click behaviour for a row (e.g., show full details).
     */
    public interface OnCrimeClickListener {
        void onCrimeClick(Crime crime);
    }

    /**
     * Explicit actions shown in each list row.
     * This avoids relying on long-press for core CRUD actions.
     */
    public interface OnCrimeActionListener {
        void onEdit(Crime crime);
        void onDelete(Crime crime);
    }

    public void setOnCrimeLongClickListener(OnCrimeLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setOnCrimeClickListener(OnCrimeClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnCrimeActionListener(OnCrimeActionListener listener) {
        this.actionListener = listener;
    }

    /**
     * Admin-only actions (edit/delete) are rendered only when true.
     */
    public void setAdmin(boolean admin) {
        this.isAdmin = admin;
        notifyDataSetChanged();
    }

    public void setCrimes(List<Crime> crimes) {
        this.crimeList = crimes;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CrimeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_crime, parent, false);
        return new CrimeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CrimeViewHolder holder, int position) {
        Crime crime = crimeList.get(position);
        holder.tvCrimeType.setText(crime.getCrimeType());
        holder.tvLocation.setText(crime.getLocation());
        holder.tvOutcome.setText(
                crime.getLastOutcomeCategory() != null ? crime.getLastOutcomeCategory() : "Unknown outcome"
        );

        // Admin-only action buttons
        holder.layoutActions.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        holder.btnEdit.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onEdit(crime);
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onDelete(crime);
        });

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onCrimeClick(crime);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onCrimeLongClick(crime);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return crimeList.size();
    }

    static class CrimeViewHolder extends RecyclerView.ViewHolder {
        TextView tvCrimeType, tvLocation, tvOutcome;
        LinearLayout layoutActions;
        ImageButton btnEdit, btnDelete;

        public CrimeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCrimeType = itemView.findViewById(R.id.tvCrimeType);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvOutcome = itemView.findViewById(R.id.tvOutcome);

            layoutActions = itemView.findViewById(R.id.layoutActions);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}