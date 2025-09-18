package com.example.mcpstreaming.service;

import com.example.mcpstreaming.model.FileOperationResult.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class FileOperationServiceTest {
    
    private FileOperationService fileOperationService;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        fileOperationService = new FileOperationService();
    }
    
    @Test
    void testListDirectory() throws IOException {
        // Create test files
        Files.createFile(tempDir.resolve("test1.txt"));
        Files.createFile(tempDir.resolve("test2.txt"));
        Files.createDirectory(tempDir.resolve("subdir"));
        
        StepVerifier.create(fileOperationService.listDirectory(tempDir.toString()))
            .assertNext(listing -> {
                assertThat(listing.getPath()).isEqualTo(tempDir.toString());
                assertThat(listing.getFiles()).hasSize(3);
                assertThat(listing.getTotalCount()).isEqualTo(3);
                
                List<String> fileNames = listing.getFiles().stream()
                    .map(FileInfo::getName)
                    .toList();
                assertThat(fileNames).containsExactlyInAnyOrder("test1.txt", "test2.txt", "subdir");
            })
            .verifyComplete();
    }
    
    @Test
    void testListDirectoryStream() throws IOException {
        // Create test files
        Files.createFile(tempDir.resolve("test1.txt"));
        Files.createFile(tempDir.resolve("test2.txt"));
        
        StepVerifier.create(fileOperationService.listDirectoryStream(tempDir.toString()))
            .expectNextCount(2)
            .verifyComplete();
    }
    
    @Test
    void testCreateFile() {
        Path filePath = tempDir.resolve("newfile.txt");
        String content = "Hello, World!";
        
        StepVerifier.create(fileOperationService.createFile(filePath.toString(), content))
            .assertNext(result -> {
                assertThat(result.getPath()).isEqualTo(filePath.toString());
                assertThat(result.getOperation()).isEqualTo("create");
                assertThat(result.isSuccess()).isTrue();
                assertThat(result.getBytesWritten()).isEqualTo(content.getBytes().length);
            })
            .verifyComplete();
        
        // Verify file was created
        assertThat(Files.exists(filePath)).isTrue();
    }
    
    @Test
    void testReadFile() throws IOException {
        Path filePath = tempDir.resolve("testread.txt");
        String content = "Test content for reading";
        Files.writeString(filePath, content);
        
        StepVerifier.create(fileOperationService.readFile(filePath.toString()))
            .assertNext(result -> {
                assertThat(result.getPath()).isEqualTo(filePath.toString());
                assertThat(result.getContent()).isEqualTo(content);
                assertThat(result.getSize()).isEqualTo(content.length());
            })
            .verifyComplete();
    }
    
    @Test
    void testReadFileStream() throws IOException {
        Path filePath = tempDir.resolve("teststreamread.txt");
        String content = "A".repeat(2048); // Larger than chunk size to test streaming
        Files.writeString(filePath, content);
        
        StepVerifier.create(fileOperationService.readFileStream(filePath.toString()))
            .expectNextCount(2) // Should be split into 2 chunks (1024 each)
            .verifyComplete();
    }
    
    @Test
    void testEditFile() throws IOException {
        Path filePath = tempDir.resolve("testedit.txt");
        Files.writeString(filePath, "Original content");
        
        String newContent = "New content";
        StepVerifier.create(fileOperationService.editFile(filePath.toString(), newContent))
            .assertNext(result -> {
                assertThat(result.getPath()).isEqualTo(filePath.toString());
                assertThat(result.getOperation()).isEqualTo("edit");
                assertThat(result.isSuccess()).isTrue();
            })
            .verifyComplete();
        
        // Verify file was edited
        assertThat(Files.readString(filePath)).isEqualTo(newContent);
    }
    
    @Test
    void testAppendToFile() throws IOException {
        Path filePath = tempDir.resolve("testappend.txt");
        String originalContent = "Original content\n";
        Files.writeString(filePath, originalContent);
        
        String appendContent = "Appended content";
        StepVerifier.create(fileOperationService.appendToFile(filePath.toString(), appendContent))
            .assertNext(result -> {
                assertThat(result.getPath()).isEqualTo(filePath.toString());
                assertThat(result.getOperation()).isEqualTo("append");
                assertThat(result.isSuccess()).isTrue();
            })
            .verifyComplete();
        
        // Verify content was appended
        String expectedContent = originalContent + appendContent;
        assertThat(Files.readString(filePath)).isEqualTo(expectedContent);
    }
    
    @Test
    void testListNonExistentDirectory() {
        StepVerifier.create(fileOperationService.listDirectory("/non/existent/path"))
            .expectError(RuntimeException.class)
            .verify();
    }
    
    @Test
    void testReadNonExistentFile() {
        StepVerifier.create(fileOperationService.readFile("/non/existent/file.txt"))
            .expectError(RuntimeException.class)
            .verify();
    }
    
    @Test
    void testCreateFileWithDirectories() throws IOException {
        Path filePath = tempDir.resolve("subdir").resolve("subsubdir").resolve("newfile.txt");
        String content = "Content in nested directory";
        
        StepVerifier.create(fileOperationService.createFile(filePath.toString(), content))
            .assertNext(result -> {
                assertThat(result.isSuccess()).isTrue();
            })
            .verifyComplete();
        
        // Verify file and directories were created
        assertThat(Files.exists(filePath)).isTrue();
        try {
            assertThat(Files.readString(filePath)).isEqualTo(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}