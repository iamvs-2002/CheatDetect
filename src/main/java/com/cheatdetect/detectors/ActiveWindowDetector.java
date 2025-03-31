package com.cheatdetect.detectors;

import com.cheatdetect.api.EventListener;
import com.cheatdetect.core.Configuration;
import com.cheatdetect.platform.PlatformInterface;
import com.cheatdetect.utils.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * Monitors active window titles to detect coding platforms and other suspicious applications.
 */
public class ActiveWindowDetector {
    private final Configuration config;
    private final PlatformInterface platform;
    private final Set<EventListener> listeners;
    private String lastActiveWindow;

    /**
     * Creates a new ActiveWindowDetector.
     *
     * @param config   the configuration to use
     * @param platform the platform implementation
     */
    public ActiveWindowDetector(Configuration config, PlatformInterface platform) {
        this.config = config;
        this.platform = platform;
        this.listeners = new HashSet<>();
        this.lastActiveWindow = "";
    }

    /**
     * Detects suspicious active windows.
     */
    public void detect() {
        try {
            String currentActiveWindow = platform.getActiveWindowTitle();

            if (currentActiveWindow == null || currentActiveWindow.equals(lastActiveWindow)) {
                return;
            }

            lastActiveWindow = currentActiveWindow;
            String lowerCaseWindow = currentActiveWindow.toLowerCase();

            if (config.isDetailedLoggingEnabled()) {
                Logger.debug("Active window: " + currentActiveWindow);
            }

            // Check for coding platforms
            for (String platformId : config.getPlatformIdentifiers()) {
                if (lowerCaseWindow.contains(platformId)) {
                    notifyListeners("ALERT_CODING_PLATFORM",
                            "Detected coding platform: " + platformId + " in window: " + currentActiveWindow);
                    break;
                }
            }

            // Check for video conferencing apps
            for (String appName : config.getVideoCallApplications()) {
                if (lowerCaseWindow.contains(appName)) {
                    notifyListeners("INFO_VIDEO_APP",
                            "Detected video app: " + appName + " in window: " + currentActiveWindow);
                    break;
                }
            }

            // Check for suspicious applications by title
            for (String process : config.getSuspiciousProcesses()) {
                if (lowerCaseWindow.contains(process)) {
                    notifyListeners("ALERT_SUSPICIOUS_WINDOW",
                            "Detected suspicious window: " + process + " in window: " + currentActiveWindow);
                    break;
                }
            }

        } catch (Exception e) {
            Logger.error("Error detecting active window", e);
        }
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