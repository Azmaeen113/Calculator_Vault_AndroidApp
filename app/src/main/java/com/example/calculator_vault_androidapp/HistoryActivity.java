package com.example.calculator_vault_androidapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.calculator_vault_androidapp.adapters.HistoryAdapter;
import com.example.calculator_vault_androidapp.database.DatabaseHelper;
import com.example.calculator_vault_androidapp.database.FirebaseHelper;
import com.example.calculator_vault_androidapp.models.CalculationHistory;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity for viewing and managing calculation history.
 */
public class HistoryActivity extends AppCompatActivity {

    private RecyclerView rvHistory;
    private LinearLayout emptyState;
    private MaterialButton btnClearAll, btnBack;

    private HistoryAdapter adapter;
    private DatabaseHelper dbHelper;
    
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        dbHelper = DatabaseHelper.getInstance(this);

        initializeUI();
        loadHistory();
    }

    private void initializeUI() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvHistory = findViewById(R.id.rvHistory);
        emptyState = findViewById(R.id.emptyState);
        btnClearAll = findViewById(R.id.btnClearAll);
        btnBack = findViewById(R.id.btnBack);

        // Setup RecyclerView
        adapter = new HistoryAdapter();
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setAdapter(adapter);

        adapter.setOnDeleteClickListener(this::confirmDeleteSingle);

        // Button listeners
        btnClearAll.setOnClickListener(v -> confirmClearAll());
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void loadHistory() {
        executor.execute(() -> {
            List<CalculationHistory> history = dbHelper.getCalculationHistory();
            runOnUiThread(() -> {
                adapter.setHistoryList(history);
                updateEmptyState(history.isEmpty());
            });
        });
    }

    private void updateEmptyState(boolean isEmpty) {
        emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvHistory.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        btnClearAll.setEnabled(!isEmpty);
    }

    private void confirmDeleteSingle(CalculationHistory history) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Entry")
                .setMessage("Delete this calculation?\n\n" + history.getExpression() + " = " + history.getResult())
                .setPositiveButton("Delete", (dialog, which) -> deleteSingle(history))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteSingle(CalculationHistory history) {
        executor.execute(() -> {
            dbHelper.deleteCalculation(history.getId());
            
            FirebaseHelper.getInstance().deleteCalculationHistory(history.getId(), 
                new FirebaseHelper.SyncCallback() {
                    @Override
                    public void onSuccess() {}
                    @Override
                    public void onFailure(String error) {}
                });

            runOnUiThread(() -> {
                Toast.makeText(this, "Entry deleted", Toast.LENGTH_SHORT).show();
                loadHistory();
            });
        });
    }

    private void confirmClearAll() {
        new AlertDialog.Builder(this)
                .setTitle("Clear All History")
                .setMessage("Are you sure you want to delete all calculation history?")
                .setPositiveButton("Clear All", (dialog, which) -> clearAll())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearAll() {
        executor.execute(() -> {
            dbHelper.clearAllHistory();
            
            FirebaseHelper.getInstance().clearCalculationHistory(
                new FirebaseHelper.SyncCallback() {
                    @Override
                    public void onSuccess() {}
                    @Override
                    public void onFailure(String error) {}
                });

            runOnUiThread(() -> {
                Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show();
                loadHistory();
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
