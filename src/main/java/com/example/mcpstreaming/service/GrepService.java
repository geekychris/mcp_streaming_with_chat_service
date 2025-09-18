package com.example.mcpstreaming.service;

import com.example.mcpstreaming.model.CommandResult.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Service for implementing grep functionality with pattern matching and streaming support.
 */
@Service
public class GrepService {
    
    private static final Logger logger = LoggerFactory.getLogger(GrepService.class);
    private static final int MAX_SEARCH_DEPTH = 10; // Prevent infinite recursion
    
    /**
     * Searches for patterns in files and returns all matches.
     */
    public Mono<GrepResult> grep(String patternStr, String path, boolean recursive, boolean caseSensitive) {
        return Mono.fromCallable(() -> {
            Path searchPath = Paths.get(path).normalize();
            
            if (!Files.exists(searchPath)) {
                throw new RuntimeException("Path does not exist: " + path);
            }
            
            List<GrepMatch> matches = new ArrayList<>();
            AtomicInteger filesSearched = new AtomicInteger(0);
            
            Pattern pattern = compilePattern(patternStr, caseSensitive);
            
            if (Files.isRegularFile(searchPath)) {
                // Search single file
                searchInFile(searchPath, pattern, matches);
                filesSearched.set(1);
            } else if (Files.isDirectory(searchPath)) {
                // Search directory
                if (recursive) {
                    searchDirectoryRecursive(searchPath, pattern, matches, filesSearched, 0);
                } else {
                    searchDirectory(searchPath, pattern, matches, filesSearched);
                }
            }
            
            return new GrepResult(patternStr, path, recursive, matches, filesSearched.get());
        });
    }
    
    /**
     * Streams grep results for large searches.
     */
    public Flux<GrepMatch> grepStream(String patternStr, String path, boolean recursive, boolean caseSensitive) {
        return Flux.create(sink -> {
            Path searchPath = Paths.get(path).normalize();
            
            if (!Files.exists(searchPath)) {
                sink.error(new RuntimeException("Path does not exist: " + path));
                return;
            }
            
            Pattern pattern = compilePattern(patternStr, caseSensitive);
            
            try {
                if (Files.isRegularFile(searchPath)) {
                    // Search single file
                    searchInFileStream(searchPath, pattern, sink);
                } else if (Files.isDirectory(searchPath)) {
                    // Search directory
                    if (recursive) {
                        searchDirectoryRecursiveStream(searchPath, pattern, sink, 0);
                    } else {
                        searchDirectoryStream(searchPath, pattern, sink);
                    }
                }
                sink.complete();
            } catch (Exception e) {
                sink.error(e);
            }
        });
    }
    
    /**
     * Compiles a regex pattern with appropriate flags.
     */
    private Pattern compilePattern(String patternStr, boolean caseSensitive) {
        int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
        try {
            return Pattern.compile(patternStr, flags);
        } catch (Exception e) {
            throw new RuntimeException("Invalid regex pattern: " + patternStr, e);
        }
    }
    
    /**
     * Searches for patterns in a single file.
     */
    private void searchInFile(Path filePath, Pattern pattern, List<GrepMatch> matches) {
        try {
            if (!isTextFile(filePath)) {
                return; // Skip binary files
            }
            
            List<String> lines = Files.readAllLines(filePath);
            
            for (int lineNumber = 0; lineNumber < lines.size(); lineNumber++) {
                String line = lines.get(lineNumber);
                Matcher matcher = pattern.matcher(line);
                
                while (matcher.find()) {
                    GrepMatch match = new GrepMatch(
                        filePath.toString(),
                        lineNumber + 1, // 1-based line numbers
                        line,
                        matcher.start(),
                        matcher.end(),
                        matcher.group()
                    );
                    matches.add(match);
                }
            }
        } catch (IOException e) {
            logger.warn("Error reading file {}: {}", filePath, e.getMessage());
        }
    }
    
