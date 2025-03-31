package com.cheatdetect.detectors;

import com.cheatdetect.api.EventListener;
import com.cheatdetect.core.Configuration;
import com.cheatdetect.platform.PlatformInterface;
import com.cheatdetect.utils.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Detects if the user is participating in multiple video calls simultaneously.
 */
public class VideoCallDetector {
    private final Configuration config;
    private final PlatformInterface platform;
    private final Set<EventListener> listeners;
    private final Set<String> activeVideoApps;
    private String primaryVideoApp;

    /**
     * Creates a new VideoCallDetector.
     *
     * @param config   the configuration to use
     * @param platform the platform implementation
     */
    public VideoCallDetector(Configuration config, PlatformInterface platform) {
        this.config = config;
        this.platform = platform;
        this.listeners = new HashSet<>();
        this.activeVideoApps = new HashSet<>();
        this.primaryVideoApp = null;
    }

    /**
     * Detects suspicious video call activities.
     */
    public void detect() {
        try {
            // Get running processes and active camera/microphone usage
            List<String> runningProcesses = platform.getRunningProcesses();
            boolean isCameraActive = platform.isCameraActive();
            boolean isMicrophoneActive = platform.isMicrophoneActive();

            if (config.isDetailedLoggingEnabled()) {
                Logger.debug("Camera active: " + isCameraActive + ", Microphone active: " + isMicrophoneActive);
            }

            // Identify video call applications from running processes
            Set<String> currentVideoApps = new HashSet<>();
            for (String process : runningProcesses) {
                String lowerCaseProcess = process.toLowerCase();
                for (String videoApp : config.getVideoCallApplications()) {
                    if (lowerCaseProcess.contains(videoApp)) {
                        currentVideoApps.add(lowerCaseProcess);
                        break;
                    }
                }
            }

            // Check for camera/microphone use without known video app
            if ((isCameraActive || isMicrophoneActive) && currentVideoApps.isEmpty()) {
                notifyListeners("ALERT_UNKNOWN_MEDIA_USE",
                        "Camera or microphone active without recognized video application");
            }

            // Set the primary video app if not already set
            if (primaryVideoApp == null && !currentVideoApps.isEmpty()) {
                primaryVideoApp = currentVideoApps.iterator().next();
                notifyListeners("INFO_PRIMARY_VIDEO_APP",
                        "Primary video application detected: " + primaryVideoApp);
            }

            // Check for multiple video apps (potential parallel calls)
            if (currentVideoApps.size() > 1) {
                notifyListeners("ALERT_MULTIPLE_VIDEO_APPS",
                        "Multiple video applications detected: " + String.join(", ", currentVideoApps));
            }

            // Check for new video apps
            for (String videoApp : currentVideoApps) {
                if (!activeVideoApps.contains(videoApp)) {
                    activeVideoApps.add(videoApp);

                    // If we already have a primary app and this is different, alert
                    if (primaryVideoApp != null && !videoApp.equals(primaryVideoApp)) {
                        notifyListeners("ALERT_ADDITIONAL_VIDEO_APP",
                                "Additional video application detected: " + videoApp);
                    } else {
                        notifyListeners("INFO_VIDEO_APP_STARTED",
                                "Video application started: " + videoApp);
                    }
                }
            }

            // Check for closed video apps
            Set<String> closedApps = new HashSet<>(activeVideoApps);
            closedApps.removeAll(currentVideoApps);

            for (String closedApp : closedApps) {
                activeVideoApps.remove(closedApp);
                notifyListeners("INFO_VIDEO_APP_CLOSED",
                        "Video application closed: " + closedApp);

                // If the primary app was closed, clear it
                if (closedApp.equals(primaryVideoApp)) {
                    primaryVideoApp = null;

                    // Set a new primary app if there's still one running
                    if (!currentVideoApps.isEmpty()) {
                        primaryVideoApp = currentVideoApps.iterator().next();
                        notifyListeners("INFO_PRIMARY_VIDEO_APP",
                                "New primary video application: " + primaryVideoApp);
                    }
                }
            }

        } catch (Exception e) {
            Logger.error("Error detecting video calls", e);
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