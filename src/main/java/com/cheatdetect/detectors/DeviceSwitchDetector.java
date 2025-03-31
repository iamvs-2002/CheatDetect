package com.cheatdetect.detectors;

import com.cheatdetect.api.EventListener;
import com.cheatdetect.core.Configuration;
import com.cheatdetect.platform.PlatformInterface;
import com.cheatdetect.utils.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Detects if the user switches to a different device during the interview.
 * Uses various system identifiers to track device changes.
 */
public class DeviceSwitchDetector {
    private final Configuration config;
    private final PlatformInterface platform;
    private final Set<EventListener> listeners;
    private final String sessionId;
    private long lastActivityTimestamp;
    private int inactivityCount;
    private String lastMacAddress;
    private String lastDeviceId;

    // Constants
    private static final int INACTIVITY_THRESHOLD = 3;
    private static final long INACTIVITY_TIME_MS = TimeUnit.MINUTES.toMillis(2);

    /**
     * Creates a new DeviceSwitchDetector.
     *
     * @param config   the configuration to use
     * @param platform the platform implementation
     */
    public DeviceSwitchDetector(Configuration config, PlatformInterface platform) {
        this.config = config;
        this.platform = platform;
        this.listeners = new HashSet<>();
        this.sessionId = UUID.randomUUID().toString();
        this.lastActivityTimestamp = System.currentTimeMillis();
        this.inactivityCount = 0;

        // Capture initial device identifiers
        try {
            this.lastMacAddress = platform.getPrimaryMacAddress();
            this.lastDeviceId = platform.getDeviceIdentifier();

            Logger.info("Device monitoring initialized with ID: " + lastDeviceId);
        } catch (Exception e) {
            Logger.error("Failed to initialize device identifiers", e);
            this.lastMacAddress = "";
            this.lastDeviceId = "";
        }
    }

    /**
     * Detects potential device switches.
     */
    public void detect() {
        try {
            // Check for device identifier changes
            String currentMacAddress = platform.getPrimaryMacAddress();
            String currentDeviceId = platform.getDeviceIdentifier();

            if (config.isDetailedLoggingEnabled()) {
                Logger.debug("Current device ID: " + currentDeviceId);
            }

            // Check for device changes
            if (!currentDeviceId.isEmpty() && !lastDeviceId.isEmpty() &&
                    !currentDeviceId.equals(lastDeviceId)) {
                notifyListeners("ALERT_DEVICE_CHANGED",
                        "Device identifier changed from " + lastDeviceId + " to " + currentDeviceId);
                lastDeviceId = currentDeviceId;
            }

            // Check for MAC address changes
            if (!currentMacAddress.isEmpty() && !lastMacAddress.isEmpty() &&
                    !currentMacAddress.equals(lastMacAddress)) {
                notifyListeners("ALERT_NETWORK_CHANGED",
                        "Network adapter changed from " + lastMacAddress + " to " + currentMacAddress);
                lastMacAddress = currentMacAddress;
            }

            // Check for extended inactivity followed by activity
            // which may indicate switching to another device and back
            long currentTime = System.currentTimeMillis();
            if (platform.isUserActive()) {
                if (currentTime - lastActivityTimestamp > INACTIVITY_TIME_MS) {
                    inactivityCount++;
                    if (inactivityCount >= INACTIVITY_THRESHOLD) {
                        notifyListeners("ALERT_ACTIVITY_PATTERN",
                                "Suspicious activity pattern detected: long inactivity followed by activity");
                        inactivityCount = 0;
                    }
                }
                lastActivityTimestamp = currentTime;
            }

        } catch (Exception e) {
            Logger.error("Error detecting device switch", e);
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