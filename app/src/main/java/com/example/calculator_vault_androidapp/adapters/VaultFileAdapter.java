package com.example.calculator_vault_androidapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.calculator_vault_androidapp.R;
import com.example.calculator_vault_androidapp.models.VaultFile;
import com.example.calculator_vault_androidapp.utils.FileUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * RecyclerView adapter for displaying vault files.
 */
public class VaultFileAdapter extends RecyclerView.Adapter<VaultFileAdapter.ViewHolder> {

    private List<VaultFile> files;
    private final Set<Integer> selectedIds;
    private OnItemClickListener clickListener;
    private boolean selectionMode = false;

    public interface OnItemClickListener {
        void onItemClick(VaultFile file);
        void onItemLongClick(VaultFile file);
        void onSelectionChanged(int selectedCount);
    }

    public VaultFileAdapter() {
        this.files = new ArrayList<>();
        this.selectedIds = new HashSet<>();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    public void setFiles(List<VaultFile> files) {
        this.files = files != null ? files : new ArrayList<>();
        selectedIds.clear();
        selectionMode = false;
        notifyDataSetChanged();
        if (clickListener != null) {
            clickListener.onSelectionChanged(0);
        }
    }

    public List<VaultFile> getSelectedFiles() {
        List<VaultFile> selected = new ArrayList<>();
        for (VaultFile file : files) {
            if (selectedIds.contains(file.getId())) {
                selected.add(file);
            }
        }
        return selected;
    }

    public void clearSelection() {
        selectedIds.clear();
        selectionMode = false;
        notifyDataSetChanged();
        if (clickListener != null) {
            clickListener.onSelectionChanged(0);
        }
    }

    public void selectAll() {
        selectionMode = true;
        for (VaultFile file : files) {
            selectedIds.add(file.getId());
        }
        notifyDataSetChanged();
        if (clickListener != null) {
            clickListener.onSelectionChanged(selectedIds.size());
        }
    }

    public int getSelectedCount() {
        return selectedIds.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vault_file, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VaultFile file = files.get(position);
        
        holder.tvFileName.setText(file.getFileName());
        holder.tvFileType.setText(file.getOriginalExtension().toUpperCase());
        holder.tvFileSize.setText(FileUtils.formatFileSize(file.getFileSize()));
        holder.tvUploadDate.setText(formatDate(file.getUploadedAt()));
        
        boolean isSelected = selectedIds.contains(file.getId());
        holder.checkBox.setChecked(isSelected);
        holder.checkBox.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
        holder.itemView.setActivated(isSelected);

        holder.itemView.setOnClickListener(v -> {
            if (selectionMode) {
                toggleSelection(file.getId());
            } else if (clickListener != null) {
                clickListener.onItemClick(file);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (!selectionMode) {
                selectionMode = true;
                toggleSelection(file.getId());
            }
            if (clickListener != null) {
                clickListener.onItemLongClick(file);
            }
            return true;
        });

        holder.checkBox.setOnClickListener(v -> toggleSelection(file.getId()));
    }

    private void toggleSelection(Integer fileId) {
        if (selectedIds.contains(fileId)) {
            selectedIds.remove(fileId);
        } else {
            selectedIds.add(fileId);
        }
        
        if (selectedIds.isEmpty()) {
            selectionMode = false;
        }
        
        notifyDataSetChanged();
        
        if (clickListener != null) {
            clickListener.onSelectionChanged(selectedIds.size());
        }
    }

    private String formatDate(String dateTime) {
        if (dateTime == null || dateTime.isEmpty()) return "";
        // Format: "yyyy-MM-dd HH:mm:ss" -> "MM/dd/yyyy"
        try {
            String[] parts = dateTime.split(" ");
            if (parts.length > 0) {
                String[] dateParts = parts[0].split("-");
                if (dateParts.length == 3) {
                    return dateParts[1] + "/" + dateParts[2] + "/" + dateParts[0];
                }
            }
        } catch (Exception e) {
            // Return original if parsing fails
        }
        return dateTime;
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        TextView tvFileName;
        TextView tvFileType;
        TextView tvFileSize;
        TextView tvUploadDate;

        ViewHolder(View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkBox);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvFileType = itemView.findViewById(R.id.tvFileType);
            tvFileSize = itemView.findViewById(R.id.tvFileSize);
            tvUploadDate = itemView.findViewById(R.id.tvUploadDate);
        }
    }
}
