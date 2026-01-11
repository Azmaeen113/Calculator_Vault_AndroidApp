package com.example.calculator_vault_androidapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.calculator_vault_androidapp.models.CalculationHistory;
import com.example.calculator_vault_androidapp.models.VaultFile;
import com.example.calculator_vault_androidapp.utils.CryptoUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * SQLite database helper for managing app data.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "calculator_vault.db";
    private static final int DATABASE_VERSION = 1;

    // Table names
    private static final String TABLE_CONFIG = "config";
    private static final String TABLE_VAULT_FILES = "vault_files";
    private static final String TABLE_CALCULATION_HISTORY = "calculation_history";

    // Config table columns
    private static final String COL_CONFIG_ID = "id";
    private static final String COL_CONFIG_PIN_HASH = "pin_hash";
    private static final String COL_CONFIG_IS_FIRST_TIME = "is_first_time";
    private static final String COL_CONFIG_CREATED_AT = "created_at";

    // Vault files table columns
    private static final String COL_FILE_ID = "id";
    private static final String COL_FILE_NAME = "file_name";
    private static final String COL_FILE_EXTENSION = "original_extension";
    private static final String COL_FILE_DATA = "file_data";
    private static final String COL_FILE_SIZE = "file_size";
    private static final String COL_FILE_UPLOADED_AT = "uploaded_at";

    // Calculation history table columns
    private static final String COL_HISTORY_ID = "id";
    private static final String COL_HISTORY_EXPRESSION = "expression";
    private static final String COL_HISTORY_RESULT = "result";
    private static final String COL_HISTORY_CALCULATED_AT = "calculated_at";

    private static DatabaseHelper instance;
    private final SimpleDateFormat dateFormat;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create config table
        String createConfigTable = "CREATE TABLE " + TABLE_CONFIG + " (" +
                COL_CONFIG_ID + " INTEGER PRIMARY KEY, " +
                COL_CONFIG_PIN_HASH + " TEXT, " +
                COL_CONFIG_IS_FIRST_TIME + " INTEGER DEFAULT 1, " +
                COL_CONFIG_CREATED_AT + " TEXT" +
                ")";
        db.execSQL(createConfigTable);

        // Create vault files table
        String createVaultFilesTable = "CREATE TABLE " + TABLE_VAULT_FILES + " (" +
                COL_FILE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_FILE_NAME + " TEXT, " +
                COL_FILE_EXTENSION + " TEXT, " +
                COL_FILE_DATA + " BLOB, " +
                COL_FILE_SIZE + " INTEGER, " +
                COL_FILE_UPLOADED_AT + " TEXT" +
                ")";
        db.execSQL(createVaultFilesTable);

        // Create calculation history table
        String createHistoryTable = "CREATE TABLE " + TABLE_CALCULATION_HISTORY + " (" +
                COL_HISTORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_HISTORY_EXPRESSION + " TEXT, " +
                COL_HISTORY_RESULT + " TEXT, " +
                COL_HISTORY_CALCULATED_AT + " TEXT" +
                ")";
        db.execSQL(createHistoryTable);

        // Insert initial config row
        ContentValues values = new ContentValues();
        values.put(COL_CONFIG_ID, 1);
        values.put(COL_CONFIG_IS_FIRST_TIME, 1);
        values.put(COL_CONFIG_CREATED_AT, dateFormat.format(new Date()));
        db.insert(TABLE_CONFIG, null, values);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONFIG);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_VAULT_FILES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CALCULATION_HISTORY);
        onCreate(db);
    }

    // ===================== CONFIG OPERATIONS =====================

    /**
     * Set the PIN hash in config.
     * @param hash The SHA-256 hash of the PIN
     */
    public void setPinHash(String hash) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_CONFIG_PIN_HASH, hash);
        values.put(COL_CONFIG_IS_FIRST_TIME, 0);
        db.update(TABLE_CONFIG, values, COL_CONFIG_ID + " = ?", new String[]{"1"});
    }

    /**
     * Get the stored PIN hash.
     * @return The PIN hash or null if not set
     */
    public String getPinHash() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_CONFIG, new String[]{COL_CONFIG_PIN_HASH},
                COL_CONFIG_ID + " = ?", new String[]{"1"}, null, null, null);
        
        String hash = null;
        if (cursor.moveToFirst()) {
            hash = cursor.getString(cursor.getColumnIndexOrThrow(COL_CONFIG_PIN_HASH));
        }
        cursor.close();
        return hash;
    }

    /**
     * Check if this is the first time the app is launched.
     * @return true if first time (PIN not set)
     */
    public boolean isFirstTime() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_CONFIG, new String[]{COL_CONFIG_IS_FIRST_TIME},
                COL_CONFIG_ID + " = ?", new String[]{"1"}, null, null, null);
        
        boolean isFirstTime = true;
        if (cursor.moveToFirst()) {
            isFirstTime = cursor.getInt(cursor.getColumnIndexOrThrow(COL_CONFIG_IS_FIRST_TIME)) == 1;
        }
        cursor.close();
        return isFirstTime;
    }

    // ===================== VAULT FILE OPERATIONS =====================

    /**
     * Save a file to the vault.
     * @param file The VaultFile object to save
     * @return The row ID of the inserted file
     */
    public long saveFile(VaultFile file) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_FILE_NAME, file.getFileName());
        values.put(COL_FILE_EXTENSION, file.getOriginalExtension());
        values.put(COL_FILE_DATA, file.getFileData());
        values.put(COL_FILE_SIZE, file.getFileSize());
        values.put(COL_FILE_UPLOADED_AT, dateFormat.format(new Date()));
        return db.insert(TABLE_VAULT_FILES, null, values);
    }

    /**
     * Get all files in the vault (without file data for performance).
     * @return List of VaultFile objects
     */
    public List<VaultFile> getAllFiles() {
        List<VaultFile> files = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        String[] columns = {COL_FILE_ID, COL_FILE_NAME, COL_FILE_EXTENSION, 
                           COL_FILE_SIZE, COL_FILE_UPLOADED_AT};
        Cursor cursor = db.query(TABLE_VAULT_FILES, columns, null, null, null, null,
                COL_FILE_UPLOADED_AT + " DESC");

        while (cursor.moveToNext()) {
            VaultFile file = new VaultFile();
            file.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_FILE_ID)));
            file.setFileName(cursor.getString(cursor.getColumnIndexOrThrow(COL_FILE_NAME)));
            file.setOriginalExtension(cursor.getString(cursor.getColumnIndexOrThrow(COL_FILE_EXTENSION)));
            file.setFileSize(cursor.getLong(cursor.getColumnIndexOrThrow(COL_FILE_SIZE)));
            file.setUploadedAt(cursor.getString(cursor.getColumnIndexOrThrow(COL_FILE_UPLOADED_AT)));
            files.add(file);
        }
        cursor.close();
        return files;
    }

    /**
     * Get file data by ID.
     * @param id The file ID
     * @return The file data as byte array
     */
    public byte[] getFileData(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_VAULT_FILES, new String[]{COL_FILE_DATA},
                COL_FILE_ID + " = ?", new String[]{String.valueOf(id)}, null, null, null);
        
        byte[] data = null;
        if (cursor.moveToFirst()) {
            data = cursor.getBlob(cursor.getColumnIndexOrThrow(COL_FILE_DATA));
        }
        cursor.close();
        return data;
    }

    /**
     * Get a complete VaultFile by ID.
     * @param id The file ID
     * @return The VaultFile object or null
     */
    public VaultFile getFileById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_VAULT_FILES, null,
                COL_FILE_ID + " = ?", new String[]{String.valueOf(id)}, null, null, null);
        
        VaultFile file = null;
        if (cursor.moveToFirst()) {
            file = new VaultFile();
            file.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_FILE_ID)));
            file.setFileName(cursor.getString(cursor.getColumnIndexOrThrow(COL_FILE_NAME)));
            file.setOriginalExtension(cursor.getString(cursor.getColumnIndexOrThrow(COL_FILE_EXTENSION)));
            file.setFileData(cursor.getBlob(cursor.getColumnIndexOrThrow(COL_FILE_DATA)));
            file.setFileSize(cursor.getLong(cursor.getColumnIndexOrThrow(COL_FILE_SIZE)));
            file.setUploadedAt(cursor.getString(cursor.getColumnIndexOrThrow(COL_FILE_UPLOADED_AT)));
        }
        cursor.close();
        return file;
    }

    /**
     * Delete a file from the vault.
     * @param id The file ID to delete
     */
    public void deleteFile(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_VAULT_FILES, COL_FILE_ID + " = ?", new String[]{String.valueOf(id)});
    }

    /**
     * Update all files' encryption when PIN is changed.
     * @param oldPin The old PIN
     * @param newPin The new PIN
     */
    public void updateAllFilesEncryption(String oldPin, String newPin) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.query(TABLE_VAULT_FILES, new String[]{COL_FILE_ID, COL_FILE_DATA},
                null, null, null, null, null);

        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_FILE_ID));
            byte[] encryptedData = cursor.getBlob(cursor.getColumnIndexOrThrow(COL_FILE_DATA));
            
            // Decrypt with old PIN
            byte[] decryptedData = CryptoUtils.decryptData(encryptedData, oldPin);
            // Re-encrypt with new PIN
            byte[] newEncryptedData = CryptoUtils.encryptData(decryptedData, newPin);
            
            ContentValues values = new ContentValues();
            values.put(COL_FILE_DATA, newEncryptedData);
            db.update(TABLE_VAULT_FILES, values, COL_FILE_ID + " = ?", new String[]{String.valueOf(id)});
        }
        cursor.close();
    }

    // ===================== CALCULATION HISTORY OPERATIONS =====================

    /**
     * Save a calculation to history.
     * @param expression The calculation expression
     * @param result The calculation result
     * @return The ID of the inserted row, or -1 if error
     */
    public long saveCalculation(String expression, String result) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_HISTORY_EXPRESSION, expression);
        values.put(COL_HISTORY_RESULT, result);
        values.put(COL_HISTORY_CALCULATED_AT, dateFormat.format(new Date()));
        return db.insert(TABLE_CALCULATION_HISTORY, null, values);
    }

    /**
     * Get all calculation history.
     * @return List of CalculationHistory objects
     */
    public List<CalculationHistory> getCalculationHistory() {
        List<CalculationHistory> history = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_CALCULATION_HISTORY, null, null, null, null, null,
                COL_HISTORY_CALCULATED_AT + " DESC");

        while (cursor.moveToNext()) {
            CalculationHistory item = new CalculationHistory();
            item.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COL_HISTORY_ID)));
            item.setExpression(cursor.getString(cursor.getColumnIndexOrThrow(COL_HISTORY_EXPRESSION)));
            item.setResult(cursor.getString(cursor.getColumnIndexOrThrow(COL_HISTORY_RESULT)));
            item.setCalculatedAt(cursor.getString(cursor.getColumnIndexOrThrow(COL_HISTORY_CALCULATED_AT)));
            history.add(item);
        }
        cursor.close();
        return history;
    }

    /**
     * Delete a single calculation from history.
     * @param id The calculation ID to delete
     */
    public void deleteCalculation(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CALCULATION_HISTORY, COL_HISTORY_ID + " = ?", new String[]{String.valueOf(id)});
    }

    /**
     * Clear all calculation history.
     */
    public void clearAllHistory() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CALCULATION_HISTORY, null, null);
    }
}
