package com.cheatdetect.platform;

import com.cheatdetect.utils.Logger;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Windows-specific implementation of the platform interface.
 */
public class WindowsPlatformImpl implements PlatformInterface {

    private static final User32 user32 = User32.INSTANCE;
    private static final Kernel32 kernel32 = Kernel32.INSTANCE;

    private long lastInputTime = 0;

    public WindowsPlatformImpl() {
        Logger.info("Initializing Windows platform implementation");
    }

    @Override
    public String getActiveWindowTitle() {
        WinDef.HWND hwnd = user32.GetForegroundWindow();
        if (hwnd == null) {
            return "";
        }

        char[] buffer = new char[1024];
        user32.GetWindowText(hwnd, buffer, buffer.length);
        return Native.toString(buffer);
    }

    @Override
    public List<String> getRunningProcesses() {
        List<String> processes = new ArrayList<>();

        try {
            Process process = Runtime.getRuntime().exec("tasklist /fo csv /nh");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                // Parse CSV output format: "process.exe","PID","Session Name","Session#","Mem Usage"
                if (line.startsWith("\"")) {
                    String[] parts = line.split("\",\"");
                    if (parts.length > 0) {
                        String processName = parts[0].replaceAll("\"", "");
                        processes.add(processName);
                    }
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
            // Get running processes that might be in the system tray
            Process process = Runtime.getRuntime().exec("tasklist /fo csv /nh");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                // Simple check for background apps
                if (line.contains("tray") || line.contains("systray") ||
                        line.contains("notify") || line.contains("background")) {
                    trayApps.add(line);
                }
            }
            reader.close();

            // For a more comprehensive approach, check shell_trayWnd window
            // This would require JNA and would be more complex

            // Add common system tray apps that might not be caught
            checkAndAddCommonTrayApp(trayApps, "Discord.exe");
            checkAndAddCommonTrayApp(trayApps, "Slack.exe");
            checkAndAddCommonTrayApp(trayApps, "Teams.exe");
            checkAndAddCommonTrayApp(trayApps, "OneDrive.exe");
            checkAndAddCommonTrayApp(trayApps, "Dropbox.exe");

        } catch (Exception e) {
            Logger.error("Failed to get system tray applications", e);
        }

        return trayApps;
    }

