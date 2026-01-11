package com.example.calculator_vault_androidapp.database;

import android.app.Activity;
import android.content.Intent;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.calculator_vault_androidapp.models.CalculationHistory;
import com.example.calculator_vault_androidapp.models.VaultFile;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Firebase helper for cloud backup and sync operations with Google Sign-In.
 */
public class FirebaseHelper {

    private static final String TAG = "FirebaseHelper";
    private static FirebaseHelper instance;
    public static final int RC_SIGN_IN = 9001;
    
    private final FirebaseAuth firebaseAuth;
    private final FirebaseDatabase firebaseDatabase;
    private String userId;

    public interface AuthCallback {
        void onSuccess(String userId, String userName, String userEmail);
        void onFailure(String error);
    }

    public interface SyncCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public static synchronized FirebaseHelper getInstance() {
        if (instance == null) {
            instance = new FirebaseHelper();
        }
        return instance;
    }

    private FirebaseHelper() {
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            userId = user.getUid();
        }
    }

    /**
     * Get GoogleSignInClient for sign-in.
     * @param activity The activity context
     * @param webClientId Your web client ID from Firebase console
     * @return GoogleSignInClient
     */
    public GoogleSignInClient getGoogleSignInClient(Activity activity, String webClientId) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build();
        return GoogleSignIn.getClient(activity, gso);
    }

    /**
     * Handle Google Sign-In result.
     * @param data Intent data from onActivityResult
     * @param callback Callback for authentication result
     */
    public void handleGoogleSignInResult(Intent data, AuthCallback callback) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account != null) {
                firebaseAuthWithGoogle(account.getIdToken(), callback);
            } else {
                callback.onFailure("Google Sign-In account is null");
            }
        } catch (ApiException e) {
            Log.e(TAG, "Google sign in failed", e);
            callback.onFailure("Google Sign-In failed: " + e.getMessage());
        }
    }

    /**
     * Authenticate with Firebase using Google credentials.
     * @param idToken Google ID token
     * @param callback Callback for authentication result
     */
    private void firebaseAuthWithGoogle(String idToken, AuthCallback callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            userId = user.getUid();
                            String userName = user.getDisplayName() != null ? user.getDisplayName() : "User";
                            String userEmail = user.getEmail() != null ? user.getEmail() : "";
                            callback.onSuccess(userId, userName, userEmail);
                        } else {
                            callback.onFailure("User is null after authentication");
                        }
                    } else {
                        String error = task.getException() != null ? 
                                task.getException().getMessage() : "Unknown error";
                        Log.e(TAG, "Firebase auth failed: " + error);
                        callback.onFailure(error);
                    }
                });
    }

    /**
     * Get the current user ID.
     * @return The user ID or null if not authenticated
     */
    public String getUserId() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            userId = user.getUid();
        }
        return userId;
    }

    /**
     * Get the current user's display name.
     * @return The user's name or null
     */
    public String getUserName() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        return user != null ? user.getDisplayName() : null;
    }

    /**
     * Get the current user's email.
     * @return The user's email or null
     */
    public String getUserEmail() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        return user != null ? user.getEmail() : null;
    }

    /**
     * Check if user is authenticated.
     * @return true if authenticated
     */
    public boolean isAuthenticated() {
        return firebaseAuth.getCurrentUser() != null;
    }

    /**
     * Get the database reference for the current user.
     * @return DatabaseReference for the user's data
     */
    private DatabaseReference getUserReference() {
        if (userId == null) {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                userId = user.getUid();
            } else {
                return null;
            }
        }
        return firebaseDatabase.getReference("users").child(userId);
    }

    /**
     * Backup PIN hash to Firebase.
     * @param pinHash The PIN hash to backup
     * @param callback Callback for result
     */
    public void backupPinHash(String pinHash, SyncCallback callback) {
        DatabaseReference userRef = getUserReference();
        if (userRef == null) {
            callback.onFailure("Not authenticated");
            return;
        }

        Map<String, Object> configData = new HashMap<>();
        configData.put("pin_hash", pinHash);
        configData.put("created_at", System.currentTimeMillis());

        userRef.child("config").setValue(configData)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Backup a vault file to Firebase.
     * @param file The VaultFile to backup
     * @param callback Callback for result
     */
    public void backupVaultFile(VaultFile file, SyncCallback callback) {
        DatabaseReference userRef = getUserReference();
        if (userRef == null) {
            callback.onFailure("Not authenticated");
            return;
        }

        Map<String, Object> fileData = new HashMap<>();
        fileData.put("file_name", file.getFileName());
        fileData.put("original_extension", file.getOriginalExtension());
        fileData.put("file_data", Base64.encodeToString(file.getFileData(), Base64.DEFAULT));
        fileData.put("file_size", file.getFileSize());
        fileData.put("uploaded_at", System.currentTimeMillis());

        String fileKey = file.getId() != null ? String.valueOf(file.getId()) : 
                userRef.child("vault_files").push().getKey();
        
        if (fileKey != null) {
            userRef.child("vault_files").child(fileKey).setValue(fileData)
                    .addOnSuccessListener(aVoid -> callback.onSuccess())
                    .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
        } else {
            callback.onFailure("Failed to generate file key");
        }
    }

    /**
     * Delete a vault file from Firebase.
     * @param fileId The file ID to delete
     * @param callback Callback for result
     */
    public void deleteVaultFile(int fileId, SyncCallback callback) {
        DatabaseReference userRef = getUserReference();
        if (userRef == null) {
            callback.onFailure("Not authenticated");
            return;
        }

        userRef.child("vault_files").child(String.valueOf(fileId)).removeValue()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Backup a calculation history entry to Firebase.
     * @param history The calculation history entry
     * @param callback Callback for result
     */
    public void backupCalculationHistory(CalculationHistory history, SyncCallback callback) {
        DatabaseReference userRef = getUserReference();
        if (userRef == null) {
            callback.onFailure("Not authenticated");
            return;
        }

        Map<String, Object> historyData = new HashMap<>();
        historyData.put("expression", history.getExpression());
        historyData.put("result", history.getResult());
        historyData.put("calculated_at", System.currentTimeMillis());

        String historyKey = history.getId() > 0 ? String.valueOf(history.getId()) : 
                userRef.child("calculation_history").push().getKey();
        
        if (historyKey != null) {
            userRef.child("calculation_history").child(historyKey).setValue(historyData)
                    .addOnSuccessListener(aVoid -> callback.onSuccess())
                    .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
        } else {
            callback.onFailure("Failed to generate history key");
        }
    }

    /**
     * Backup all data to Firebase.
     * @param pinHash The PIN hash
     * @param files List of vault files
     * @param history List of calculation history
     * @param callback Callback for result
     */
    public void backupAllData(String pinHash, List<VaultFile> files, 
                              List<CalculationHistory> history, SyncCallback callback) {
        DatabaseReference userRef = getUserReference();
        if (userRef == null) {
            callback.onFailure("Not authenticated");
            return;
        }

        Map<String, Object> allData = new HashMap<>();

        // Config
        Map<String, Object> configData = new HashMap<>();
        configData.put("pin_hash", pinHash);
        configData.put("created_at", System.currentTimeMillis());
        allData.put("config", configData);

        // Vault files
        Map<String, Object> filesData = new HashMap<>();
        for (VaultFile file : files) {
            Map<String, Object> fileData = new HashMap<>();
            fileData.put("file_name", file.getFileName());
            fileData.put("original_extension", file.getOriginalExtension());
            if (file.getFileData() != null) {
                fileData.put("file_data", Base64.encodeToString(file.getFileData(), Base64.DEFAULT));
            }
            fileData.put("file_size", file.getFileSize());
            fileData.put("uploaded_at", file.getUploadedAt());
            filesData.put(String.valueOf(file.getId()), fileData);
        }
        allData.put("vault_files", filesData);

        // Calculation history
        Map<String, Object> historyMap = new HashMap<>();
        for (CalculationHistory item : history) {
            Map<String, Object> historyData = new HashMap<>();
            historyData.put("expression", item.getExpression());
            historyData.put("result", item.getResult());
            historyData.put("calculated_at", item.getCalculatedAt());
            historyMap.put(String.valueOf(item.getId()), historyData);
        }
        allData.put("calculation_history", historyMap);

        userRef.setValue(allData)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Clear all calculation history from Firebase.
     * @param callback Callback for result
     */
    public void clearCalculationHistory(SyncCallback callback) {
        DatabaseReference userRef = getUserReference();
        if (userRef == null) {
            callback.onFailure("Not authenticated");
            return;
        }

        userRef.child("calculation_history").removeValue()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Delete a calculation history entry from Firebase.
     * @param historyId The history entry ID to delete
     * @param callback Callback for result
     */
    public void deleteCalculationHistory(int historyId, SyncCallback callback) {
        DatabaseReference userRef = getUserReference();
        if (userRef == null) {
            callback.onFailure("Not authenticated");
            return;
        }

        userRef.child("calculation_history").child(String.valueOf(historyId)).removeValue()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Callback interface for fetching calculation history.
     */
    public interface FetchHistoryCallback {
        void onSuccess(List<CalculationHistory> history);
        void onFailure(String error);
    }

    /**
     * Fetch all calculation history from Firebase.
     * @param callback Callback with fetched history
     */
    public void fetchCalculationHistory(FetchHistoryCallback callback) {
        DatabaseReference userRef = getUserReference();
        if (userRef == null) {
            callback.onFailure("Not authenticated");
            return;
        }

        userRef.child("calculation_history").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<CalculationHistory> historyList = new java.util.ArrayList<>();
                
                for (DataSnapshot child : snapshot.getChildren()) {
                    try {
                        String idStr = child.getKey();
                        String expression = child.child("expression").getValue(String.class);
                        String result = child.child("result").getValue(String.class);
                        Object calculatedAtObj = child.child("calculated_at").getValue();
                        
                        if (idStr != null && expression != null && result != null) {
                            CalculationHistory history = new CalculationHistory();
                            history.setId(Integer.parseInt(idStr));
                            history.setExpression(expression);
                            history.setResult(result);
                            
                            // Handle both timestamp (long) and date string formats
                            if (calculatedAtObj instanceof Long) {
                                long timestamp = (Long) calculatedAtObj;
                                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                                    "yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                                history.setCalculatedAt(sdf.format(new java.util.Date(timestamp)));
                            } else if (calculatedAtObj instanceof String) {
                                history.setCalculatedAt((String) calculatedAtObj);
                            }
                            
                            historyList.add(history);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing history item: " + e.getMessage());
                    }
                }
                
                // Sort by calculated_at in descending order
                historyList.sort((h1, h2) -> h2.getCalculatedAt().compareTo(h1.getCalculatedAt()));
                
                callback.onSuccess(historyList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to fetch history: " + error.getMessage());
                callback.onFailure(error.getMessage());
            }
        });
    }

    /**
     * Sign out from Firebase.
     */
    public void signOut() {
        firebaseAuth.signOut();
        userId = null;
    }
}
