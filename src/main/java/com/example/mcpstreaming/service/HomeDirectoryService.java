package com.example.mcpstreaming.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for managing home directory paths dynamically.
 * Uses system properties with Spring property override capability.
 */
@Service
public class HomeDirectoryService {
    
    private final String homeDirectory;
    
    public HomeDirectoryService(@Value("${app.user.home:#{systemProperties['user.home']}}") String homeDirectory) {
        this.homeDirectory = homeDirectory;
    }
    
    /**
     * Get the user's home directory path.
     * @return The home directory path (e.g., "/Users/chris" on macOS, "/home/username" on Linux)
     */
    public String getHomeDirectory() {
        return homeDirectory;
    }
    
    /**
     * Check if a path starts with the home directory.
     * @param path The path to check
     * @return true if the path starts with the home directory
     */
    public boolean isHomePath(String path) {
        return path != null && path.startsWith(homeDirectory);
    }
    
    /**
     * Convert a tilde path to absolute path.
     * @param path Path that may start with ~ or ~/
     * @return Absolute path with home directory resolved
     */
    public String expandTildePath(String path) {
        if (path == null) {
            return homeDirectory;
        }
        if ("~".equals(path) || "~/".equals(path)) {
            return homeDirectory;
        }
        if (path.startsWith("~/")) {
            return path.replace("~/", homeDirectory + "/");
        }
        return path;
    }
    
    /**
     * Get a path relative to the home directory.
     * @param relativePath The relative path from home directory
     * @return Absolute path within the home directory
     */
    public String getHomeRelativePath(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return homeDirectory;
        }
        // Remove leading slash if present
        String cleanPath = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
        return homeDirectory + "/" + cleanPath;
    }
}