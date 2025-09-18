package com.example.mcpstreaming.service;

import com.example.mcpstreaming.model.FileOperationResult.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * Service for handling file system operations with reactive support.
 * Provides methods for listing directories, reading/writing files, and managing file content.
 */
@Service
public class FileOperationService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileOperationService.class);
    private static final int STREAM_CHUNK_SIZE = 1024; // 1KB chunks for streaming
    
    /**
     * Lists files and directories in the specified path.
     */
    public Mono<DirectoryListing> listDirectory(String path) {
        return Mono.fromCallable(() -> {
            Path dirPath = Paths.get(path).normalize();
            
            if (!Files.exists(dirPath)) {
                throw new RuntimeException("Directory does not exist: " + path);
            }
            
            if (!Files.isDirectory(dirPath)) {
                throw new RuntimeException("Path is not a directory: " + path);
            }
            
            List<FileInfo> files = new ArrayList<>();
            
            try (Stream<Path> stream = Files.list(dirPath)) {
                stream.forEach(filePath -> {
                    try {
                        BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
                        String permissions = getPermissionsString(filePath);
                        
                        FileInfo fileInfo = new FileInfo(
                            filePath.getFileName().toString(),
                            filePath.toString(),
                            attrs.isDirectory() ? "directory" : "file",
                            attrs.size(),
                            attrs.lastModifiedTime().toInstant(),
                            permissions
                        );
                        
                        files.add(fileInfo);
                    } catch (IOException e) {
                        logger.warn("Error reading file attributes for {}: {}", filePath, e.getMessage());
                    }
                });
            }
            
            return new DirectoryListing(dirPath.toString(), files);
        });
    }
    
    /**
     * Streams directory listing for large directories.
     */
    public Flux<FileInfo> listDirectoryStream(String path) {
        return Flux.create(sink -> {
            Path dirPath = Paths.get(path).normalize();
            
            if (!Files.exists(dirPath)) {
                sink.error(new RuntimeException("Directory does not exist: " + path));
                return;
            }
            
            if (!Files.isDirectory(dirPath)) {
                sink.error(new RuntimeException("Path is not a directory: " + path));
                return;
            }
            
            try (Stream<Path> stream = Files.list(dirPath)) {
                stream.forEach(filePath -> {
                    try {
                        BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
                        String permissions = getPermissionsString(filePath);
                        
                        FileInfo fileInfo = new FileInfo(
                            filePath.getFileName().toString(),
                            filePath.toString(),
                            attrs.isDirectory() ? "directory" : "file",
                            attrs.size(),
                            attrs.lastModifiedTime().toInstant(),
                            permissions
                        );
                        
                        sink.next(fileInfo);
                    } catch (IOException e) {
                        logger.warn("Error reading file attributes for {}: {}", filePath, e.getMessage());
                    }
                });
                sink.complete();
            } catch (IOException e) {
                sink.error(e);
            }
        });
    }
    
    /**
     * Reads the content of a file.
     */
    public Mono<FileContent> readFile(String path) {
        return Mono.fromCallable(() -> {
            Path filePath = Paths.get(path).normalize();
            
            if (!Files.exists(filePath)) {
                throw new RuntimeException("File does not exist: " + path);
            }
            
            if (Files.isDirectory(filePath)) {
                throw new RuntimeException("Path is a directory, not a file: " + path);
            }
            
            try {
                String content = Files.readString(filePath);
                return new FileContent(filePath.toString(), content);
            } catch (IOException e) {
                throw new RuntimeException("Error reading file: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Streams file content in chunks for large files.
     */
    public Flux<String> readFileStream(String path) {
        return Flux.create(sink -> {
            Path filePath = Paths.get(path).normalize();
            
            if (!Files.exists(filePath)) {
                sink.error(new RuntimeException("File does not exist: " + path));
                return;
            }
            
            if (Files.isDirectory(filePath)) {
                sink.error(new RuntimeException("Path is a directory, not a file: " + path));
                return;
            }
            
            try {
                String content = Files.readString(filePath);
                
                // Split content into chunks
                int length = content.length();
                for (int i = 0; i < length; i += STREAM_CHUNK_SIZE) {
                    int end = Math.min(i + STREAM_CHUNK_SIZE, length);
                    sink.next(content.substring(i, end));
                }
                
                sink.complete();
            } catch (IOException e) {
                sink.error(new RuntimeException("Error reading file: " + e.getMessage(), e));
            }
        });
    }
    
    /**
     * Creates a new file with the specified content.
     */
    public Mono<FileModification> createFile(String path, String content) {
        return Mono.fromCallable(() -> {
            Path filePath = Paths.get(path).normalize();
            
            if (Files.exists(filePath)) {
                throw new RuntimeException("File already exists: " + path);
            }
            
            try {
                // Create parent directories if they don't exist
                Path parent = filePath.getParent();
                if (parent != null && !Files.exists(parent)) {
                    Files.createDirectories(parent);
                }
                
                Files.writeString(filePath, content);
                long bytesWritten = content.getBytes().length;
                
                return new FileModification(
                    filePath.toString(),
                    "create",
                    true,
                    "File created successfully",
                    bytesWritten
                );
            } catch (IOException e) {
                throw new RuntimeException("Error creating file: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Edits (overwrites) a file with new content.
     */
    public Mono<FileModification> editFile(String path, String content) {
        return Mono.fromCallable(() -> {
            Path filePath = Paths.get(path).normalize();
            
            if (!Files.exists(filePath)) {
                throw new RuntimeException("File does not exist: " + path);
            }
            
            if (Files.isDirectory(filePath)) {
                throw new RuntimeException("Path is a directory, not a file: " + path);
            }
            
            try {
                Files.writeString(filePath, content);
                long bytesWritten = content.getBytes().length;
                
                return new FileModification(
                    filePath.toString(),
                    "edit",
                    true,
                    "File edited successfully",
                    bytesWritten
                );
            } catch (IOException e) {
                throw new RuntimeException("Error editing file: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Appends content to an existing file.
     */
    public Mono<FileModification> appendToFile(String path, String content) {
        return Mono.fromCallable(() -> {
            Path filePath = Paths.get(path).normalize();
            
            if (!Files.exists(filePath)) {
                throw new RuntimeException("File does not exist: " + path);
            }
            
            if (Files.isDirectory(filePath)) {
                throw new RuntimeException("Path is a directory, not a file: " + path);
            }
            
            try {
                Files.writeString(filePath, content, StandardOpenOption.APPEND);
                long bytesWritten = content.getBytes().length;
                
                return new FileModification(
                    filePath.toString(),
                    "append",
                    true,
                    "Content appended successfully",
                    bytesWritten
                );
            } catch (IOException e) {
                throw new RuntimeException("Error appending to file: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Gets file permissions as a string (Unix-style).
     */
    private String getPermissionsString(Path filePath) {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                // Windows - simplified permissions
                boolean readable = Files.isReadable(filePath);
                boolean writable = Files.isWritable(filePath);
                boolean executable = Files.isExecutable(filePath);
                
                return (readable ? "r" : "-") + 
                       (writable ? "w" : "-") + 
                       (executable ? "x" : "-");
            } else {
                // Unix-like systems
                return PosixFilePermissions.toString(Files.getPosixFilePermissions(filePath));
            }
        } catch (Exception e) {
            return "unknown";
        }
    }
}