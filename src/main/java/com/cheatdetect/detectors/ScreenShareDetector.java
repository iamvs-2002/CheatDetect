package com.cheatdetect.detectors;

import com.cheatdetect.api.EventListener;
import com.cheatdetect.core.Configuration;
import com.cheatdetect.platform.PlatformInterface;
import com.cheatdetect.utils.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * Detects if the user is sharing their screen with an external application.
 */
public class ScreenShareDetector {
    private final Configuration config;
    private final PlatformInterface platform;
    private final Set<EventListener> listeners;
    private boolean wasScreenSharing;

    /**
     * Creates a new ScreenShareDetector.
     *
     * @param config   the configuration to use
     * @param platform the platform implementation
     */
    public ScreenShareDetector(Configuration config, PlatformInterface platform) {
        this.config = config;
        this.platform = platform;
        this.listeners = new HashSet<>();
        this.wasScreenSharing = false;
    }

    /**
     * Detects screen sharing activities.
     */
    public void detect() {
        try {
            boolean isScreenSharing = platform.isScreenSharing();

            if (config.isDetailedLoggingEnabled()) {
                Logger.debug("Screen sharing status: " + isScreenSharing);
            }

            // Detect changes in screen sharing status
            if (isScreenSharing && !wasScreenSharing) {
                notifyListeners("ALERT_SCREEN_SHARING_STARTED",
                        "Screen sharing detected");
                wasScreenSharing = true;
            } else if (!isScreenSharing && wasScreenSharing) {
                notifyListeners("INFO_SCREEN_SHARING_STOPPED",
                        "Screen sharing stopped");
                wasScreenSharing = false;
            }

            // Check for specific screen sharing applications if still ongoing
            if (isScreenSharing) {
                // Get additional details about what application is being used for sharing
                String sharingApp = platform.getScreenSharingApplication();
                if (sharingApp != null && !sharingApp.isEmpty()) {
                    notifyListeners("INFO_SCREEN_SHARING_APP",
                            "Screen sharing application: " + sharingApp);
                }

                // Check if sharing is related to the interview platform or outside
                boolean isLegitimateSharing = isLegitimateScreenSharing();
                if (!isLegitimateSharing) {
                    notifyListeners("ALERT_UNAUTHORIZED_SHARING",
                            "Screen is being shared with an unauthorized application");
                }
            }

        } catch (Exception e) {
            Logger.error("Error detecting screen sharing", e);
        }
    }

    /**
     * Determines if the current screen sharing is legitimate.
     * For example, sharing with the interview platform is legitimate.
     *
     * @return true if legitimate, false otherwise
     */
    private boolean isLegitimateScreenSharing() {
        try {
            String sharingApp = platform.getScreenSharingApplication();
            if (sharingApp == null || sharingApp.isEmpty()) {
                return false;
            }

            // Check against legitimate interview applications
            String lowerCaseSharingApp = sharingApp.toLowerCase();
            for (String videoApp : config.getVideoCallApplications()) {
                if (lowerCaseSharingApp.contains(videoApp)) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            Logger.error("Error checking legitimacy of screen sharing", e);
            return false;
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