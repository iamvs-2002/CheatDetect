package com.cheatdetect.utils;

import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger("CheatDetect");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static boolean detailedLoggingEnabled = false;
    private static CustomLogger customLogger = null;

    public static void setCustomLogger(CustomLogger logger) {
        customLogger = logger;
    }

    /**
     * Logs an info message.
     *
     * @param message the message to log
     */
    public static void info(String message) {
        LOG.info(message);
        logToConsole("INFO", message);
        if (customLogger != null) {
            customLogger.log("INFO", message);
        }
    }

    /**
     * Logs a warning message.
     *
     * @param message the message to log
     */
    public static void warn(String message) {
        LOG.warn(message);
        logToConsole("WARN", message);
        if (customLogger != null) {
            customLogger.log("WARN", message);
        }
    }

    /**
     * Logs an error message.
     *
     * @param message the message to log
     */
    public static void error(String message) {
        LOG.error(message);
        logToConsole("ERROR", message);
        if (customLogger != null) {
            customLogger.log("ERROR", message);
        }
    }

    /**
     * Logs an error message with an exception.
     *
     * @param message the message to log
     * @param e       the exception to log
     */
    public static void error(String message, Throwable e) {
        LOG.error(message, e);
        logToConsole("ERROR", message + ": " + e.getMessage());
        if (customLogger != null) {
            customLogger.log("ERROR", message + ": " + e.getMessage());
        }
    }

    /**
     * Logs a debug message.
     * These messages are only logged if detailed logging is enabled.
     *
     * @param message the message to log
     */
    public static void debug(String message) {
        if (detailedLoggingEnabled) {
            LOG.debug(message);
            logToConsole("DEBUG", message);
            if (customLogger != null) {
                customLogger.log("DEBUG", message);
            }
        }
    }

    /**
     * Sets whether detailed logging is enabled.
     *
     * @param enabled true to enable detailed logging, false otherwise
     */
    public static void setDetailedLoggingEnabled(boolean enabled) {
        detailedLoggingEnabled = enabled;
    }

    /**
     * Checks if detailed logging is enabled.
     *
     * @return true if detailed logging is enabled, false otherwise
     */
    public static boolean isDetailedLoggingEnabled() {
        return detailedLoggingEnabled;
    }

    /**
     * Logs a message to the console with a timestamp and log level.
     *
     * @param level   the log level
     * @param message the message to log
     */
    private static void logToConsole(String level, String message) {
        String timestamp = DATE_FORMAT.format(new Date());
        System.out.println(timestamp + " [" + level + "] CheatDetect - " + message);
    }
}