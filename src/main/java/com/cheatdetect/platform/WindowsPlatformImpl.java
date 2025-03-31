package com.cheatdetect.platform;

import com.cheatdetect.utils.Logger;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
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
            // On Windows, we identify system tray apps by looking for hidden windows
            WinUser.WNDENUMPROC enumProc = (hwnd, param) -> {
                if (user32.IsWindowVisible(hwnd)) {
                    return true;
                }

                int length = user32.GetWindowTextLength(hwnd) + 1;
                char[] buffer = new char[length];
                user32.GetWindowText(hwnd, buffer, length);
                String title = Native.toString(buffer);

                if (!title.isEmpty()) {
                    IntByReference processId = new IntByReference();
                    user32.GetWindowThreadProcessId(hwnd, processId);

                    WinNT.HANDLE process = kernel32.OpenProcess(
                            Kernel32.PROCESS_QUERY_INFORMATION | Kernel32.PROCESS_VM_READ,
                            false,
                            processId.getValue());

                    if (process != null) {
                        char[] moduleName = new char[1024];
                        Psapi.INSTANCE.GetModuleFileNameExW(process, null, moduleName, moduleName.length);
                        String processName = Native.toString(moduleName);

                        if (!processName.isEmpty()) {
                            // Extract process name from full path
                            int lastBackslashIndex = processName.lastIndexOf('\\');
                            if (lastBackslashIndex != -1) {
                                processName = processName.substring(lastBackslashIndex + 1);
                            }

                            trayApps.add(processName);
                        }

                        kernel32.CloseHandle(process);
                    }
                }

                return true;
            };

            user32.EnumWindows(enumProc, null);
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
            // Check for camera usage by looking for specific processes
            List<String> processes = getRunningProcesses();
            for (String process : processes) {
                String lowerCase = process.toLowerCase();
                if (lowerCase.contains("webcam") ||
                        lowerCase.contains("camera") ||
                        isCameraProcessName(lowerCase)) {
                    return true;
                }
            }

            // Alternative method: Try to detect camera usage via DirectShow
            // This requires more complex JNA bindings and might not be reliable

            return false;
        } catch (Exception e) {
            Logger.error("Failed to check camera status", e);
            return false;
        }
    }

    private boolean isCameraProcessName(String processName) {
        return processName.equals("mswebcam.exe") ||
                processName.equals("webexmta.exe") ||
                processName.equals("zoom.exe") ||
                processName.equals("teams.exe") ||
                processName.equals("skype.exe") ||
                processName.equals("webex.exe");
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
            // Check for screen sharing by looking for specific processes
            // This is a simple heuristic; more accurate detection would require hooking into Windows APIs
            List<String> processes = getRunningProcesses();
            for (String process : processes) {
                String lowerCase = process.toLowerCase();
                if (lowerCase.contains("screenshare") ||
                        lowerCase.contains("screencast") ||
                        isScreenSharingProcessName(lowerCase)) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            Logger.error("Failed to check screen sharing status", e);
            return false;
        }
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
            // Get Windows-specific device ID using WMI
            Process process = Runtime.getRuntime().exec(
                    "wmic csproduct get UUID");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.contains("UUID")) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        return line;
                    }
                }
            }

            reader.close();
            return "";
        } catch (Exception e) {
            Logger.error("Failed to get device identifier", e);
            return "";
        }
    }

    @Override
    public void cleanup() {
        // No specific resources to clean up
        Logger.info("Cleaning up Windows platform implementation");
    }
}