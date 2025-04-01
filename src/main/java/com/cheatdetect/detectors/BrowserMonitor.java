package com.cheatdetect.detectors;

import com.cheatdetect.api.EventListener;
import com.cheatdetect.core.Configuration;
import com.cheatdetect.platform.PlatformInterface;
import com.cheatdetect.utils.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Monitors browser activities including tab count and multiple browser instances.
 */
public class BrowserMonitor {
    private final Configuration config;
    private final PlatformInterface platform;
    private final Set<EventListener> listeners;
    private Map<String, Integer> lastBrowserTabCounts;
    private int lastTotalTabCount;
    private int lastBrowserCount;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean isRunning;

    // Common browser process names
    private static final String[] BROWSER_PROCESSES = {
            "chrome", "firefox", "msedge", "opera", "brave", "safari", "iexplore"
    };

    /**
     * Creates a new BrowserMonitor.
     *
     * @param config   the configuration to use
     * @param platform the platform implementation
     */
    public BrowserMonitor(Configuration config, PlatformInterface platform) {
        this.config = config;
        this.platform = platform;
        this.listeners = new HashSet<>();
        this.lastBrowserTabCounts = new HashMap<>();
        this.lastTotalTabCount = 0;
        this.lastBrowserCount = 0;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.isRunning = new AtomicBoolean(false);
    }

    /**
     * Starts monitoring browser activity.
     */
    public void startMonitoring() {
        if (isRunning.compareAndSet(false, true)) {
            Logger.info("Starting browser monitoring");

            // Reset state
            lastBrowserTabCounts = new HashMap<>();
            lastTotalTabCount = 0;
            lastBrowserCount = 0;

            scheduler.scheduleAtFixedRate(() -> {
                try {
                    detect();
                } catch (Exception e) {
                    Logger.error("Error detecting browser activity", e);
                }
            }, 0, 5000, TimeUnit.MILLISECONDS); // Check every 5 seconds
        }
    }

