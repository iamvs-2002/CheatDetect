package com.cheatdetect.platform;

import com.cheatdetect.utils.Logger;

/**
 * Factory for creating the appropriate platform-specific implementation.
 */
public class PlatformFactory {
    private static PlatformInterface instance;

    /**
     * Gets the platform implementation for the current operating system.
     *
     * @return the platform implementation
     */
    public static synchronized PlatformInterface getPlatformImplementation() {
        if (instance == null) {
            PlatformDetector.Platform platform = PlatformDetector.getPlatform();

            try {
                switch (platform) {
                    case WINDOWS:
                        instance = new WindowsPlatformImpl();
                        break;
                    case MAC_OS:
                        instance = new MacOSPlatformImpl();
                        break;
                    case LINUX:
                        instance = new LinuxPlatformImpl();
                        break;
                    default:
                        Logger.error("Unsupported platform: " + PlatformDetector.getPlatformName());
                        throw new UnsupportedOperationException("Unsupported platform");
                }

                Logger.info("Created platform implementation for " + PlatformDetector.getPlatformName());
            } catch (Exception e) {
                Logger.error("Failed to create platform implementation", e);
                throw new RuntimeException("Failed to initialize platform implementation", e);
            }
        }

        return instance;
    }

    /**
     * Resets the platform implementation.
     * Primarily used for testing.
     */
    public static synchronized void reset() {
        instance = null;
    }
}