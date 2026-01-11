package com.example.calculator_vault_androidapp.models;

/**
 * Model class representing a file stored in the vault.
 */
public class VaultFile {
    private Integer id;
    private String fileName;
    private String originalExtension;
    private byte[] fileData;
    private long fileSize;
    private String uploadedAt;

    public VaultFile() {}

    public VaultFile(Integer id, String fileName, String originalExtension,
                     byte[] fileData, long fileSize, String uploadedAt) {
        this.id = id;
        this.fileName = fileName;
        this.originalExtension = originalExtension;
        this.fileData = fileData;
        this.fileSize = fileSize;
        this.uploadedAt = uploadedAt;
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getOriginalExtension() { return originalExtension; }
    public void setOriginalExtension(String originalExtension) { this.originalExtension = originalExtension; }

    public byte[] getFileData() { return fileData; }
    public void setFileData(byte[] fileData) { this.fileData = fileData; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public String getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(String uploadedAt) { this.uploadedAt = uploadedAt; }
}
