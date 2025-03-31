package com.cheatdetect.platform;

import java.util.List;

/**
 * Interface for platform-specific operations.
 * Each supported operating system must provide an implementation.
 */
public interface PlatformInterface {

    /**
     * Gets the title of the active window.
     *
     * @return the active window title
     */
    String getActiveWindowTitle();

    /**
     * Gets the list of running processes.
     *
     * @return the list of running processes
     */
    List<String> getRunningProcesses();

    /**
     * Gets the list of applications in the system tray.
     *
     * @return the list of system tray applications
     */
    List<String> getSystemTrayApplications();

    /**
     * Gets the content of the clipboard.
     *
     * @return the clipboard content
     */
    String getClipboardContent();

    /**
     * Checks if the camera is currently active.
     *
     * @return true if active, false otherwise
     */
    boolean isCameraActive();

    /**
     * Checks if the microphone is currently active.
     *
     * @return true if active, false otherwise
     */
    boolean isMicrophoneActive();

    /**
     * Checks if the screen is being shared.
     *
     * @return true if sharing, false otherwise
     */
    boolean isScreenSharing();

    /**
     * Gets the application that is sharing the screen.
     *
     * @return the screen sharing application name
     */
    String getScreenSharingApplication();

    /**
     * Checks if the user is actively using the system.
     *
     * @return true if active, false otherwise
     */
    boolean isUserActive();

    /**
     * Gets the MAC address of the primary network adapter.
     *
     * @return the MAC address
     */
    String getPrimaryMacAddress();

    /**
     * Gets a unique identifier for the device.
     *
     * @return the device identifier
     */
    String getDeviceIdentifier();

    /**
     * Releases any resources held by the platform implementation.
     */
    void cleanup();
}