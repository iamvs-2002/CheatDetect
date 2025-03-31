package com.cheatdetect.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for process-related operations.
 */
public class ProcessUtils {

    // List of known virtualization software process names
    private static final Set<String> VIRTUALIZATION_PROCESSES = new HashSet<>(Arrays.asList(
            "vmware", "virtualbox", "vboxservice", "vboxtray", "vmwaretray", "vmwareservice",
            "vmwarep2v", "vmusrvc", "vbox", "qemu", "kvm", "xen", "virtualpc", "parallels",
            "hyperv", "vmcompute", "vmms", "vmwp"
    ));

    // List of known remote access process names
    private static final Set<String> REMOTE_ACCESS_PROCESSES = new HashSet<>(Arrays.asList(
            "teamviewer", "anydesk", "ammyy", "vnc", "x11vnc", "tightvnc", "ultravnc",
            "realvnc", "logmein", "gotomypc", "remotepc", "screenconnect", "bomgar",
            "supremo", "remotedesktop", "mstsc", "rdesktop", "rdp", "ssh", "tmate"
    ));

    // List of known AI assistant process names
    private static final Set<String> AI_ASSISTANT_PROCESSES = new HashSet<>(Arrays.asList(
            "chatgpt", "copilot", "gemini", "codegeex", "tabnine", "kite", "aichat",
            "gpt", "bard", "claude", "anthropic", "llama", "codex", "codewhisperer"
    ));

    /**
     * Checks if a process name belongs to a known virtualization software.
     *
     * @param processName the name of the process to check
     * @return true if the process is a virtualization software, false otherwise
     */
    public static boolean isVirtualizationProcess(String processName) {
        if (processName == null || processName.isEmpty()) {
            return false;
        }

        String lowerCase = processName.toLowerCase();

        // Check against known virtualization process names
        for (String vProcess : VIRTUALIZATION_PROCESSES) {
            if (lowerCase.contains(vProcess)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if a process name belongs to a known remote access software.
     *
     * @param processName the name of the process to check
     * @return true if the process is a remote access software, false otherwise
     */
    public static boolean isRemoteAccessProcess(String processName) {
        if (processName == null || processName.isEmpty()) {
            return false;
        }

        String lowerCase = processName.toLowerCase();

        // Check against known remote access process names
        for (String raProcess : REMOTE_ACCESS_PROCESSES) {
            if (lowerCase.contains(raProcess)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if a process name belongs to a known AI assistant.
     *
     * @param processName the name of the process to check
     * @return true if the process is an AI assistant, false otherwise
     */
    public static boolean isAIAssistantProcess(String processName) {
        if (processName == null || processName.isEmpty()) {
            return false;
        }

        String lowerCase = processName.toLowerCase();

        // Check against known AI assistant process names
        for (String aiProcess : AI_ASSISTANT_PROCESSES) {
            if (lowerCase.contains(aiProcess)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if a process name is a browser.
     *
     * @param processName the name of the process to check
     * @return true if the process is a browser, false otherwise
     */
    public static boolean isBrowserProcess(String processName) {
        if (processName == null || processName.isEmpty()) {
            return false;
        }

        String lowerCase = processName.toLowerCase();

        return lowerCase.contains("chrome") ||
                lowerCase.contains("firefox") ||
                lowerCase.contains("safari") ||
                lowerCase.contains("edge") ||
                lowerCase.contains("opera") ||
                lowerCase.contains("brave") ||
                lowerCase.contains("vivaldi") ||
                lowerCase.contains("iexplore");
    }

    /**
     * Sanitizes a process name for safer logging and comparison.
     *
     * @param processName the name of the process to sanitize
     * @return the sanitized process name
     */
    public static String sanitizeProcessName(String processName) {
        if (processName == null) {
            return "";
        }

        // Remove any non-alphanumeric characters and convert to lowercase
        return processName.replaceAll("[^a-zA-Z0-9\\._-]", "").toLowerCase();
    }
}