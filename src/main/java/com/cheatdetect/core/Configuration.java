package com.cheatdetect.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuration class for CheatDetect.
 * Contains settings for all detection modules and general behavior.
 */
public class Configuration {
    private final boolean processMonitoringEnabled;
    private final boolean clipboardMonitoringEnabled;
    private final boolean browserMonitoringEnabled;
    private final boolean leetCodeDetectionEnabled;
    private final boolean screenShareDetectionEnabled;
    private final boolean videoCallDetectionEnabled;
    private final boolean systemTrayMonitoringEnabled;
    private final boolean deviceSwitchDetectionEnabled;
    private final int scanIntervalMs;
    private final boolean detailedLoggingEnabled;
    private final List<String> suspiciousProcesses;
    private final List<String> platformIdentifiers;
    private final List<String> videoCallApplications;

    private Configuration(Builder builder) {
        this.processMonitoringEnabled = builder.processMonitoringEnabled;
        this.clipboardMonitoringEnabled = builder.clipboardMonitoringEnabled;
        this.browserMonitoringEnabled = builder.browserMonitoringEnabled;
        this.leetCodeDetectionEnabled = builder.leetCodeDetectionEnabled;
        this.screenShareDetectionEnabled = builder.screenShareDetectionEnabled;
        this.videoCallDetectionEnabled = builder.videoCallDetectionEnabled;
        this.systemTrayMonitoringEnabled = builder.systemTrayMonitoringEnabled;
        this.deviceSwitchDetectionEnabled = builder.deviceSwitchDetectionEnabled;
        this.scanIntervalMs = builder.scanIntervalMs;
        this.detailedLoggingEnabled = builder.detailedLoggingEnabled;
        this.suspiciousProcesses = Collections.unmodifiableList(new ArrayList<>(builder.suspiciousProcesses));
        this.platformIdentifiers = Collections.unmodifiableList(new ArrayList<>(builder.platformIdentifiers));
        this.videoCallApplications = Collections.unmodifiableList(new ArrayList<>(builder.videoCallApplications));
    }

    public boolean isProcessMonitoringEnabled() {
        return processMonitoringEnabled;
    }

    public boolean isClipboardMonitoringEnabled() {
        return clipboardMonitoringEnabled;
    }

    public boolean isBrowserMonitoringEnabled() {
        return browserMonitoringEnabled;
    }

    public boolean isLeetCodeDetectionEnabled() {
        return leetCodeDetectionEnabled;
    }

    public boolean isScreenShareDetectionEnabled() {
        return screenShareDetectionEnabled;
    }

    public boolean isVideoCallDetectionEnabled() {
        return videoCallDetectionEnabled;
    }

    public boolean isSystemTrayMonitoringEnabled() {
        return systemTrayMonitoringEnabled;
    }

    public boolean isDeviceSwitchDetectionEnabled() {
        return deviceSwitchDetectionEnabled;
    }

    public int getScanIntervalMs() {
        return scanIntervalMs;
    }

    public boolean isDetailedLoggingEnabled() {
        return detailedLoggingEnabled;
    }

    public List<String> getSuspiciousProcesses() {
        return suspiciousProcesses;
    }

    public List<String> getPlatformIdentifiers() {
        return platformIdentifiers;
    }

    public List<String> getVideoCallApplications() {
        return videoCallApplications;
    }

    /**
     * Builder for Configuration.
     */
    public static class Builder {
        private boolean processMonitoringEnabled = true;
        private boolean clipboardMonitoringEnabled = true;
        private boolean browserMonitoringEnabled = true;
        private boolean leetCodeDetectionEnabled = true;
        private boolean screenShareDetectionEnabled = true;
        private boolean videoCallDetectionEnabled = true;
        private boolean systemTrayMonitoringEnabled = true;
        private boolean deviceSwitchDetectionEnabled = true;
        private int scanIntervalMs = 5000; // 5 seconds default
        private boolean detailedLoggingEnabled = false;
        private final List<String> suspiciousProcesses = new ArrayList<>();
        private final List<String> platformIdentifiers = new ArrayList<>();
        private final List<String> videoCallApplications = new ArrayList<>();

        public Builder() {
            // Initialize with default suspicious processes
            suspiciousProcesses.add("chatgpt");
            suspiciousProcesses.add("copilot");
            suspiciousProcesses.add("virtualbox");
            suspiciousProcesses.add("vmware");
            suspiciousProcesses.add("anydesk");
            suspiciousProcesses.add("teamviewer");

            // Initialize with default platform identifiers
            platformIdentifiers.add("leetcode");
            platformIdentifiers.add("hackerrank");
            platformIdentifiers.add("codility");
            platformIdentifiers.add("codesignal");
            platformIdentifiers.add("hackerearth");
            platformIdentifiers.add("topcoder");
            platformIdentifiers.add("codeforces");

            // Initialize with default video call applications
            videoCallApplications.add("zoom");
            videoCallApplications.add("teams");
            videoCallApplications.add("webex");
            videoCallApplications.add("meet");
            videoCallApplications.add("skype");
            videoCallApplications.add("discord");
        }

        public Builder enableProcessMonitoring(boolean enabled) {
            this.processMonitoringEnabled = enabled;
            return this;
        }

        public Builder enableClipboardMonitoring(boolean enabled) {
            this.clipboardMonitoringEnabled = enabled;
            return this;
        }

        public Builder enableBrowserMonitoring(boolean enabled) {
            this.browserMonitoringEnabled = enabled;
            return this;
        }

        public Builder enableLeetCodeDetection(boolean enabled) {
            this.leetCodeDetectionEnabled = enabled;
            return this;
        }

        public Builder enableScreenShareDetection(boolean enabled) {
            this.screenShareDetectionEnabled = enabled;
            return this;
        }

        public Builder enableVideoCallDetection(boolean enabled) {
            this.videoCallDetectionEnabled = enabled;
            return this;
        }

        public Builder enableSystemTrayMonitoring(boolean enabled) {
            this.systemTrayMonitoringEnabled = enabled;
            return this;
        }

        public Builder enableDeviceSwitchDetection(boolean enabled) {
            this.deviceSwitchDetectionEnabled = enabled;
            return this;
        }

        public Builder setScanInterval(int intervalMs) {
            this.scanIntervalMs = intervalMs;
            return this;
        }

        public Builder enableDetailedLogging(boolean enabled) {
            this.detailedLoggingEnabled = enabled;
            return this;
        }

        public Builder addSuspiciousProcess(String processName) {
            this.suspiciousProcesses.add(processName.toLowerCase());
            return this;
        }

        public Builder setSuspiciousProcesses(List<String> processes) {
            this.suspiciousProcesses.clear();
            for (String process : processes) {
                this.suspiciousProcesses.add(process.toLowerCase());
            }
            return this;
        }

        public Builder addPlatformIdentifier(String identifier) {
            this.platformIdentifiers.add(identifier.toLowerCase());
            return this;
        }

        public Builder setPlatformIdentifiers(List<String> identifiers) {
            this.platformIdentifiers.clear();
            for (String identifier : identifiers) {
                this.platformIdentifiers.add(identifier.toLowerCase());
            }
            return this;
        }

        public Builder addVideoCallApplication(String application) {
            this.videoCallApplications.add(application.toLowerCase());
            return this;
        }

        public Builder setVideoCallApplications(List<String> applications) {
            this.videoCallApplications.clear();
            for (String application : applications) {
                this.videoCallApplications.add(application.toLowerCase());
            }
            return this;
        }

        public Configuration build() {
            return new Configuration(this);
        }
    }
}