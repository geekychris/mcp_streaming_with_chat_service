package com.example.mcpstreaming.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Result DTOs for command execution and grep operations.
 */
public class CommandResult {
    
    /**
     * Result for command execution operations.
     */
    public static class CommandExecution {
        @JsonProperty("command")
        private String command;
        
        @JsonProperty("exit_code")
        private int exitCode;
        
        @JsonProperty("stdout")
        private String stdout;
        
        @JsonProperty("stderr")
        private String stderr;
        
        @JsonProperty("execution_time_ms")
        private long executionTimeMs;
        
        @JsonProperty("success")
        private boolean success;
        
        public CommandExecution() {}
        
        public CommandExecution(String command, int exitCode, String stdout, String stderr, long executionTimeMs) {
            this.command = command;
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
            this.executionTimeMs = executionTimeMs;
            this.success = exitCode == 0;
        }
        
        // Getters and setters
        public String getCommand() { return command; }
        public void setCommand(String command) { this.command = command; }
        
        public int getExitCode() { return exitCode; }
        public void setExitCode(int exitCode) { 
            this.exitCode = exitCode; 
            this.success = exitCode == 0;
        }
        
        public String getStdout() { return stdout; }
        public void setStdout(String stdout) { this.stdout = stdout; }
        
        public String getStderr() { return stderr; }
        public void setStderr(String stderr) { this.stderr = stderr; }
        
        public long getExecutionTimeMs() { return executionTimeMs; }
        public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
    }
    
    /**
     * Result for grep operations.
     */
    public static class GrepResult {
        @JsonProperty("pattern")
        private String pattern;
        
        @JsonProperty("path")
        private String path;
        
        @JsonProperty("recursive")
        private boolean recursive;
        
        @JsonProperty("matches")
        private List<GrepMatch> matches;
        
        @JsonProperty("total_matches")
        private int totalMatches;
        
        @JsonProperty("files_searched")
        private int filesSearched;
        
        public GrepResult() {}
        
        public GrepResult(String pattern, String path, boolean recursive, List<GrepMatch> matches, int filesSearched) {
            this.pattern = pattern;
            this.path = path;
            this.recursive = recursive;
            this.matches = matches;
            this.totalMatches = matches.size();
            this.filesSearched = filesSearched;
        }
        
        // Getters and setters
        public String getPattern() { return pattern; }
        public void setPattern(String pattern) { this.pattern = pattern; }
        
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public boolean isRecursive() { return recursive; }
        public void setRecursive(boolean recursive) { this.recursive = recursive; }
        
        public List<GrepMatch> getMatches() { return matches; }
        public void setMatches(List<GrepMatch> matches) { 
            this.matches = matches; 
            this.totalMatches = matches != null ? matches.size() : 0;
        }
        
        public int getTotalMatches() { return totalMatches; }
        public void setTotalMatches(int totalMatches) { this.totalMatches = totalMatches; }
        
        public int getFilesSearched() { return filesSearched; }
        public void setFilesSearched(int filesSearched) { this.filesSearched = filesSearched; }
    }
    
    /**
     * Individual grep match result.
     */
    public static class GrepMatch {
        @JsonProperty("file_path")
        private String filePath;
        
        @JsonProperty("line_number")
        private int lineNumber;
        
        @JsonProperty("line_content")
        private String lineContent;
        
        @JsonProperty("match_start")
        private int matchStart;
        
        @JsonProperty("match_end")
        private int matchEnd;
        
        @JsonProperty("matched_text")
        private String matchedText;
        
        public GrepMatch() {}
        
        public GrepMatch(String filePath, int lineNumber, String lineContent, int matchStart, int matchEnd, String matchedText) {
            this.filePath = filePath;
            this.lineNumber = lineNumber;
            this.lineContent = lineContent;
            this.matchStart = matchStart;
            this.matchEnd = matchEnd;
            this.matchedText = matchedText;
        }
        
        // Getters and setters
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        
        public int getLineNumber() { return lineNumber; }
        public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }
        
        public String getLineContent() { return lineContent; }
        public void setLineContent(String lineContent) { this.lineContent = lineContent; }
        
        public int getMatchStart() { return matchStart; }
        public void setMatchStart(int matchStart) { this.matchStart = matchStart; }
        
        public int getMatchEnd() { return matchEnd; }
        public void setMatchEnd(int matchEnd) { this.matchEnd = matchEnd; }
        
        public String getMatchedText() { return matchedText; }
        public void setMatchedText(String matchedText) { this.matchedText = matchedText; }
    }
}