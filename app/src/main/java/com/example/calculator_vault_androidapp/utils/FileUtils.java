package com.example.calculator_vault_androidapp.utils;

import android.content.Context;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for file operations.
 */
public class FileUtils {
    
    private static final List<File> TEMP_FILES = new ArrayList<>();

    /**
     * Format file size to human-readable string.
     * @param bytes Size in bytes
     * @return Formatted string (e.g., "1.5 KB" or "2.3 MB")
     */
    public static String formatFileSize(long bytes) {
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format("%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        return String.format("%.1f MB", mb);
    }

    /**
     * Get file extension from file name.
     * @param fileName The file name
     * @return The extension without dot, or empty string if none
     */
    public static String getExtension(String fileName) {
        if (fileName == null) return "";
        int idx = fileName.lastIndexOf('.');
        return idx >= 0 ? fileName.substring(idx + 1).toLowerCase() : "";
    }

    /**
     * Get file name without extension.
     * @param fileName The file name
     * @return The file name without extension
     */
    public static String getNameWithoutExtension(String fileName) {
        if (fileName == null) return "";
        int idx = fileName.lastIndexOf('.');
        return idx >= 0 ? fileName.substring(0, idx) : fileName;
    }

    /**
     * Create a temporary file with the given data.
     * @param context Android context
     * @param fileName The file name
     * @param data The file data
     * @return The created temp file
     * @throws IOException If file creation fails
     */
    public static File createTempFile(Context context, String fileName, byte[] data) throws IOException {
        File tempDir = new File(context.getCacheDir(), "vault_temp");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        File tempFile = new File(tempDir, fileName);
        FileOutputStream fos = new FileOutputStream(tempFile);
        fos.write(data);
        fos.close();

        synchronized (TEMP_FILES) {
            TEMP_FILES.add(tempFile);
        }
        return tempFile;
    }

    /**
     * Delete all temporary files created by the vault.
     */
    public static void deleteTempFiles() {
        synchronized (TEMP_FILES) {
            for (File f : new ArrayList<>(TEMP_FILES)) {
                if (f != null && f.exists()) {
                    f.delete();
                }
            }
            TEMP_FILES.clear();
        }
    }

    /**
     * Delete the vault temp directory.
     * @param context Android context
     */
    public static void clearTempDirectory(Context context) {
        File tempDir = new File(context.getCacheDir(), "vault_temp");
        if (tempDir.exists() && tempDir.isDirectory()) {
            File[] files = tempDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
        synchronized (TEMP_FILES) {
            TEMP_FILES.clear();
        }
    }

    /**
     * Get MIME type from file extension.
     * @param extension The file extension
     * @return The MIME type or "application/octet-stream" if unknown
     */
    public static String getMimeType(String extension) {
        if (extension == null || extension.isEmpty()) {
            return "application/octet-stream";
        }
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
        return mimeType != null ? mimeType : "application/octet-stream";
    }

    /**
     * Check if the file type is an image.
     * @param extension The file extension
     * @return true if image type
     */
    public static boolean isImage(String extension) {
        if (extension == null) return false;
        String ext = extension.toLowerCase();
        return ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") ||
               ext.equals("gif") || ext.equals("bmp") || ext.equals("webp");
    }

    /**
     * Check if the file type is a video.
     * @param extension The file extension
     * @return true if video type
     */
    public static boolean isVideo(String extension) {
        if (extension == null) return false;
        String ext = extension.toLowerCase();
        return ext.equals("mp4") || ext.equals("avi") || ext.equals("mkv") ||
               ext.equals("mov") || ext.equals("wmv") || ext.equals("flv") || ext.equals("webm");
    }

    /**
     * Check if the file type is audio.
     * @param extension The file extension
     * @return true if audio type
     */
    public static boolean isAudio(String extension) {
        if (extension == null) return false;
        String ext = extension.toLowerCase();
        return ext.equals("mp3") || ext.equals("wav") || ext.equals("aac") ||
               ext.equals("flac") || ext.equals("ogg") || ext.equals("m4a");
    }

    /**
     * Check if the file type is a document.
     * @param extension The file extension
     * @return true if document type
     */
    public static boolean isDocument(String extension) {
        if (extension == null) return false;
        String ext = extension.toLowerCase();
        return ext.equals("pdf") || ext.equals("doc") || ext.equals("docx") ||
               ext.equals("xls") || ext.equals("xlsx") || ext.equals("ppt") ||
               ext.equals("pptx") || ext.equals("txt") || ext.equals("rtf");
    }
}
