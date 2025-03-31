package com.cheatdetect.detectors;

import com.cheatdetect.api.EventListener;
import com.cheatdetect.core.Configuration;
import com.cheatdetect.platform.PlatformInterface;
import com.cheatdetect.utils.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Monitors clipboard activity to detect suspicious copy-paste operations.
 */
public class ClipboardMonitor {
    private final Configuration config;
    private final PlatformInterface platform;
    private final Set<EventListener> listeners;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean isRunning;
    private final AtomicInteger clipboardActivityCount;
    private String lastClipboardContent;

    /**
     * Creates a new ClipboardMonitor.
     *
     * @param config   the configuration to use
     * @param platform the platform implementation
     */
    public ClipboardMonitor(Configuration config, PlatformInterface platform) {
        this.config = config;
        this.platform = platform;
        this.listeners = new HashSet<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.isRunning = new AtomicBoolean(false);
        this.clipboardActivityCount = new AtomicInteger(0);
        this.lastClipboardContent = "";
    }

    /**
     * Starts monitoring clipboard activity.
     */
    public void startMonitoring() {
        if (isRunning.compareAndSet(false, true)) {
            Logger.info("Starting clipboard monitoring");

            scheduler.scheduleAtFixedRate(() -> {
                try {
                    checkClipboard();
                } catch (Exception e) {
                    Logger.error("Error monitoring clipboard", e);
                }
            }, 0, 1000, TimeUnit.MILLISECONDS); // Check every second
        }
    }

    /**
     * Stops monitoring clipboard activity.
     */
    public void stopMonitoring() {
        if (isRunning.compareAndSet(true, false)) {
            Logger.info("Stopping clipboard monitoring");
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Checks the clipboard for changes.
     */
    private void checkClipboard() {
        try {
            String currentContent = platform.getClipboardContent();
            if (currentContent == null) {
                return;
            }

            // Detect changes
            if (!currentContent.equals(lastClipboardContent) && !currentContent.isEmpty()) {
                if (config.isDetailedLoggingEnabled()) {
                    // Log a small preview to avoid exposing sensitive information
                    String preview = currentContent.length() > 50
                            ? currentContent.substring(0, 50) + "..."
                            : currentContent;
                    Logger.debug("Clipboard changed: " + preview);
                }

                // Track clipboard activity frequency
                int count = clipboardActivityCount.incrementAndGet();
                lastClipboardContent = currentContent;

                // Alert on high frequency or large content
                if (count > 5) {
                    notifyListeners("ALERT_CLIPBOARD_FREQUENCY",
                            "High clipboard activity detected: " + count + " changes in a short period");
                    clipboardActivityCount.set(0); // Reset counter
                }

                // Alert on code-like content
                if (containsCodePatterns(currentContent)) {
                    notifyListeners("ALERT_CLIPBOARD_CODE",
                            "Code-like content detected in clipboard");
                }

                // Alert on large content
                if (currentContent.length() > 500) {
                    notifyListeners("INFO_CLIPBOARD_LARGE",
                            "Large content copied to clipboard: " + currentContent.length() + " characters");
                }
            }
        } catch (Exception e) {
            Logger.error("Error checking clipboard", e);
        }
    }

    /**
     * Checks if the content contains patterns typically found in code.
     *
     * @param content the content to check
     * @return true if the content likely contains code, false otherwise
     */
    private boolean containsCodePatterns(String content) {
        // Simplified check for common code patterns
        return content.contains("public ") ||
                content.contains("private ") ||
                content.contains("function ") ||
                content.contains("class ") ||
                content.contains("def ") ||
                content.contains("import ") ||
                content.contains("return ") ||
                content.contains("for(") ||
                content.contains("for (") ||
                content.contains("while(") ||
                content.contains("while (") ||
                content.contains("if(") ||
                content.contains("if (");
    }

    /**
     * Registers an event listener to receive detection events.
     *
     * @param listener the listener to register
     */
    public void registerEventListener(EventListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Unregisters a previously registered event listener.
     *
     * @param listener the listener to unregister
     */
    public void unregisterEventListener(EventListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notifies all registered listeners of an event.
     *
     * @param eventType the type of the event
     * @param details   details about the event
     */
    private void notifyListeners(String eventType, String details) {
        for (EventListener listener : listeners) {
            try {
                listener.onEvent(eventType, details);
            } catch (Exception e) {
                Logger.error("Error in event listener", e);
            }
        }
    }
}