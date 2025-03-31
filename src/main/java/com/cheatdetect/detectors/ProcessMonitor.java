package com.cheatdetect.detectors;

import com.cheatdetect.api.EventListener;
import com.cheatdetect.core.Configuration;
import com.cheatdetect.platform.PlatformInterface;
import com.cheatdetect.utils.Logger;
import com.cheatdetect.utils.ProcessUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Monitors running processes to detect unauthorized tools or applications.
 */
public class ProcessMonitor {
    private final Configuration config;
    private final PlatformInterface platform;
    private final Set<EventListener> listeners;
    private final Set<String> knownProcesses;

    /**
     * Creates a new ProcessMonitor.
     *
     * @param config   the configuration to use
     * @param platform the platform implementation
     */
    public ProcessMonitor(Configuration config, PlatformInterface platform) {
        this.config = config;
        this.platform = platform;
        this.listeners = new HashSet<>();
        this.knownProcesses = new HashSet<>();
    }

    /**
     * Detects suspicious processes.
     */
    public void detect() {
        try {
            List<String> runningProcesses = platform.getRunningProcesses();

            if (config.isDetailedLoggingEnabled()) {
                Logger.debug("Monitoring " + runningProcesses.size() + " processes");
            }

            // Check for suspicious processes
            for (String process : runningProcesses) {
                String lowerCaseProcess = process.toLowerCase();

                // Check against the list of suspicious processes
                for (String suspiciousProcess : config.getSuspiciousProcesses()) {
                    if (lowerCaseProcess.contains(suspiciousProcess)) {
                        // If this is a new process, alert
                        if (!knownProcesses.contains(lowerCaseProcess)) {
                            knownProcesses.add(lowerCaseProcess);
                            notifyListeners("ALERT_SUSPICIOUS_PROCESS",
                                    "Detected suspicious process: " + process);
                        }
                        break;
                    }
                }

                // Check for virtualization software
                if (ProcessUtils.isVirtualizationProcess(lowerCaseProcess) &&
                        !knownProcesses.contains(lowerCaseProcess)) {
                    knownProcesses.add(lowerCaseProcess);
                    notifyListeners("ALERT_VIRTUALIZATION",
                            "Detected virtualization software: " + process);
                }

                // Check for remote access tools
                if (ProcessUtils.isRemoteAccessProcess(lowerCaseProcess) &&
                        !knownProcesses.contains(lowerCaseProcess)) {
                    knownProcesses.add(lowerCaseProcess);
                    notifyListeners("ALERT_REMOTE_ACCESS",
                            "Detected remote access software: " + process);
                }

                // Check for video call applications
                for (String videoApp : config.getVideoCallApplications()) {
                    if (lowerCaseProcess.contains(videoApp) &&
                            !knownProcesses.contains(lowerCaseProcess)) {
                        knownProcesses.add(lowerCaseProcess);
                        notifyListeners("INFO_VIDEO_PROCESS",
                                "Detected video application: " + process);
                        break;
                    }
                }
            }

            // Check for newly terminated suspicious processes
            Set<String> terminatedProcesses = new HashSet<>(knownProcesses);
            terminatedProcesses.removeAll(runningProcesses);

            for (String terminated : terminatedProcesses) {
                knownProcesses.remove(terminated);
                notifyListeners("INFO_PROCESS_TERMINATED",
                        "Previously detected process terminated: " + terminated);
            }

        } catch (Exception e) {
            Logger.error("Error detecting processes", e);
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