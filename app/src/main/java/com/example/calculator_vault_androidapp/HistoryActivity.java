package com.example.calculator_vault_androidapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.calculator_vault_androidapp.adapters.HistoryAdapter;
import com.example.calculator_vault_androidapp.database.DatabaseHelper;
import com.example.calculator_vault_androidapp.database.FirebaseHelper;
import com.example.calculator_vault_androidapp.models.CalculationHistory;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity for viewing and managing calculation history with Google Sign-In.
 */
public class HistoryActivity extends AppCompatActivity {

    // Web Client ID from Firebase Console
    private static final String WEB_CLIENT_ID = "53844317238-nf8750r57m0g12homptpajafmgpo4a93.apps.googleusercontent.com";

    private LinearLayout signInState, contentState, emptyState;
    private RecyclerView rvHistory;
    private MaterialButton btnGoogleSignIn, btnSignOut, btnClearAll, btnBack;
    private TextView tvUserInfo;

    private HistoryAdapter adapter;
    private DatabaseHelper dbHelper;
    private GoogleSignInClient googleSignInClient;
    
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        dbHelper = DatabaseHelper.getInstance(this);
        googleSignInClient = FirebaseHelper.getInstance().getGoogleSignInClient(this, WEB_CLIENT_ID);

        initializeUI();
        checkAuthenticationState();
    }

    private void initializeUI() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // UI elements
        signInState = findViewById(R.id.signInState);
        contentState = findViewById(R.id.contentState);
        emptyState = findViewById(R.id.emptyState);
        rvHistory = findViewById(R.id.rvHistory);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        btnSignOut = findViewById(R.id.btnSignOut);
        btnClearAll = findViewById(R.id.btnClearAll);
        btnBack = findViewById(R.id.btnBack);
        tvUserInfo = findViewById(R.id.tvUserInfo);

        // Setup RecyclerView
        adapter = new HistoryAdapter();
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setAdapter(adapter);

        adapter.setOnDeleteClickListener(this::confirmDeleteSingle);

        // Button listeners
        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());
        btnSignOut.setOnClickListener(v -> confirmSignOut());
        btnClearAll.setOnClickListener(v -> confirmClearAll());
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void checkAuthenticationState() {
        if (FirebaseHelper.getInstance().isAuthenticated()) {
            showContentState();
            updateUserInfo();
            loadHistory();
        } else {
            showSignInState();
        }
    }

    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, FirebaseHelper.RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FirebaseHelper.RC_SIGN_IN) {
            if (data != null) {
                FirebaseHelper.getInstance().handleGoogleSignInResult(data, 
                    new FirebaseHelper.AuthCallback() {
                        @Override
                        public void onSuccess(String userId, String userName, String userEmail) {
                            Toast.makeText(HistoryActivity.this, 
                                "Signed in as " + userName, Toast.LENGTH_SHORT).show();
                            checkAuthenticationState();
                        }

                        @Override
                        public void onFailure(String error) {
                            Toast.makeText(HistoryActivity.this, 
                                "Sign-in failed: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
            }
        }
    }

    private void updateUserInfo() {
        String email = FirebaseHelper.getInstance().getUserEmail();
        String name = FirebaseHelper.getInstance().getUserName();
        
        if (email != null) {
            tvUserInfo.setText("Signed in as: " + email);
        } else if (name != null) {
            tvUserInfo.setText("Signed in as: " + name);
        }
    }

    private void showSignInState() {
        signInState.setVisibility(View.VISIBLE);
        contentState.setVisibility(View.GONE);
    }

    private void showContentState() {
        signInState.setVisibility(View.GONE);
        contentState.setVisibility(View.VISIBLE);
    }

    private void loadHistory() {
        executor.execute(() -> {
            // First load from local database
            List<CalculationHistory> localHistory = dbHelper.getCalculationHistory();
            
            runOnUiThread(() -> {
                adapter.setHistoryList(localHistory);
                updateEmptyState(localHistory.isEmpty());
            });
            
            // Then fetch from Firebase to sync any missing entries
            if (FirebaseHelper.getInstance().isAuthenticated()) {
                FirebaseHelper.getInstance().fetchCalculationHistory(
                    new FirebaseHelper.FetchHistoryCallback() {
                        @Override
                        public void onSuccess(List<CalculationHistory> firebaseHistory) {
                            executor.execute(() -> {
                                // Merge Firebase history with local database
                                for (CalculationHistory fbItem : firebaseHistory) {
                                    // Check if this history item exists in local database
                                    boolean exists = localHistory.stream()
                                        .anyMatch(localItem -> 
                                            localItem.getExpression().equals(fbItem.getExpression()) &&
                                            localItem.getResult().equals(fbItem.getResult()) &&
                                            localItem.getCalculatedAt().equals(fbItem.getCalculatedAt()));
                                    
                                    // If not exists, add to local database
                                    if (!exists) {
                                        // This will generate a new ID in local DB
                                        dbHelper.saveCalculation(fbItem.getExpression(), fbItem.getResult());
                                    }
                                }
                                
                                // Reload the combined history
                                List<CalculationHistory> updatedHistory = dbHelper.getCalculationHistory();
                                runOnUiThread(() -> {
                                    adapter.setHistoryList(updatedHistory);
                                    updateEmptyState(updatedHistory.isEmpty());
                                });
                            });
                        }
                        
                        @Override
                        public void onFailure(String error) {
                            // Firebase fetch failed, but local data is already displayed
                        }
                    });
            }
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

    private void confirmSignOut() {
        new AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out? Your history will remain in the cloud.")
                .setPositiveButton("Sign Out", (dialog, which) -> signOut())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void signOut() {
        FirebaseHelper.getInstance().signOut();
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            adapter.setHistoryList(new java.util.ArrayList<>());
            Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show();
            checkAuthenticationState();
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
