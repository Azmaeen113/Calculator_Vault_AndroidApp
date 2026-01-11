package com.example.calculator_vault_androidapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.calculator_vault_androidapp.R;
import com.example.calculator_vault_androidapp.models.CalculationHistory;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for displaying calculation history.
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<CalculationHistory> historyList;
    private OnDeleteClickListener deleteClickListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(CalculationHistory history);
    }

    public HistoryAdapter() {
        this.historyList = new ArrayList<>();
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteClickListener = listener;
    }

    public void setHistoryList(List<CalculationHistory> historyList) {
        this.historyList = historyList != null ? historyList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CalculationHistory history = historyList.get(position);
        
        holder.tvExpression.setText(history.getExpression());
        holder.tvResult.setText("= " + history.getResult());
        holder.tvTime.setText(formatDateTime(history.getCalculatedAt()));

        holder.btnDelete.setOnClickListener(v -> {
            if (deleteClickListener != null) {
                deleteClickListener.onDeleteClick(history);
            }
        });
    }

    private String formatDateTime(String dateTime) {
        if (dateTime == null || dateTime.isEmpty()) return "";
        // Format: "yyyy-MM-dd HH:mm:ss" -> "MM/dd/yyyy HH:mm"
        try {
            String[] parts = dateTime.split(" ");
            if (parts.length >= 2) {
                String[] dateParts = parts[0].split("-");
                String[] timeParts = parts[1].split(":");
                if (dateParts.length == 3 && timeParts.length >= 2) {
                    return dateParts[1] + "/" + dateParts[2] + "/" + dateParts[0] + 
                           " " + timeParts[0] + ":" + timeParts[1];
                }
            }
        } catch (Exception e) {
            // Return original if parsing fails
        }
        return dateTime;
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvExpression;
        TextView tvResult;
        TextView tvTime;
        ImageButton btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvExpression = itemView.findViewById(R.id.tvExpression);
            tvResult = itemView.findViewById(R.id.tvResult);
            tvTime = itemView.findViewById(R.id.tvTime);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
