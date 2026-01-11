package com.example.calculator_vault_androidapp.database;

import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.calculator_vault_androidapp.models.CalculationHistory;
import com.example.calculator_vault_androidapp.models.VaultFile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Firebase helper for cloud backup and sync operations.
 */
public class FirebaseHelper {

    private static final String TAG = "FirebaseHelper";
    private static FirebaseHelper instance;
    
    private final FirebaseAuth firebaseAuth;
    private final FirebaseDatabase firebaseDatabase;
    private String deviceId;

    public interface AuthCallback {
        void onSuccess(String deviceId);
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
    }

    /**
     * Authenticate anonymously to get a unique device ID.
     * @param callback Callback for authentication result
     */
    public void authenticateAnonymously(AuthCallback callback) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        
        if (currentUser != null) {
            deviceId = currentUser.getUid();
            callback.onSuccess(deviceId);
            return;
        }

        firebaseAuth.signInAnonymously()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            deviceId = user.getUid();
                            callback.onSuccess(deviceId);
                        } else {
                            callback.onFailure("User is null after authentication");
                        }
                    } else {
                        String error = task.getException() != null ? 
                                task.getException().getMessage() : "Unknown error";
                        Log.e(TAG, "Anonymous auth failed: " + error);
                        callback.onFailure(error);
                    }
                });
    }

    /**
     * Get the current device ID.
     * @return The device ID or null if not authenticated
     */
    public String getDeviceId() {
        return deviceId;
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
        if (deviceId == null) {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                deviceId = user.getUid();
            } else {
                return null;
            }
        }
        return firebaseDatabase.getReference("users").child(deviceId);
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
     * Sign out from Firebase.
     */
    public void signOut() {
        firebaseAuth.signOut();
        deviceId = null;
    }
}
