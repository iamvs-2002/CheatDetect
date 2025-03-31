package com.cheatdetect.api;

import com.cheatdetect.core.CheatDetect;
import com.cheatdetect.core.Configuration;
import com.cheatdetect.utils.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Public API for the CheatDetect library.
 * Provides a simplified interface for client applications.
 */
public class CheatDetectAPI {

    private static final Map<String, CheatDetect> INSTANCES = new ConcurrentHashMap<>();

    /**
     * Creates a new CheatDetect instance with the specified configuration.
     *
     * @param config the configuration to use
     * @return the ID of the created instance
     */
    public static String createInstance(Configuration config) {
        String instanceId = UUID.randomUUID().toString();
        CheatDetect instance = new CheatDetect(config);
        INSTANCES.put(instanceId, instance);

        Logger.info("Created CheatDetect instance with ID: " + instanceId);
        return instanceId;
    }

    /**
     * Starts monitoring with the specified CheatDetect instance.
     *
     * @param instanceId the ID of the instance to use
     * @return true if successful, false otherwise
     */
    public static boolean startMonitoring(String instanceId) {
        CheatDetect instance = INSTANCES.get(instanceId);
        if (instance != null) {
            instance.start();
            Logger.info("Started monitoring for instance: " + instanceId);
            return true;
        }

        Logger.error("Invalid instance ID: " + instanceId);
        return false;
    }

    /**
     * Stops monitoring with the specified CheatDetect instance.
     *
     * @param instanceId the ID of the instance to use
     * @return true if successful, false otherwise
     */
    public static boolean stopMonitoring(String instanceId) {
        CheatDetect instance = INSTANCES.get(instanceId);
        if (instance != null) {
            instance.stop();
            Logger.info("Stopped monitoring for instance: " + instanceId);
            return true;
        }

        Logger.error("Invalid instance ID: " + instanceId);
        return false;
    }

    /**
     * Registers an alert callback with the specified CheatDetect instance.
     *
     * @param instanceId the ID of the instance to use
     * @param callback   the callback to register
     * @return true if successful, false otherwise
     */
    public static boolean registerAlertCallback(String instanceId, AlertCallback callback) {
        CheatDetect instance = INSTANCES.get(instanceId);
        if (instance != null) {
            instance.registerAlertCallback(callback);
            return true;
        }

        Logger.error("Invalid instance ID: " + instanceId);
        return false;
    }

    /**
     * Unregisters an alert callback from the specified CheatDetect instance.
     *
     * @param instanceId the ID of the instance to use
     * @param callback   the callback to unregister
     * @return true if successful, false otherwise
     */
    public static boolean unregisterAlertCallback(String instanceId, AlertCallback callback) {
        CheatDetect instance = INSTANCES.get(instanceId);
        if (instance != null) {
            instance.unregisterAlertCallback(callback);
            return true;
        }

        Logger.error("Invalid instance ID: " + instanceId);
        return false;
    }

    /**
     * Releases the specified CheatDetect instance.
     *
     * @param instanceId the ID of the instance to release
     * @return true if successful, false otherwise
     */
    public static boolean releaseInstance(String instanceId) {
        CheatDetect instance = INSTANCES.remove(instanceId);
        if (instance != null) {
            instance.shutdown();
            Logger.info("Released CheatDetect instance: " + instanceId);
            return true;
        }

        Logger.error("Invalid instance ID: " + instanceId);
        return false;
    }

    /**
     * Checks if the specified CheatDetect instance is running.
     *
     * @param instanceId the ID of the instance to check
     * @return true if running, false otherwise
     */
    public static boolean isMonitoring(String instanceId) {
        CheatDetect instance = INSTANCES.get(instanceId);
        if (instance != null) {
            return instance.isRunning();
        }

        return false;
    }

    /**
     * Gets the configuration for the specified CheatDetect instance.
     *
     * @param instanceId the ID of the instance to use
     * @return the configuration, or null if the instance doesn't exist
     */
    public static Configuration getInstanceConfiguration(String instanceId) {
        CheatDetect instance = INSTANCES.get(instanceId);
        if (instance != null) {
            return instance.getConfiguration();
        }

        return null;
    }

    /**
     * Gets all active CheatDetect instances.
     *
     * @return a map of instance IDs to their running status
     */
    public static Map<String, Boolean> getAllInstances() {
        Map<String, Boolean> result = new HashMap<>();

        for (Map.Entry<String, CheatDetect> entry : INSTANCES.entrySet()) {
            result.put(entry.getKey(), entry.getValue().isRunning());
        }

        return result;
    }

    /**
     * Sets whether detailed logging is enabled for all instances.
     *
     * @param enabled true to enable detailed logging, false otherwise
     */
    public static void setDetailedLoggingEnabled(boolean enabled) {
        Logger.setDetailedLoggingEnabled(enabled);
        Logger.info("Detailed logging " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * Releases all CheatDetect instances.
     * This should be called when the application is shutting down.
     */
    public static void releaseAllInstances() {
        for (String instanceId : INSTANCES.keySet()) {
            releaseInstance(instanceId);
        }

        INSTANCES.clear();
        Logger.info("Released all CheatDetect instances");
    }
}