package com.cheatdetect.platform;

import com.cheatdetect.utils.Logger;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * macOS-specific implementation of the platform interface.
 */
public class MacOSPlatformImpl implements PlatformInterface {

    private long lastUserActivityTime = System.currentTimeMillis();

    public MacOSPlatformImpl() {
        Logger.info("Initializing macOS platform implementation");

        // Initialize any macOS-specific resources
        try {
            // Set up activity monitoring
            initActivityMonitoring();
        } catch (Exception e) {
            Logger.error("Failed to initialize macOS platform implementation", e);
        }
    }

    private void initActivityMonitoring() {
        // On macOS we would ideally use a native activity monitor
        // For now, we'll rely on Java's limited capabilities
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            lastUserActivityTime = System.currentTimeMillis();
        }, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
    }

    @Override
    public String getActiveWindowTitle() {
        try {
            // Use AppleScript to get the active window title
            String[] cmd = {
                    "osascript",
                    "-e",
                    "tell application \"System Events\"\n" +
                            "    set frontApp to name of first application process whose frontmost is true\n" +
                            "    tell process frontApp\n" +
                            "        try\n" +
                            "            set windowTitle to name of front window\n" +
                            "        on error\n" +
                            "            set windowTitle to \"\"\n" +
                            "        end try\n" +
                            "    end tell\n" +
                            "    return {frontApp, windowTitle}\n" +
                            "end tell"
            };

            Process process = Runtime.getRuntime().exec(cmd);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String result = reader.readLine();
            reader.close();

            if (result != null && !result.isEmpty()) {
                // Parse result - it may contain both app name and window title
                String[] parts = result.split(", ");
                if (parts.length > 1) {
                    return parts[0] + " - " + parts[1];
                } else {
                    return result;
                }
            }

            return "";
        } catch (Exception e) {
            Logger.error("Failed to get active window title", e);
            return "";
        }
    }

    @Override
    public List<String> getRunningProcesses() {
        List<String> processes = new ArrayList<>();

        try {
            Process process = Runtime.getRuntime().exec("ps -e -o comm");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            // Skip the header line
            reader.readLine();

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    processes.add(line);
                }
            }

            reader.close();
        } catch (Exception e) {
            Logger.error("Failed to get running processes", e);
        }

        return processes;
    }

    @Override
    public List<String> getSystemTrayApplications() {
        List<String> trayApps = new ArrayList<>();

        try {
            // macOS doesn't have a traditional system tray, but we can check menu bar apps
            String[] cmd = {
                    "osascript",
                    "-e",
                    "tell application \"System Events\"\n" +
                            "    set menuBarItems to menu bar items of menu bar 1\n" +
                            "    set appNames to {}\n" +
                            "    repeat with menuItem in menuBarItems\n" +
                            "        try\n" +
                            "            set end of appNames to name of menuItem\n" +
                            "        on error\n" +
                            "            -- ignore errors\n" +
                            "        end try\n" +
                            "    end repeat\n" +
                            "    return appNames\n" +
                            "end tell"
            };

            Process process = Runtime.getRuntime().exec(cmd);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String result = reader.readLine();
            reader.close();

            if (result != null && !result.isEmpty()) {
                // Parse the comma-separated list
                String[] items = result.split(", ");
                for (String item : items) {
                    if (!item.isEmpty()) {
                        trayApps.add(item.trim());
                    }
                }
            }
        } catch (Exception e) {
            Logger.error("Failed to get menu bar applications", e);
        }

        return trayApps;
    }

    @Override
    public String getClipboardContent() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                return (String) clipboard.getData(DataFlavor.stringFlavor);
            }
        } catch (Exception e) {
            // Ignore exceptions during clipboard access as they are common
            if (Logger.isDetailedLoggingEnabled()) {
                Logger.debug("Could not access clipboard: " + e.getMessage());
            }
        }

        return "";
    }

    @Override
    public boolean isCameraActive() {
        try {
            // Check for camera processes on macOS
            // Note: This is a heuristic approach
            Process process = Runtime.getRuntime().exec("lsof | grep -i 'AppleCamera\\|iSight\\|VDC'");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line = reader.readLine();
            reader.close();

            return line != null && !line.isEmpty();
        } catch (Exception e) {
            Logger.error("Failed to check camera status", e);
            return false;
        }
    }

    @Override
    public boolean isMicrophoneActive() {
        try {
            // Check for microphone usage on macOS
            Process process = Runtime.getRuntime().exec("lsof | grep -i 'CoreAudio\\|coreaudiod'");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line = reader.readLine();
            reader.close();

            // Additional check with another command to improve accuracy
            Process process2 = Runtime.getRuntime().exec("ps -ef | grep -i 'audiod\\|audio'");
            BufferedReader reader2 = new BufferedReader(new InputStreamReader(process2.getInputStream()));
            String line2 = reader2.readLine();
            reader2.close();

            return (line != null && !line.isEmpty()) || (line2 != null && !line2.isEmpty());
        } catch (Exception e) {
            Logger.error("Failed to check microphone status", e);
            return false;
        }
    }

    @Override
    public boolean isScreenSharing() {
        try {
            // Check for screen sharing processes on macOS
            Process process = Runtime.getRuntime().exec("ps -ef | grep -i 'screensharing\\|screencast\\|zoom\\|webex\\|teams'");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                // Skip grep process itself
                if (!line.contains("grep -i")) {
                    reader.close();
                    return true;
                }
            }

            reader.close();
            return false;
        } catch (Exception e) {
            Logger.error("Failed to check screen sharing status", e);
            return false;
        }
    }

    @Override
    public String getScreenSharingApplication() {
        try {
            // Check for known screen sharing applications
            List<String> processes = getRunningProcesses();
            for (String process : processes) {
                String lowerCase = process.toLowerCase();
                if (lowerCase.contains("zoom") ||
                        lowerCase.contains("teams") ||
                        lowerCase.contains("webex") ||
                        lowerCase.contains("meet") ||
                        lowerCase.contains("screenflow") ||
                        lowerCase.contains("screencast") ||
                        lowerCase.contains("anydesk") ||
                        lowerCase.contains("teamviewer")) {
                    return process;
                }
            }

            return "";
        } catch (Exception e) {
            Logger.error("Failed to get screen sharing application", e);
            return "";
        }
    }

    @Override
    public boolean isUserActive() {
        // Check if there was activity in the last 30 seconds
        return System.currentTimeMillis() - lastUserActivityTime < 30000;
    }

    @Override
    public String getPrimaryMacAddress() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();
                if (!ni.isLoopback() && ni.isUp()) {
                    byte[] hardwareAddress = ni.getHardwareAddress();
                    if (hardwareAddress != null) {
                        StringBuilder sb = new StringBuilder();
                        for (byte b : hardwareAddress) {
                            sb.append(String.format("%02X:", b));
                        }
                        if (sb.length() > 0) {
                            sb.deleteCharAt(sb.length() - 1);
                        }
                        return sb.toString();
                    }
                }
            }

            return "";
        } catch (Exception e) {
            Logger.error("Failed to get MAC address", e);
            return "";
        }
    }

    @Override
    public String getDeviceIdentifier() {
        try {
            // Get macOS serial number
            Process process = Runtime.getRuntime().exec("system_profiler SPHardwareDataType | grep 'Serial Number'");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line = reader.readLine();
            reader.close();

            if (line != null && !line.isEmpty()) {
                Pattern pattern = Pattern.compile("Serial Number[^:]*: (.*)");
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    return matcher.group(1).trim();
                }
            }

            return "";
        } catch (Exception e) {
            Logger.error("Failed to get device identifier", e);
            return "";
        }
    }

    @Override
    public void cleanup() {
        Logger.info("Cleaning up macOS platform implementation");
        // Remove AWT event listener if needed
        try {
            Toolkit.getDefaultToolkit().removeAWTEventListener(null);
        } catch (Exception e) {
            Logger.error("Error during cleanup", e);
        }
    }
}