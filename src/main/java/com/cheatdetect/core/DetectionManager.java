package com.cheatdetect.core;

import com.cheatdetect.api.EventListener;
import com.cheatdetect.detectors.*;
import com.cheatdetect.platform.PlatformDetector;
import com.cheatdetect.platform.PlatformFactory;
import com.cheatdetect.platform.PlatformInterface;
import com.cheatdetect.utils.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages the initialization and execution of all detection modules.
 * Acts as a central coordinator for all detection activities.
 */
public class DetectionManager {
    private final Configuration config;
    private final List<EventListener> eventListeners;
    private final ScheduledExecutorService scheduledExecutor;
    private PlatformInterface platformImpl;

    // Detectors
    private ActiveWindowDetector activeWindowDetector;
    private ClipboardMonitor clipboardMonitor;
    private BrowserMonitor browserMonitor;
    private DeviceSwitchDetector deviceSwitchDetector;
    private ProcessMonitor processMonitor;
    private ScreenShareDetector screenShareDetector;
    private SystemTrayMonitor systemTrayMonitor;
    private VideoCallDetector videoCallDetector;

    /**
     * Creates a new DetectionManager with the provided configuration.
     *
     * @param config the configuration to use
     */
    public DetectionManager(Configuration config) {
        this.config = config;
        this.eventListeners = new ArrayList<>();
        this.scheduledExecutor = Executors.newScheduledThreadPool(3);
    }

    /**
     * Initializes all detection modules based on the configuration.
     */
    public void initializeDetectors() {
        Logger.info("Initializing platform implementation...");
        this.platformImpl = PlatformFactory.getPlatformImplementation();
        if (platformImpl == null) {
            Logger.error("Failed to initialize platform implementation");
            throw new RuntimeException("Unsupported platform");
        }

        Logger.info("Detected platform: " + PlatformDetector.getPlatformName());

        // Initialize detectors
        if (config.isLeetCodeDetectionEnabled()) {
            activeWindowDetector = new ActiveWindowDetector(config, platformImpl);
            activeWindowDetector.registerEventListener(this::forwardEvent);
            Logger.info("Active window detector initialized");
        }

        if (config.isBrowserMonitoringEnabled()) {
            browserMonitor = new BrowserMonitor(config, platformImpl);
            browserMonitor.registerEventListener(this::forwardEvent);
            Logger.info("Browser monitor initialized");
        }

        if (config.isClipboardMonitoringEnabled()) {
            clipboardMonitor = new ClipboardMonitor(config, platformImpl);
            clipboardMonitor.registerEventListener(this::forwardEvent);
            Logger.info("Clipboard monitor initialized");
        }

        if (config.isDeviceSwitchDetectionEnabled()) {
            deviceSwitchDetector = new DeviceSwitchDetector(config, platformImpl);
            deviceSwitchDetector.registerEventListener(this::forwardEvent);
            Logger.info("Device switch detector initialized");
        }

        if (config.isProcessMonitoringEnabled()) {
            processMonitor = new ProcessMonitor(config, platformImpl);
            processMonitor.registerEventListener(this::forwardEvent);
            Logger.info("Process monitor initialized");
        }

        if (config.isScreenShareDetectionEnabled()) {
            screenShareDetector = new ScreenShareDetector(config, platformImpl);
            screenShareDetector.registerEventListener(this::forwardEvent);
            Logger.info("Screen share detector initialized");
        }

        if (config.isSystemTrayMonitoringEnabled()) {
            systemTrayMonitor = new SystemTrayMonitor(config, platformImpl);
            systemTrayMonitor.registerEventListener(this::forwardEvent);
            Logger.info("System tray monitor initialized");
        }

        if (config.isVideoCallDetectionEnabled()) {
            videoCallDetector = new VideoCallDetector(config, platformImpl);
            videoCallDetector.registerEventListener(this::forwardEvent);
            Logger.info("Video call detector initialized");
        }
    }

    /**
     * Starts all initialized detection modules.
     */
    public void startDetection() {
        Logger.info("Starting detection modules...");

        if (activeWindowDetector != null) {
            scheduledExecutor.scheduleAtFixedRate(
                    activeWindowDetector::detect,
                    0,
                    config.getScanIntervalMs(),
                    TimeUnit.MILLISECONDS
            );
        }

        if (clipboardMonitor != null) {
            clipboardMonitor.startMonitoring();
        }

        if (browserMonitor != null) {
            browserMonitor.startMonitoring();
        }

        if (deviceSwitchDetector != null) {
            scheduledExecutor.scheduleAtFixedRate(
                    deviceSwitchDetector::detect,
                    0,
                    config.getScanIntervalMs() * 2, // Less frequent check
                    TimeUnit.MILLISECONDS
            );
        }

        if (processMonitor != null) {
            scheduledExecutor.scheduleAtFixedRate(
                    processMonitor::detect,
                    0,
                    config.getScanIntervalMs(),
                    TimeUnit.MILLISECONDS
            );
        }

        if (screenShareDetector != null) {
            scheduledExecutor.scheduleAtFixedRate(
                    screenShareDetector::detect,
                    0,
                    config.getScanIntervalMs(),
                    TimeUnit.MILLISECONDS
            );
        }

        if (systemTrayMonitor != null) {
            scheduledExecutor.scheduleAtFixedRate(
                    systemTrayMonitor::detect,
                    0,
                    config.getScanIntervalMs(),
                    TimeUnit.MILLISECONDS
            );
        }

        if (videoCallDetector != null) {
            scheduledExecutor.scheduleAtFixedRate(
                    videoCallDetector::detect,
                    0,
                    config.getScanIntervalMs(),
                    TimeUnit.MILLISECONDS
            );
        }

        Logger.info("All enabled detection modules started");
    }

    /**
     * Stops all running detection modules.
     */
    public void stopDetection() {
        Logger.info("Stopping detection modules...");

        if (clipboardMonitor != null) {
            clipboardMonitor.stopMonitoring();
        }

        if (browserMonitor != null) {
            browserMonitor.stopMonitoring();
        }

        scheduledExecutor.shutdown();
        try {
            if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduledExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        Logger.info("All detection modules stopped");
    }

    /**
     * Registers an event listener to receive detection events.
     *
     * @param listener the listener to register
     */
    public void registerEventListener(EventListener listener) {
        if (listener != null) {
            eventListeners.add(listener);
        }
    }

    /**
     * Unregisters a previously registered event listener.
     *
     * @param listener the listener to unregister
     */
    public void unregisterEventListener(EventListener listener) {
        eventListeners.remove(listener);
    }

    /**
     * Forwards events from detectors to all registered listeners.
     *
     * @param eventType the type of the event
     * @param details   details about the event
     */
    private void forwardEvent(String eventType, String details) {
        for (EventListener listener : eventListeners) {
            try {
                listener.onEvent(eventType, details);
            } catch (Exception e) {
                Logger.error("Error in event listener", e);
            }
        }
    }
}