    /**
     * Streams matches from a single file.
     */
    private void searchInFileStream(Path filePath, Pattern pattern, reactor.core.publisher.FluxSink<GrepMatch> sink) {
        try {
            if (!isTextFile(filePath)) {
                return; // Skip binary files
            }
            
            List<String> lines = Files.readAllLines(filePath);
            
            for (int lineNumber = 0; lineNumber < lines.size(); lineNumber++) {
                String line = lines.get(lineNumber);
                Matcher matcher = pattern.matcher(line);
                
                while (matcher.find()) {
                    GrepMatch match = new GrepMatch(
                        filePath.toString(),
                        lineNumber + 1, // 1-based line numbers
                        line,
                        matcher.start(),
                        matcher.end(),
                        matcher.group()
                    );
                    sink.next(match);
                }
            }
        } catch (IOException e) {
            logger.warn("Error reading file {}: {}", filePath, e.getMessage());
        }
    }
    
    /**
     * Searches files in a directory (non-recursive).
     */
    private void searchDirectory(Path dirPath, Pattern pattern, List<GrepMatch> matches, AtomicInteger filesSearched) {
        try (Stream<Path> stream = Files.list(dirPath)) {
            stream.filter(Files::isRegularFile)
                  .forEach(filePath -> {
                      filesSearched.incrementAndGet();
                      searchInFile(filePath, pattern, matches);
                  });
        } catch (IOException e) {
            logger.warn("Error listing directory {}: {}", dirPath, e.getMessage());
        }
    }
    
    /**
     * Streams matches from a directory (non-recursive).
     */
    private void searchDirectoryStream(Path dirPath, Pattern pattern, reactor.core.publisher.FluxSink<GrepMatch> sink) {
        try (Stream<Path> stream = Files.list(dirPath)) {
            stream.filter(Files::isRegularFile)
                  .forEach(filePath -> searchInFileStream(filePath, pattern, sink));
        } catch (IOException e) {
            logger.warn("Error listing directory {}: {}", dirPath, e.getMessage());
        }
    }
    
    /**
     * Searches files in a directory recursively.
     */
    private void searchDirectoryRecursive(Path dirPath, Pattern pattern, List<GrepMatch> matches, 
                                        AtomicInteger filesSearched, int depth) {
        if (depth > MAX_SEARCH_DEPTH) {
            logger.warn("Maximum search depth exceeded for directory: {}", dirPath);
            return;
        }
        
        try (Stream<Path> stream = Files.list(dirPath)) {
            stream.forEach(filePath -> {
                if (Files.isRegularFile(filePath)) {
                    filesSearched.incrementAndGet();
                    searchInFile(filePath, pattern, matches);
                } else if (Files.isDirectory(filePath)) {
                    searchDirectoryRecursive(filePath, pattern, matches, filesSearched, depth + 1);
                }
            });
        } catch (IOException e) {
            logger.warn("Error listing directory {}: {}", dirPath, e.getMessage());
        }
    }
    
    /**
     * Streams matches from a directory recursively.
     */
    private void searchDirectoryRecursiveStream(Path dirPath, Pattern pattern, 
                                              reactor.core.publisher.FluxSink<GrepMatch> sink, int depth) {
        if (depth > MAX_SEARCH_DEPTH) {
            logger.warn("Maximum search depth exceeded for directory: {}", dirPath);
            return;
        }
        
        try (Stream<Path> stream = Files.list(dirPath)) {
            stream.forEach(filePath -> {
                if (Files.isRegularFile(filePath)) {
                    searchInFileStream(filePath, pattern, sink);
                } else if (Files.isDirectory(filePath)) {
                    searchDirectoryRecursiveStream(filePath, pattern, sink, depth + 1);
                }
            });
        } catch (IOException e) {
            logger.warn("Error listing directory {}: {}", dirPath, e.getMessage());
        }
    }
    
    /**
     * Checks if a file is likely a text file (heuristic approach).
     */
    private boolean isTextFile(Path filePath) {
        try {
            // Simple heuristic: read first 512 bytes and check for null bytes
            byte[] bytes = Files.readAllBytes(filePath);
            if (bytes.length > 512) {
                byte[] sample = new byte[512];
                System.arraycopy(bytes, 0, sample, 0, 512);
                bytes = sample;
            }
            
            // Count null bytes - if more than 1% are null, consider it binary
            int nullCount = 0;
            for (byte b : bytes) {
                if (b == 0) {
                    nullCount++;
                }
            }
            
            return (bytes.length == 0) || ((double) nullCount / bytes.length) < 0.01;
        } catch (IOException e) {
            return false;
        }
    }
}