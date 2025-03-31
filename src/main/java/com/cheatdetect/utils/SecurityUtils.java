package com.cheatdetect.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for security-related operations.
 */
public class SecurityUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Generates a secure random token.
     *
     * @param length the length of the token
     * @return the generated token
     */
    public static String generateSecureToken(int length) {
        byte[] randomBytes = new byte[length];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Calculates the SHA-256 hash of a string.
     *
     * @param input the input string
     * @return the SHA-256 hash
     */
    public static String calculateSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // Convert bytes to hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (byte hashByte : hashBytes) {
                String hex = Integer.toHexString(0xff & hashByte);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            Logger.error("SHA-256 algorithm not available", e);
            return "";
        }
    }

    /**
     * Sanitizes potentially sensitive information for logging.
     * This masks or truncates sensitive data to prevent leakage in logs.
     *
     * @param data the data to sanitize
     * @return the sanitized data
     */
    public static String sanitizeForLogging(String data) {
        if (data == null || data.isEmpty()) {
            return "";
        }

        // Truncate long strings
        if (data.length() > 100) {
            return data.substring(0, 97) + "...";
        }

        return data;
    }

    /**
     * Verifies if a given string might be a code snippet.
     *
     * @param content the content to check
     * @return true if the content appears to be code, false otherwise
     */
    public static boolean isLikelyCode(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }

        // Simple heuristic to detect code patterns
        String[] codeIndicators = {
                "public ", "private ", "protected ", "class ", "function ", "def ",
                "import ", "package ", "var ", "const ", "let ", "if(", "if (", "for(",
                "for (", "while(", "while (", "{", "}", ";", "=>", "->", "===", "!=="
        };

        for (String indicator : codeIndicators) {
            if (content.contains(indicator)) {
                return true;
            }
        }

        // Check for indentation patterns common in code
        String[] lines = content.split("\\r?\\n");
        int indentedLines = 0;

        for (String line : lines) {
            if (line.startsWith("    ") || line.startsWith("\t")) {
                indentedLines++;
            }
        }

        // If more than 30% of lines are indented, likely code
        return lines.length > 3 && (double) indentedLines / lines.length > 0.3;
    }

    /**
     * Prevents command injection by sanitizing command arguments.
     *
     * @param command the command to sanitize
     * @return the sanitized command
     */
    public static String sanitizeCommand(String command) {
        if (command == null) {
            return "";
        }

        // Remove potentially dangerous characters
        return command.replaceAll("[;&|<>`$\\\\]", "");
    }
}