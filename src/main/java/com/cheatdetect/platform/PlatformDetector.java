package com.cheatdetect.platform;

/**
 * Utility class to detect the operating system platform.
 */
public class PlatformDetector {
    public enum Platform {
        WINDOWS, MAC_OS, LINUX, UNKNOWN
    }

    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();

    /**
     * Determines the platform.
     *
     * @return the detected platform
     */
    public static Platform getPlatform() {
        if (isWindows()) {
            return Platform.WINDOWS;
        } else if (isMacOS()) {
            return Platform.MAC_OS;
        } else if (isLinux()) {
            return Platform.LINUX;
        } else {
            return Platform.UNKNOWN;
        }
    }

    /**
     * Gets the platform name as a string.
     *
     * @return the platform name
     */
    public static String getPlatformName() {
        switch (getPlatform()) {
            case WINDOWS:
                return "Windows";
            case MAC_OS:
                return "macOS";
            case LINUX:
                return "Linux";
            default:
                return "Unknown";
        }
    }

    /**
     * Checks if the platform is Windows.
     *
     * @return true if Windows, false otherwise
     */
    public static boolean isWindows() {
        return OS_NAME.contains("win");
    }

    /**
     * Checks if the platform is macOS.
     *
     * @return true if macOS, false otherwise
     */
    public static boolean isMacOS() {
        return OS_NAME.contains("mac");
    }

    /**
     * Checks if the platform is Linux.
     *
     * @return true if Linux, false otherwise
     */
    public static boolean isLinux() {
        return OS_NAME.contains("nix") ||
                OS_NAME.contains("nux") ||
                OS_NAME.contains("aix");
    }

    /**
     * Checks if the platform is supported.
     *
     * @return true if supported, false otherwise
     */
    public static boolean isSupported() {
        return getPlatform() != Platform.UNKNOWN;
    }
}