    private void checkAndAddCommonTrayApp(List<String> trayApps, String appName) {
        try {
            Process process = Runtime.getRuntime().exec("tasklist /fi \"imagename eq " + appName + "\"");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(appName)) {
                    trayApps.add(appName);
                    break;
                }
            }
            reader.close();
        } catch (Exception e) {
            // Ignore errors for this helper method
        }
    }

    @Override
    public boolean isCameraActive() {
        try {
            // Check if common video apps are running
            for (String videoApp : new String[]{"zoom", "teams", "webex", "skype", "meet", "chrome"}) {
                Process process = Runtime.getRuntime().exec("tasklist /fi \"imagename eq " +
                        videoApp + ".exe\"");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line;
                boolean found = false;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(videoApp)) {
                        found = true;
                        break;
                    }
                }
                reader.close();

                if (found) {
                    // For Chrome, do an additional check for Google Meet
                    if (videoApp.equals("chrome")) {
                        String activeWindow = getActiveWindowTitle().toLowerCase();
                        if (activeWindow.contains("meet.google.com") || activeWindow.contains("google meet")) {
                            return true;
                        }
                    } else {
                        return true;
                    }
                }
            }

            // Additional checks for DirectShow/media foundation usage would require JNA
            return false;
        } catch (Exception e) {
            Logger.error("Failed to check camera status", e);
            return false;
        }
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
    public boolean isMicrophoneActive() {
        try {
            // Check for microphone usage using Windows APIs
            // This is a simplified approach; a more accurate method would use Windows Core Audio APIs

            // For now, we'll use a heuristic based on active processes
            List<String> processes = getRunningProcesses();
            for (String process : processes) {
                String lowerCase = process.toLowerCase();
                if (lowerCase.contains("audio") ||
                        lowerCase.contains("microphone") ||
                        isMicrophoneProcessName(lowerCase)) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            Logger.error("Failed to check microphone status", e);
            return false;
        }
    }

    private boolean isMicrophoneProcessName(String processName) {
        return processName.equals("audiodg.exe") ||
                processName.equals("rtkaudioservice.exe") ||
                processName.equals("zoom.exe") ||
                processName.equals("teams.exe") ||
                processName.equals("skype.exe") ||
                processName.equals("webex.exe");
    }

    @Override
    public boolean isScreenSharing() {
        try {
            // Check for screen sharing by looking for specific processes and window titles
            String activeWindow = getActiveWindowTitle().toLowerCase();
            if (activeWindow.contains("sharing") || activeWindow.contains("present") ||
                    activeWindow.contains("screen")) {
                return true;
            }

            // Look for DWM processes with screen sharing
            Process process = Runtime.getRuntime().exec(
                    "tasklist /fi \"imagename eq dwm.exe\" /v");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.toLowerCase().contains("screen sharing") ||
                        line.toLowerCase().contains("presenting")) {
                    reader.close();
                    return true;
                }
            }
            reader.close();

            // Check Window classes that might indicate screen sharing
            return checkScreenSharingWindows();
        } catch (Exception e) {
            Logger.error("Failed to check screen sharing status", e);
            return false;
        }
    }

    private boolean checkScreenSharingWindows() {
        // This would ideally use JNA to check for specific window classes
        // that indicate screen sharing

        // For Google Meet specifically, check for processes
        try {
            Process process = Runtime.getRuntime().exec("tasklist /fi \"imagename eq chrome.exe\" /v");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.toLowerCase().contains("meet.google.com")) {
                    // Now check if this Chrome instance is likely screen sharing
                    String activeWindow = getActiveWindowTitle().toLowerCase();
                    if (activeWindow.contains("meet") &&
                            (activeWindow.contains("presenting") || activeWindow.contains("you are presenting"))) {
                        reader.close();
                        return true;
                    }
                }
            }
            reader.close();
        } catch (Exception e) {
            // Ignore errors in this additional check
        }

        return false;
    }

    private boolean isScreenSharingProcessName(String processName) {
        return processName.equals("zoom.exe") ||
                processName.equals("teams.exe") ||
                processName.equals("skype.exe") ||
                processName.equals("webex.exe") ||
                processName.equals("discord.exe") ||
                processName.equals("anydesk.exe") ||
                processName.equals("teamviewer.exe");
    }

    @Override
    public String getScreenSharingApplication() {
        try {
            List<String> processes = getRunningProcesses();
            for (String process : processes) {
                String lowerCase = process.toLowerCase();
                if (isScreenSharingProcessName(lowerCase)) {
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
        try {
            User32.LASTINPUTINFO lastInputInfo = new User32.LASTINPUTINFO();
            lastInputInfo.cbSize = lastInputInfo.size();

            if (user32.GetLastInputInfo(lastInputInfo)) {
                long currentTime = kernel32.GetTickCount();
                long idleTime = currentTime - lastInputInfo.dwTime;

                // If idle time is less than 5 seconds, user is considered active
                return idleTime < 5000;
            }

            return false;
        } catch (Exception e) {
            Logger.error("Failed to check user activity", e);
            return false;
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
            // Try primary method using WMI
            try {
                Process process = Runtime.getRuntime().exec("wmic csproduct get UUID");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.contains("UUID")) {
                        line = line.trim();
                        if (!line.isEmpty()) {
                            reader.close();
                            return line;
                        }
                    }
                }
                reader.close();
            } catch (IOException e) {
                // WMIC command failed, try alternative method
                Logger.debug("WMIC command failed, trying alternative method: " + e.getMessage());
            }

            // Alternative method using PowerShell
            Process process = Runtime.getRuntime().exec(
                    new String[] {
                            "powershell.exe",
                            "-Command",
                            "(Get-CimInstance -Class Win32_ComputerSystemProduct).UUID"
                    });

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            reader.close();

            if (line != null && !line.isEmpty()) {
                return line.trim();
            }

            // If both methods fail, fall back to a combination of computer name and user name
            String computerName = System.getenv("COMPUTERNAME");
            String userName = System.getProperty("user.name");

            if (computerName != null && userName != null) {
                return calculateSHA256(computerName + "-" + userName);
            }

            return "";
        } catch (Exception e) {
            Logger.error("Failed to get device identifier", e);
            return "";
        }
    }

    private String calculateSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte hashByte : hashBytes) {
                String hex = Integer.toHexString(0xff & hashByte);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            Logger.error("SHA-256 algorithm not available", e);
            return "";
        }
    }

    @Override
    public void cleanup() {
        // No specific resources to clean up
        Logger.info("Cleaning up Windows platform implementation");
    }
}