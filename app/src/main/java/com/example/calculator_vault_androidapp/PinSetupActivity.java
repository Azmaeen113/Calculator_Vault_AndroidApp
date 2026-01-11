package com.example.calculator_vault_androidapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.calculator_vault_androidapp.database.DatabaseHelper;
import com.example.calculator_vault_androidapp.database.FirebaseHelper;
import com.example.calculator_vault_androidapp.utils.CryptoUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import android.widget.TextView;

/**
 * Activity for setting up or changing the vault PIN.
 */
public class PinSetupActivity extends AppCompatActivity {

    private TextInputLayout tilCurrentPin, tilNewPin, tilConfirmPin;
    private TextInputEditText etCurrentPin, etNewPin, etConfirmPin;
    private MaterialButton btnSubmit, btnCancel;
    private TextView tvTitle, tvSubtitle;

    private DatabaseHelper dbHelper;
    private boolean isChangeMode = false;
    private String currentStoredPin; // For re-encryption when changing PIN

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_setup);

        dbHelper = DatabaseHelper.getInstance(this);

        // Check mode from intent
        String mode = getIntent().getStringExtra("mode");
        isChangeMode = "change".equals(mode);
        currentStoredPin = getIntent().getStringExtra("current_pin");

        initializeUI();
        setupMode();
    }

    private void initializeUI() {
        tvTitle = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tilCurrentPin = findViewById(R.id.tilCurrentPin);
        tilNewPin = findViewById(R.id.tilNewPin);
        tilConfirmPin = findViewById(R.id.tilConfirmPin);
        etCurrentPin = findViewById(R.id.etCurrentPin);
        etNewPin = findViewById(R.id.etNewPin);
        etConfirmPin = findViewById(R.id.etConfirmPin);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnCancel = findViewById(R.id.btnCancel);

        // Add text watchers for validation
        TextWatcher pinWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                clearErrors();
            }
        };

        etCurrentPin.addTextChangedListener(pinWatcher);
        etNewPin.addTextChangedListener(pinWatcher);
        etConfirmPin.addTextChangedListener(pinWatcher);

        btnSubmit.setOnClickListener(v -> validateAndSubmit());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void setupMode() {
        if (isChangeMode) {
            tvTitle.setText("Change PIN");
            tvSubtitle.setText("Enter your current PIN and set a new one");
            tilCurrentPin.setVisibility(View.VISIBLE);
            btnSubmit.setText("Change PIN");
            btnCancel.setVisibility(View.VISIBLE);
        } else {
            tvTitle.setText("Set Your PIN");
            tvSubtitle.setText("Create a 5-digit PIN to secure your vault");
            tilCurrentPin.setVisibility(View.GONE);
            btnSubmit.setText("Set PIN");
            btnCancel.setVisibility(View.GONE);
        }
    }

    private void clearErrors() {
        tilCurrentPin.setError(null);
        tilNewPin.setError(null);
        tilConfirmPin.setError(null);
    }

    private void validateAndSubmit() {
        clearErrors();

        String newPin = etNewPin.getText() != null ? etNewPin.getText().toString() : "";
        String confirmPin = etConfirmPin.getText() != null ? etConfirmPin.getText().toString() : "";

        // Validate new PIN
        if (!CryptoUtils.isValidPin(newPin)) {
            tilNewPin.setError("PIN must be exactly 5 digits");
            return;
        }

        // Validate confirm PIN
        if (!newPin.equals(confirmPin)) {
            tilConfirmPin.setError("PINs do not match");
            return;
        }

        if (isChangeMode) {
            String currentPin = etCurrentPin.getText() != null ? etCurrentPin.getText().toString() : "";
            
            // Validate current PIN
            if (currentPin.isEmpty()) {
                tilCurrentPin.setError("Please enter your current PIN");
                return;
            }

            String storedHash = dbHelper.getPinHash();
            if (!CryptoUtils.verifyPin(currentPin, storedHash)) {
                tilCurrentPin.setError("Current PIN is incorrect");
                return;
            }

            // Ensure new PIN is different
            if (currentPin.equals(newPin)) {
                tilNewPin.setError("New PIN must be different from current PIN");
                return;
            }

            // Update PIN and re-encrypt files
            changePinAndReencrypt(currentPin, newPin);
        } else {
            // First time setup - just set the PIN
            setNewPin(newPin);
        }
    }

    private void setNewPin(String pin) {
        String hash = CryptoUtils.hashPin(pin);
        dbHelper.setPinHash(hash);

        // Backup to Firebase
        FirebaseHelper.getInstance().backupPinHash(hash, new FirebaseHelper.SyncCallback() {
            @Override
            public void onSuccess() {
                // Successfully backed up
            }

            @Override
            public void onFailure(String error) {
                // Backup failed - will work offline
            }
        });

        Toast.makeText(this, "PIN set successfully", Toast.LENGTH_SHORT).show();
        
        // Navigate to main activity
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void changePinAndReencrypt(String oldPin, String newPin) {
        try {
            // Update all files with new encryption
            dbHelper.updateAllFilesEncryption(oldPin, newPin);

            // Update PIN hash
            String newHash = CryptoUtils.hashPin(newPin);
            dbHelper.setPinHash(newHash);

            // Backup to Firebase
            FirebaseHelper.getInstance().backupPinHash(newHash, new FirebaseHelper.SyncCallback() {
                @Override
                public void onSuccess() {}

                @Override
                public void onFailure(String error) {}
            });

            Toast.makeText(this, "PIN changed successfully", Toast.LENGTH_SHORT).show();

            // Return to vault with new PIN
            Intent intent = new Intent(this, VaultActivity.class);
            intent.putExtra("pin", newPin);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();

        } catch (Exception e) {
            Toast.makeText(this, "Error changing PIN: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (isChangeMode) {
            super.onBackPressed();
        } else {
            // Don't allow back on first-time setup
            Toast.makeText(this, "Please set a PIN to continue", Toast.LENGTH_SHORT).show();
        }
    }
}
