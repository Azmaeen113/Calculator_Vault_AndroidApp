package com.example.calculator_vault_androidapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.calculator_vault_androidapp.adapters.VaultFileAdapter;
import com.example.calculator_vault_androidapp.database.DatabaseHelper;
import com.example.calculator_vault_androidapp.database.FirebaseHelper;
import com.example.calculator_vault_androidapp.models.VaultFile;
import com.example.calculator_vault_androidapp.utils.CryptoUtils;
import com.example.calculator_vault_androidapp.utils.FileUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Secret vault activity for managing encrypted files.
 */
public class VaultActivity extends AppCompatActivity {

    private RecyclerView rvFiles;
    private LinearLayout emptyState, selectionBar;
    private TextView tvSelectionCount;
    private MaterialButton btnUpload, btnOpen, btnDownload, btnDelete, btnChangePin, btnLock;
    private ImageButton btnSelectAll, btnClearSelection;

    private VaultFileAdapter adapter;
    private DatabaseHelper dbHelper;
    private String currentPin;
    
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // File picker launcher
    private final ActivityResultLauncher<String> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            this::handleFilePicked
    );

    // Save file launcher
    private final ActivityResultLauncher<String> saveFileLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("*/*"),
            this::handleFileSaved
    );

    private VaultFile fileToSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vault);

        // Get PIN from intent
        currentPin = getIntent().getStringExtra("pin");
        if (currentPin == null || currentPin.isEmpty()) {
            Toast.makeText(this, "Invalid access", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbHelper = DatabaseHelper.getInstance(this);
        
        initializeUI();
        loadFiles();
    }

    private void initializeUI() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> lockVault());

        rvFiles = findViewById(R.id.rvFiles);
        emptyState = findViewById(R.id.emptyState);
        selectionBar = findViewById(R.id.selectionBar);
        tvSelectionCount = findViewById(R.id.tvSelectionCount);

        btnUpload = findViewById(R.id.btnUpload);
        btnOpen = findViewById(R.id.btnOpen);
        btnDownload = findViewById(R.id.btnDownload);
        btnDelete = findViewById(R.id.btnDelete);
        btnChangePin = findViewById(R.id.btnChangePin);
        btnLock = findViewById(R.id.btnLock);
        btnSelectAll = findViewById(R.id.btnSelectAll);
        btnClearSelection = findViewById(R.id.btnClearSelection);

        // Setup RecyclerView
        adapter = new VaultFileAdapter();
        rvFiles.setLayoutManager(new LinearLayoutManager(this));
        rvFiles.setAdapter(adapter);

        adapter.setOnItemClickListener(new VaultFileAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(VaultFile file) {
                openFile(file);
            }

            @Override
            public void onItemLongClick(VaultFile file) {
                updateSelectionUI();
            }

            @Override
            public void onSelectionChanged(int selectedCount) {
                updateSelectionUI();
            }
        });

        // Button listeners
        btnUpload.setOnClickListener(v -> uploadFile());
        btnOpen.setOnClickListener(v -> openSelectedFiles());
        btnDownload.setOnClickListener(v -> downloadSelectedFiles());
        btnDelete.setOnClickListener(v -> deleteSelectedFiles());
        btnChangePin.setOnClickListener(v -> changePin());
        btnLock.setOnClickListener(v -> lockVault());
        btnSelectAll.setOnClickListener(v -> adapter.selectAll());
        btnClearSelection.setOnClickListener(v -> adapter.clearSelection());
    }

    private void loadFiles() {
        executor.execute(() -> {
            List<VaultFile> files = dbHelper.getAllFiles();
            runOnUiThread(() -> {
                adapter.setFiles(files);
                updateEmptyState(files.isEmpty());
            });
        });
    }

    private void updateEmptyState(boolean isEmpty) {
        emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvFiles.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void updateSelectionUI() {
        int count = adapter.getSelectedCount();
        if (count > 0) {
            selectionBar.setVisibility(View.VISIBLE);
            tvSelectionCount.setText(count + " selected");
        } else {
            selectionBar.setVisibility(View.GONE);
        }
    }

    private void uploadFile() {
        filePickerLauncher.launch("*/*");
    }

    private void handleFilePicked(Uri uri) {
        if (uri == null) return;

        executor.execute(() -> {
            try {
                String fileName = getFileName(uri);
                String extension = FileUtils.getExtension(fileName);
                
                // Read file data
                InputStream inputStream = getContentResolver().openInputStream(uri);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                inputStream.close();
                
                byte[] originalData = baos.toByteArray();
                long fileSize = originalData.length;

                // Encrypt file data
                byte[] encryptedData = CryptoUtils.encryptData(originalData, currentPin);

                // Create vault file
                VaultFile vaultFile = new VaultFile();
                vaultFile.setFileName(fileName);
                vaultFile.setOriginalExtension(extension);
                vaultFile.setFileData(encryptedData);
                vaultFile.setFileSize(fileSize);

                // Save to database
                long id = dbHelper.saveFile(vaultFile);
                vaultFile.setId((int) id);

                // Backup to Firebase
                FirebaseHelper.getInstance().backupVaultFile(vaultFile, new FirebaseHelper.SyncCallback() {
                    @Override
                    public void onSuccess() {}
                    @Override
                    public void onFailure(String error) {}
                });

                runOnUiThread(() -> {
                    Toast.makeText(this, "File uploaded successfully", Toast.LENGTH_SHORT).show();
                    loadFiles();
                });

            } catch (Exception e) {
                runOnUiThread(() -> 
                    Toast.makeText(this, "Error uploading file: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    private String getFileName(Uri uri) {
        String fileName = "unknown";
        try {
            android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex);
                }
                cursor.close();
            }
        } catch (Exception e) {
            // Use default name
        }
        return fileName;
    }

    private void openFile(VaultFile file) {
        executor.execute(() -> {
            try {
                // Get encrypted data
                byte[] encryptedData = dbHelper.getFileData(file.getId());
                
                // Decrypt data
                byte[] decryptedData = CryptoUtils.decryptData(encryptedData, currentPin);

                // Create temp file
                File tempFile = FileUtils.createTempFile(this, file.getFileName(), decryptedData);

                // Get URI using FileProvider
                Uri uri = FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider", tempFile);

                // Open with appropriate app
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, FileUtils.getMimeType(file.getOriginalExtension()));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                runOnUiThread(() -> {
                    try {
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() ->
                    Toast.makeText(this, "Error opening file: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    private void openSelectedFiles() {
        List<VaultFile> selected = adapter.getSelectedFiles();
        if (selected.isEmpty()) {
            Toast.makeText(this, "Please select a file", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Open first selected file
        openFile(selected.get(0));
        adapter.clearSelection();
    }

    private void downloadSelectedFiles() {
        List<VaultFile> selected = adapter.getSelectedFiles();
        if (selected.isEmpty()) {
            Toast.makeText(this, "Please select a file", Toast.LENGTH_SHORT).show();
            return;
        }

        // Download first selected file
        fileToSave = selected.get(0);
        saveFileLauncher.launch(fileToSave.getFileName());
    }

    private void handleFileSaved(Uri uri) {
        if (uri == null || fileToSave == null) return;

        executor.execute(() -> {
            try {
                // Get encrypted data
                byte[] encryptedData = dbHelper.getFileData(fileToSave.getId());
                
                // Decrypt data
                byte[] decryptedData = CryptoUtils.decryptData(encryptedData, currentPin);

                // Write to output
                OutputStream outputStream = getContentResolver().openOutputStream(uri);
                outputStream.write(decryptedData);
                outputStream.close();

                runOnUiThread(() -> {
                    Toast.makeText(this, "File downloaded successfully", Toast.LENGTH_SHORT).show();
                    adapter.clearSelection();
                });

            } catch (Exception e) {
                runOnUiThread(() ->
                    Toast.makeText(this, "Error downloading file: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    private void deleteSelectedFiles() {
        List<VaultFile> selected = adapter.getSelectedFiles();
        if (selected.isEmpty()) {
            Toast.makeText(this, "Please select files to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Files")
                .setMessage("Are you sure you want to delete " + selected.size() + " file(s)?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    executor.execute(() -> {
                        for (VaultFile file : selected) {
                            dbHelper.deleteFile(file.getId());
                            FirebaseHelper.getInstance().deleteVaultFile(file.getId(), 
                                new FirebaseHelper.SyncCallback() {
                                    @Override
                                    public void onSuccess() {}
                                    @Override
                                    public void onFailure(String error) {}
                                });
                        }
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Files deleted", Toast.LENGTH_SHORT).show();
                            loadFiles();
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void changePin() {
        Intent intent = new Intent(this, PinSetupActivity.class);
        intent.putExtra("mode", "change");
        intent.putExtra("current_pin", currentPin);
        startActivity(intent);
        finish();
    }

    private void lockVault() {
        // Clean up temp files
        FileUtils.deleteTempFiles();
        FileUtils.clearTempDirectory(this);
        
        // Return to calculator
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        lockVault();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FileUtils.deleteTempFiles();
        executor.shutdown();
    }
}
