package com.cheatdetect.detectors;

import com.cheatdetect.api.EventListener;
import com.cheatdetect.core.Configuration;
import com.cheatdetect.platform.PlatformInterface;
import com.cheatdetect.utils.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Monitors the system tray for hidden applications that could be used for cheating.
 */
public class SystemTrayMonitor {
    private final Configuration config;
    private final PlatformInterface platform;
    private final Set<EventListener> listeners;
    private final Set<String> knownTrayApps;

    /**
     * Creates a new SystemTrayMonitor.
     *
     * @param config   the configuration to use
     * @param platform the platform implementation
     */
    public SystemTrayMonitor(Configuration config, PlatformInterface platform) {
        this.config = config;
        this.platform = platform;
        this.listeners = new HashSet<>();
        this.knownTrayApps = new HashSet<>();
    }

    /**
     * Detects suspicious applications in the system tray.
     */
    public void detect() {
        try {
            List<String> trayApplications = platform.getSystemTrayApplications();

            if (config.isDetailedLoggingEnabled()) {
                Logger.debug("System tray applications: " + String.join(", ", trayApplications));
            }

            // Check for suspicious tray applications
            for (String trayApp : trayApplications) {
                String lowerCaseTrayApp = trayApp.toLowerCase();

                // Check against the list of suspicious processes
                for (String suspiciousProcess : config.getSuspiciousProcesses()) {
                    if (lowerCaseTrayApp.contains(suspiciousProcess)) {
                        // If this is a new tray app, alert
                        if (!knownTrayApps.contains(lowerCaseTrayApp)) {
                            knownTrayApps.add(lowerCaseTrayApp);
                            notifyListeners("ALERT_SUSPICIOUS_TRAY_APP",
                                    "Detected suspicious application in system tray: " + trayApp);
                        }
                        break;
                    }
                }

                // Track all tray apps for changes
                if (!knownTrayApps.contains(lowerCaseTrayApp)) {
                    knownTrayApps.add(lowerCaseTrayApp);
                    notifyListeners("INFO_NEW_TRAY_APP",
                            "New application detected in system tray: " + trayApp);
                }
            }

            // Check for removed tray apps
            Set<String> removedTrayApps = new HashSet<>(knownTrayApps);
            for (String trayApp : trayApplications) {
                removedTrayApps.remove(trayApp.toLowerCase());
            }

            for (String removedApp : removedTrayApps) {
                knownTrayApps.remove(removedApp);
                notifyListeners("INFO_TRAY_APP_REMOVED",
                        "Application removed from system tray: " + removedApp);
            }

        } catch (Exception e) {
            Logger.error("Error detecting system tray applications", e);
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