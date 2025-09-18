package com.example.mcpstreaming.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

/**
 * Result DTOs for various file operations.
 */
public class FileOperationResult {
    
    /**
     * Result for directory listing operations.
     */
    public static class DirectoryListing {
        @JsonProperty("path")
        private String path;
        
        @JsonProperty("files")
        private List<FileInfo> files;
        
        @JsonProperty("total_count")
        private int totalCount;
        
        public DirectoryListing() {}
        
        public DirectoryListing(String path, List<FileInfo> files) {
            this.path = path;
            this.files = files;
            this.totalCount = files.size();
        }
        
        // Getters and setters
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public List<FileInfo> getFiles() { return files; }
        public void setFiles(List<FileInfo> files) { 
            this.files = files; 
            this.totalCount = files != null ? files.size() : 0;
        }
        
        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    }
    
    /**
     * Information about a single file or directory.
     */
    public static class FileInfo {
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("path")
        private String path;
        
        @JsonProperty("type")
        private String type; // "file" or "directory"
        
        @JsonProperty("size")
        private long size;
        
        @JsonProperty("last_modified")
        private Instant lastModified;
        
        @JsonProperty("permissions")
        private String permissions;
        
        public FileInfo() {}
        
        public FileInfo(String name, String path, String type, long size, Instant lastModified, String permissions) {
            this.name = name;
            this.path = path;
            this.type = type;
            this.size = size;
            this.lastModified = lastModified;
            this.permissions = permissions;
        }
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        
        public Instant getLastModified() { return lastModified; }
        public void setLastModified(Instant lastModified) { this.lastModified = lastModified; }
        
        public String getPermissions() { return permissions; }
        public void setPermissions(String permissions) { this.permissions = permissions; }
    }
    
    /**
     * Result for file content operations (cat, read).
     */
    public static class FileContent {
        @JsonProperty("path")
        private String path;
        
        @JsonProperty("content")
        private String content;
        
        @JsonProperty("size")
        private long size;
        
        @JsonProperty("encoding")
        private String encoding = "UTF-8";
        
        public FileContent() {}
        
        public FileContent(String path, String content) {
            this.path = path;
            this.content = content;
            this.size = content != null ? content.length() : 0;
        }
        
        // Getters and setters
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public String getContent() { return content; }
        public void setContent(String content) { 
            this.content = content; 
            this.size = content != null ? content.length() : 0;
        }
        
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        
        public String getEncoding() { return encoding; }
        public void setEncoding(String encoding) { this.encoding = encoding; }
    }
    
    /**
     * Result for file modification operations (create, edit, append).
     */
    public static class FileModification {
        @JsonProperty("path")
        private String path;
        
        @JsonProperty("operation")
        private String operation; // "create", "edit", "append"
        
        @JsonProperty("success")
        private boolean success;
        
        @JsonProperty("message")
        private String message;
        
        @JsonProperty("bytes_written")
        private long bytesWritten;
        
        public FileModification() {}
        
        public FileModification(String path, String operation, boolean success, String message, long bytesWritten) {
            this.path = path;
            this.operation = operation;
            this.success = success;
            this.message = message;
            this.bytesWritten = bytesWritten;
        }
        
        // Getters and setters
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public long getBytesWritten() { return bytesWritten; }
        public void setBytesWritten(long bytesWritten) { this.bytesWritten = bytesWritten; }
    }
}