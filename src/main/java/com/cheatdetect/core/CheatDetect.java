package com.cheatdetect.core;

import com.cheatdetect.api.AlertCallback;
import com.cheatdetect.api.EventListener;
import com.cheatdetect.utils.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main entry point for the CheatDetect library.
 * Manages detection modules and provides a simple interface for clients.
 */
public class CheatDetect {
    private final Configuration config;
    private final DetectionManager detectionManager;
    private final List<AlertCallback> alertCallbacks;
    private final ExecutorService executorService;
    private boolean isRunning;

    /**
     * Creates a new instance of CheatDetect with the provided configuration.
     *
     * @param config the configuration to use
     */
    public CheatDetect(Configuration config) {
        this.config = config;
        this.detectionManager = new DetectionManager(config);
        this.alertCallbacks = new ArrayList<>();
        this.executorService = Executors.newCachedThreadPool();
        this.isRunning = false;

        // Register internal event listener to forward alerts
        this.detectionManager.registerEventListener(new EventListener() {
            @Override
            public void onEvent(String eventType, String details) {
                if (eventType.startsWith("ALERT_")) {
                    notifyAlertCallbacks(eventType.substring(6), details);
                }
                Logger.info("Event detected: " + eventType + " - " + details);
            }
        });
    }

    /**
     * Starts all detection modules based on the configuration.
     */
    public synchronized void start() {
        if (isRunning) {
            Logger.warn("CheatDetect is already running");
            return;
        }

        Logger.info("Starting CheatDetect...");
        executorService.submit(() -> {
            try {
                detectionManager.initializeDetectors();
                detectionManager.startDetection();
                isRunning = true;
                Logger.info("CheatDetect started successfully");
            } catch (Exception e) {
                Logger.error("Failed to start CheatDetect", e);
                stop();
            }
        });
    }

    /**
     * Stops all detection modules.
     */
    public synchronized void stop() {
        if (!isRunning) {
            Logger.warn("CheatDetect is not running");
            return;
        }

        Logger.info("Stopping CheatDetect...");
        try {
            detectionManager.stopDetection();
            isRunning = false;
            Logger.info("CheatDetect stopped successfully");
        } catch (Exception e) {
            Logger.error("Error stopping CheatDetect", e);
        }
    }

    /**
     * Registers a callback to be notified when alerts are detected.
     *
     * @param callback the callback to register
     */
    public void registerAlertCallback(AlertCallback callback) {
        if (callback != null) {
            alertCallbacks.add(callback);
            Logger.info("Alert callback registered");
        }
    }

    /**
     * Unregisters a previously registered alert callback.
     *
     * @param callback the callback to unregister
     */
    public void unregisterAlertCallback(AlertCallback callback) {
        if (callback != null && alertCallbacks.remove(callback)) {
            Logger.info("Alert callback unregistered");
        }
    }

    /**
     * Returns the current configuration.
     *
     * @return the current configuration
     */
    public Configuration getConfiguration() {
        return config;
    }

    /**
     * Checks if CheatDetect is currently running.
     *
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Notifies all registered alert callbacks.
     *
     * @param alertType the type of the alert
     * @param details   details about the alert
     */
    private void notifyAlertCallbacks(String alertType, String details) {
        for (AlertCallback callback : alertCallbacks) {
            try {
                callback.onAlert(alertType, details);
            } catch (Exception e) {
                Logger.error("Error in alert callback", e);
            }
        }
    }

    /**
     * Releases all resources held by CheatDetect.
     * This should be called when CheatDetect is no longer needed.
     */
    public void shutdown() {
        stop();
        executorService.shutdown();
        Logger.info("CheatDetect shutdown complete");
    }
}