package com.cheatdetect.platform;

import com.cheatdetect.utils.Logger;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Linux-specific implementation of the platform interface.
 */
public class LinuxPlatformImpl implements PlatformInterface {

    private long lastUserActivityTime = System.currentTimeMillis();

    public LinuxPlatformImpl() {
        Logger.info("Initializing Linux platform implementation");

        // Initialize any Linux-specific resources
        try {
            // Set up activity monitoring
            initActivityMonitoring();
        } catch (Exception e) {
            Logger.error("Failed to initialize Linux platform implementation", e);
        }
    }

    private void initActivityMonitoring() {
        // On Linux we rely on AWT events for basic activity monitoring
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            lastUserActivityTime = System.currentTimeMillis();
        }, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
    }

    @Override
    public String getActiveWindowTitle() {
        try {
            // Use xdotool to get the active window title on X11-based Linux
            Process process = Runtime.getRuntime().exec("xdotool getwindowfocus getwindowname");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line = reader.readLine();
            reader.close();

            return line != null ? line : "";
        } catch (Exception e) {
            // xdotool might not be installed or we might be on Wayland
            try {
                // Try another approach with wmctrl
                Process process = Runtime.getRuntime().exec("wmctrl -l | grep $(xprop -root _NET_ACTIVE_WINDOW | cut -d ' ' -f 5)");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line = reader.readLine();
                reader.close();

                if (line != null && !line.isEmpty()) {
                    // Extract window title from the output
                    // Format: window_id desktop_id hostname window_title
                    String[] parts = line.split("\\s+", 4);
                    if (parts.length >= 4) {
                        return parts[3];
                    }
                }

                return "";
            } catch (Exception ex) {
                Logger.error("Failed to get active window title", ex);
                return "";
            }
        }
    }

    @Override
    public List<String> getRunningProcesses() {
        List<String> processes = new ArrayList<>();

        try {
            // Use ps command to get the list of running processes
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
            // Try to get system tray icons from common desktop environments

            // For GNOME, try using D-Bus
            Process process = Runtime.getRuntime().exec(
                    "dbus-send --session --dest=org.kde.StatusNotifierWatcher " +
                            "--type=method_call --print-reply /StatusNotifierWatcher " +
                            "org.kde.StatusNotifierWatcher.RegisteredStatusNotifierItems");

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            // Parse the output to extract application names
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("string")) {
                    String[] parts = line.split("\"");
                    if (parts.length >= 2) {
                        String appName = parts[1].split("/")[0];
                        if (!appName.isEmpty() && !trayApps.contains(appName)) {
                            trayApps.add(appName);
                        }
                    }
                }
            }

            reader.close();

            // If no results, try another approach for different desktop environments
            if (trayApps.isEmpty()) {
                // For some desktop environments, check for specific processes
                List<String> processes = getRunningProcesses();
                for (String proc : processes) {
                    if (proc.contains("indicator") || proc.contains("tray") || proc.contains("status")) {
                        trayApps.add(proc);
                    }
                }
            }

        } catch (Exception e) {
            Logger.error("Failed to get system tray applications", e);
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
            // Check if any process is using the video device
            Process process = Runtime.getRuntime().exec("lsof /dev/video*");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            boolean hasOutput = reader.readLine() != null;
            reader.close();

            return hasOutput;
        } catch (Exception e) {
            Logger.error("Failed to check camera status", e);
            return false;
        }
    }

    @Override
    public boolean isMicrophoneActive() {
        try {
            // Check if any process is using the audio device
            Process process = Runtime.getRuntime().exec("lsof -c pulseaudio | grep -i 'PCM\\|AUDIO'");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            boolean hasOutput = reader.readLine() != null;
            reader.close();

            // Also check if any audio recording processes are running
            Process process2 = Runtime.getRuntime().exec("ps -e | grep -i 'pulse\\|alsa\\|audio'");
            BufferedReader reader2 = new BufferedReader(new InputStreamReader(process2.getInputStream()));
            String line;

            while ((line = reader2.readLine()) != null) {
                if (!line.contains("grep")) {
                    reader2.close();
                    return true;
                }
            }

            reader2.close();
            return hasOutput;
        } catch (Exception e) {
            Logger.error("Failed to check microphone status", e);
            return false;
        }
    }

    @Override
    public boolean isScreenSharing() {
        try {
            // Check for common screen sharing processes on Linux
            Process process = Runtime.getRuntime().exec(
                    "ps -e | grep -i 'vnc\\|x11vnc\\|teamviewer\\|anydesk\\|zoom\\|webex\\|teams'");

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                if (!line.contains("grep")) {
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
            String[] checkProcesses = {
                    "vnc", "x11vnc", "teamviewer", "anydesk", "zoom", "webex", "teams", "meet", "skype"
            };

            for (String processName : checkProcesses) {
                Process process = Runtime.getRuntime().exec("ps -e | grep -i " + processName);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;

                while ((line = reader.readLine()) != null) {
                    if (!line.contains("grep")) {
                        // Extract the actual process name
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length >= 4) {
                            reader.close();
                            return parts[3];
                        } else {
                            reader.close();
                            return processName;
                        }
                    }
                }

                reader.close();
            }

            return "";
        } catch (Exception e) {
            Logger.error("Failed to get screen sharing application", e);
            return "";
        }
    }

    @Override
    public boolean isUserActive() {
        try {
            // First check our internal AWT-based activity tracking
            if (System.currentTimeMillis() - lastUserActivityTime < 30000) {
                return true;
            }

            // For Linux, also try to check X11 idle time if we have xprintidle installed
            Process process = Runtime.getRuntime().exec("xprintidle");
            process.waitFor(1, TimeUnit.SECONDS);

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            reader.close();

            if (line != null && !line.isEmpty()) {
                long idleTimeMs = Long.parseLong(line.trim());
                return idleTimeMs < 30000; // User is active if idle for less than 30 seconds
            }

            // Fallback to our AWT-based tracking
            return System.currentTimeMillis() - lastUserActivityTime < 30000;
        } catch (Exception e) {
            // Fallback if xprintidle is not available
            return System.currentTimeMillis() - lastUserActivityTime < 30000;
        }
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
            // Try to get the machine-id which is a unique identifier on Linux systems
            File machineIdFile = new File("/etc/machine-id");
            if (machineIdFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(machineIdFile))) {
                    String machineId = reader.readLine();
                    if (machineId != null && !machineId.isEmpty()) {
                        return machineId.trim();
                    }
                }
            }

            // Fallback to DMI system UUID
            File dmidecodeFile = new File("/sys/class/dmi/id/product_uuid");
            if (dmidecodeFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(dmidecodeFile))) {
                    String uuid = reader.readLine();
                    if (uuid != null && !uuid.isEmpty()) {
                        return uuid.trim();
                    }
                }
            }

            // Fallback to trying to run dmidecode (requires root privileges)
            Process process = Runtime.getRuntime().exec("sudo dmidecode -s system-uuid");
            process.waitFor(1, TimeUnit.SECONDS);

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            reader.close();

            if (line != null && !line.isEmpty()) {
                return line.trim();
            }

            return "";
        } catch (Exception e) {
            Logger.error("Failed to get device identifier", e);
            return "";
        }
    }

    @Override
    public void cleanup() {
        Logger.info("Cleaning up Linux platform implementation");
        // Remove AWT event listener if needed
        try {
            Toolkit.getDefaultToolkit().removeAWTEventListener(null);
        } catch (Exception e) {
            Logger.error("Error during cleanup", e);
        }
    }
}