    /**
     * Stops monitoring browser activity.
     */
    public void stopMonitoring() {
        if (isRunning.compareAndSet(true, false)) {
            Logger.info("Stopping browser monitoring");
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Detects browser activities.
     */
    public void detect() {
        try {
            Map<String, Integer> currentBrowserTabCounts = getBrowserTabCounts();
            int totalTabCount = 0;
            int browserCount = currentBrowserTabCounts.size();

            for (int count : currentBrowserTabCounts.values()) {
                totalTabCount += count;
            }

            if (config.isDetailedLoggingEnabled()) {
                Logger.debug("Browser count: " + browserCount + ", Total tab count: " + totalTabCount);
                for (Map.Entry<String, Integer> entry : currentBrowserTabCounts.entrySet()) {
                    Logger.debug(entry.getKey() + ": " + entry.getValue() + " tabs");
                }
            }

            // Check for significant changes in browser or tab counts
            if (totalTabCount > lastTotalTabCount && totalTabCount > 5) {
                notifyListeners("INFO_BROWSER_TABS",
                        "High browser tab count detected: " + totalTabCount + " tabs");
            }

            if (browserCount > lastBrowserCount && browserCount > 1) {
                notifyListeners("ALERT_MULTIPLE_BROWSERS",
                        "Multiple browser instances detected: " + browserCount + " browsers");

                // Include the list of browsers
                StringBuilder browsers = new StringBuilder("Active browsers: ");
                for (String browser : currentBrowserTabCounts.keySet()) {
                    browsers.append(browser).append(" (").append(currentBrowserTabCounts.get(browser)).append(" tabs), ");
                }
                notifyListeners("INFO_BROWSER_LIST", browsers.substring(0, browsers.length() - 2));
            }

            // Check for any browser that has many tabs open
            for (Map.Entry<String, Integer> entry : currentBrowserTabCounts.entrySet()) {
                String browser = entry.getKey();
                int currentCount = entry.getValue();
                int previousCount = lastBrowserTabCounts.getOrDefault(browser, 0);

                if (currentCount > previousCount && currentCount > 10) {
                    notifyListeners("ALERT_EXCESSIVE_TABS",
                            browser + " has " + currentCount + " tabs open");
                }
            }

            // Update state for next check
            lastBrowserTabCounts = currentBrowserTabCounts;
            lastTotalTabCount = totalTabCount;
            lastBrowserCount = browserCount;

        } catch (Exception e) {
            Logger.error("Error detecting browser activity", e);
        }
    }

    /**
     * Gets the count of tabs for each browser.
     *
     * @return a map of browser names to tab counts
     */
    private Map<String, Integer> getBrowserTabCounts() {
        Map<String, Integer> tabCounts = new HashMap<>();

        try {
            // Determine OS using system properties instead of platform interface
            String osName = System.getProperty("os.name").toLowerCase();

            if (osName.contains("win")) {
                getBrowserTabCountsWindows(tabCounts);
            } else if (osName.contains("mac")) {
                getBrowserTabCountsMacOS(tabCounts);
            } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
                getBrowserTabCountsLinux(tabCounts);
            }
        } catch (Exception e) {
            Logger.error("Error getting browser tab counts", e);
        }

        return tabCounts;
    }

    /**
     * Gets the count of tabs for each browser on Windows.
     *
     * @param tabCounts the map to populate with browser tab counts
     */
    private void getBrowserTabCountsWindows(Map<String, Integer> tabCounts) throws Exception {
        // Chrome
        getWindowsTabCount(tabCounts, "chrome.exe", "chrome");

        // Firefox
        getWindowsTabCount(tabCounts, "firefox.exe", "firefox");

        // Edge
        getWindowsTabCount(tabCounts, "msedge.exe", "edge");

        // Internet Explorer
        getWindowsTabCount(tabCounts, "iexplore.exe", "ie");

        // Opera
        getWindowsTabCount(tabCounts, "opera.exe", "opera");

        // Brave
        getWindowsTabCount(tabCounts, "brave.exe", "brave");
    }

    /**
     * Gets the tab count for a specific browser on Windows.
     *
     * @param tabCounts   the map to populate with browser tab counts
     * @param processName the process name to look for
     * @param browserName the friendly name of the browser
     */
    private void getWindowsTabCount(Map<String, Integer> tabCounts, String processName, String browserName) throws Exception {
        Process process = Runtime.getRuntime().exec("tasklist /fi \"imagename eq " + processName + "\" /v");
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String line;
        int instanceCount = 0;
        int estimatedTabCount = 0;

        while ((line = reader.readLine()) != null) {
            if (line.contains(processName)) {
                instanceCount++;

                // Roughly estimate tab count based on memory usage
                // This is a heuristic and not entirely accurate
                Pattern memPattern = Pattern.compile("\\b(\\d+)\\s+K\\b");
                Matcher memMatcher = memPattern.matcher(line);
                if (memMatcher.find()) {
                    int memory = Integer.parseInt(memMatcher.group(1));
                    // Roughly estimate - each tab might use ~25-50MB
                    estimatedTabCount += Math.max(1, memory / 50000);
                } else {
                    estimatedTabCount += 1; // Default to at least 1 tab per process
                }
            }
        }
        reader.close();

        if (instanceCount > 0) {
            tabCounts.put(browserName, estimatedTabCount);
        }
    }

    /**
     * Gets the count of tabs for each browser on macOS.
     *
     * @param tabCounts the map to populate with browser tab counts
     */
    private void getBrowserTabCountsMacOS(Map<String, Integer> tabCounts) throws Exception {
        // Get all browser processes
        Process process = Runtime.getRuntime().exec("ps -ex | grep -i 'chrome\\|firefox\\|safari\\|opera\\|brave\\|edge'");
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        Map<String, Integer> processCounter = new HashMap<>();
        String line;

        while ((line = reader.readLine()) != null) {
            if (line.contains("grep")) continue; // Skip the grep process itself

            String lowerLine = line.toLowerCase();
            for (String browser : BROWSER_PROCESSES) {
                if (lowerLine.contains(browser)) {
                    processCounter.put(browser, processCounter.getOrDefault(browser, 0) + 1);
                    break;
                }
            }
        }
        reader.close();

        // For each browser, attempt to get window/tab count with AppleScript
        for (String browser : processCounter.keySet()) {
            if (browser.equals("chrome")) {
                getAppleScriptTabCount(tabCounts, "Google Chrome", "chrome");
            } else if (browser.equals("firefox")) {
                getAppleScriptTabCount(tabCounts, "Firefox", "firefox");
            } else if (browser.equals("safari")) {
                getAppleScriptTabCount(tabCounts, "Safari", "safari");
            } else if (browser.equals("opera")) {
                getAppleScriptTabCount(tabCounts, "Opera", "opera");
            } else if (browser.equals("brave")) {
                getAppleScriptTabCount(tabCounts, "Brave Browser", "brave");
            } else if (browser.equals("msedge")) {
                getAppleScriptTabCount(tabCounts, "Microsoft Edge", "edge");
            }
        }
    }

    /**
     * Gets the tab count for a specific browser on macOS using AppleScript.
     *
     * @param tabCounts      the map to populate with browser tab counts
     * @param appleBrowserName the name of the browser as known to AppleScript
     * @param browserName    the friendly name of the browser
     */
    private void getAppleScriptTabCount(Map<String, Integer> tabCounts, String appleBrowserName, String browserName) throws Exception {
        String[] cmd = {
                "osascript",
                "-e", "tell application \"" + appleBrowserName + "\" to count every tab of every window"
        };

        Process process = Runtime.getRuntime().exec(cmd);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String result = reader.readLine();
        reader.close();

        if (result != null && !result.isEmpty()) {
            try {
                int tabCount = Integer.parseInt(result.trim());
                tabCounts.put(browserName, tabCount);
            } catch (NumberFormatException e) {
                // Fallback to process count
                Process countProcess = Runtime.getRuntime().exec("pgrep -f " + browserName);
                BufferedReader countReader = new BufferedReader(new InputStreamReader(countProcess.getInputStream()));
                int count = 0;
                while (countReader.readLine() != null) {
                    count++;
                }
                countReader.close();
                tabCounts.put(browserName, Math.max(1, count - 1)); // Subtract 1 for main process
            }
        }
    }

    /**
     * Gets the count of tabs for each browser on Linux.
     *
     * @param tabCounts the map to populate with browser tab counts
     */
    private void getBrowserTabCountsLinux(Map<String, Integer> tabCounts) throws Exception {
        // Get all browser processes
        Process process = Runtime.getRuntime().exec("ps ax | grep -i 'chrome\\|firefox\\|opera\\|brave\\|edge'");
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        Map<String, List<String>> processList = new HashMap<>();
        String line;

        while ((line = reader.readLine()) != null) {
            if (line.contains("grep")) continue; // Skip the grep process itself

            String lowerLine = line.toLowerCase();
            for (String browser : BROWSER_PROCESSES) {
                if (lowerLine.contains(browser)) {
                    processList.computeIfAbsent(browser, k -> new ArrayList<>()).add(line);
                    break;
                }
            }
        }
        reader.close();

        // Estimate tab count based on process count and command line arguments
        for (Map.Entry<String, List<String>> entry : processList.entrySet()) {
            String browser = entry.getKey();
            List<String> processes = entry.getValue();

            // Heuristic: each tab might have its own process or thread
            // For browsers like Firefox, it's trickier to estimate
            int estimatedTabCount = Math.max(1, processes.size() - 1); // Subtract 1 for main process

            tabCounts.put(browser, estimatedTabCount);
        }

        // For Firefox specifically, try to get actual tab count using xdotool
        try {
            Process xdoProcess = Runtime.getRuntime().exec("xdotool search --class Firefox getwindowname");
            BufferedReader xdoReader = new BufferedReader(new InputStreamReader(xdoProcess.getInputStream()));
            int firefoxTabs = 0;

            while ((line = xdoReader.readLine()) != null) {
                if (line.contains("-") || line.contains("Firefox")) {
                    firefoxTabs++;
                }
            }
            xdoReader.close();

            if (firefoxTabs > 0) {
                tabCounts.put("firefox", firefoxTabs);
            }
        } catch (Exception e) {
            // xdotool might not be available, stick with the heuristic estimate
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