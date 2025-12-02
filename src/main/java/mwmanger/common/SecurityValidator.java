package mwmanger.common;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Security validation utilities for command execution and file path validation.
 * Prevents command injection and path traversal attacks.
 */
public class SecurityValidator {

    // Dangerous characters that could be used for command injection
    private static final Pattern DANGEROUS_CHARS = Pattern.compile("[;&|`$(){}\\[\\]<>\\n\\r]");

    // Path traversal patterns
    private static final Pattern PATH_TRAVERSAL = Pattern.compile("\\.\\.[\\\\/]");

    /**
     * Validates command parameters to prevent command injection.
     * Checks for dangerous shell metacharacters.
     *
     * @param param The parameter to validate
     * @return true if the parameter is safe, false otherwise
     */
    public static boolean isValidCommandParam(String param) {
        if (param == null || param.isEmpty()) {
            return true;
        }
        return !DANGEROUS_CHARS.matcher(param).find();
    }

    /**
     * Sanitizes a command parameter by removing dangerous characters.
     *
     * @param param The parameter to sanitize
     * @return The sanitized parameter
     */
    public static String sanitizeCommandParam(String param) {
        if (param == null) {
            return "";
        }
        return DANGEROUS_CHARS.matcher(param).replaceAll("");
    }

    /**
     * Validates a file path to prevent path traversal attacks.
     *
     * @param basePath The base directory path
     * @param userPath The user-provided path to validate
     * @return true if the path is safe (within base directory), false otherwise
     */
    public static boolean isValidPath(String basePath, String userPath) {
        if (userPath == null || userPath.isEmpty()) {
            return false;
        }

        // Check for obvious path traversal patterns
        if (PATH_TRAVERSAL.matcher(userPath).find()) {
            return false;
        }

        try {
            File baseDir = new File(basePath).getCanonicalFile();
            File targetFile = new File(basePath, userPath).getCanonicalFile();

            // Ensure the target is within the base directory
            return targetFile.getPath().startsWith(baseDir.getPath());
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Validates an absolute file path to prevent path traversal attacks.
     * Checks if the path is within allowed directories.
     *
     * @param absolutePath The absolute path to validate
     * @param allowedBasePaths Array of allowed base directories
     * @return true if the path is within allowed directories, false otherwise
     */
    public static boolean isValidAbsolutePath(String absolutePath, String... allowedBasePaths) {
        if (absolutePath == null || absolutePath.isEmpty()) {
            return false;
        }

        // Check for path traversal patterns
        if (PATH_TRAVERSAL.matcher(absolutePath).find()) {
            return false;
        }

        try {
            File targetFile = new File(absolutePath).getCanonicalFile();

            for (String basePath : allowedBasePaths) {
                File baseDir = new File(basePath).getCanonicalFile();
                if (targetFile.getPath().startsWith(baseDir.getPath())) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Gets the canonical path, preventing path traversal.
     *
     * @param basePath The base directory
     * @param userPath The user-provided relative path
     * @return The validated canonical path
     * @throws SecurityException if path traversal is detected
     */
    public static String getValidatedPath(String basePath, String userPath) throws SecurityException {
        if (!isValidPath(basePath, userPath)) {
            throw new SecurityException("Path traversal detected: " + userPath);
        }

        try {
            return new File(basePath, userPath).getCanonicalPath();
        } catch (IOException e) {
            throw new SecurityException("Invalid path: " + userPath, e);
        }
    }

    /**
     * Validates a filename (no directory components allowed).
     *
     * @param filename The filename to validate
     * @return true if it's a valid filename without path components
     */
    public static boolean isValidFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }

        // Check for path separators
        if (filename.contains("/") || filename.contains("\\")) {
            return false;
        }

        // Check for path traversal
        if (filename.equals("..") || filename.equals(".")) {
            return false;
        }

        return true;
    }
}
