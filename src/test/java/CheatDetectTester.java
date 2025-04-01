import com.cheatdetect.api.AlertCallback;
import com.cheatdetect.api.CheatDetectAPI;
import com.cheatdetect.core.Configuration;
import com.cheatdetect.utils.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CheatDetectTester extends JFrame {
    private String instanceId;
    private DefaultTableModel tableModel;
    private JTable alertTable;
    private JLabel statusLabel;
    private Map<String, JLabel> detectorStatusMap = new HashMap<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private JTextArea logTextArea;

    public CheatDetectTester() {
        setTitle("CheatDetect Test Dashboard");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create UI components
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Control panel with buttons
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton startButton = new JButton("Start Monitoring");
        JButton stopButton = new JButton("Stop Monitoring");
        JButton clearButton = new JButton("Clear Alerts");

        controlPanel.add(startButton);
        controlPanel.add(stopButton);
        controlPanel.add(clearButton);

        // Status label
        statusLabel = new JLabel("Status: Not Monitoring");
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        statusPanel.add(statusLabel);

        // Top panel combining controls and status
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(controlPanel, BorderLayout.WEST);
        topPanel.add(statusPanel, BorderLayout.EAST);

        // Detector status panel
        JPanel detectorPanel = new JPanel(new GridLayout(5, 2, 5, 5));
        detectorPanel.setBorder(BorderFactory.createTitledBorder("Detector Status"));

        addDetectorStatus(detectorPanel, "CODING_PLATFORM", "Coding Platform");
        addDetectorStatus(detectorPanel, "SUSPICIOUS_PROCESS", "Suspicious Process");
        addDetectorStatus(detectorPanel, "CLIPBOARD", "Clipboard");
        addDetectorStatus(detectorPanel, "SCREEN_SHARING", "Screen Sharing");
        addDetectorStatus(detectorPanel, "VIDEO_CALL", "Video Call");
        addDetectorStatus(detectorPanel, "SYSTEM_TRAY", "System Tray");
        addDetectorStatus(detectorPanel, "MULTI_VIDEO", "Multiple Video Apps");
        addDetectorStatus(detectorPanel, "MULTIPLE_BROWSERS", "Multiple Browsers");
        addDetectorStatus(detectorPanel, "AI_TOOLS", "AI Tools");
        addDetectorStatus(detectorPanel, "BROWSER_TABS", "Browser Tabs");

        // Alert table
        String[] columnNames = {"Time", "Alert Type", "Details"};
        tableModel = new DefaultTableModel(columnNames, 0);
        alertTable = new JTable(tableModel);
        JScrollPane tableScrollPane = new JScrollPane(alertTable);
        tableScrollPane.setBorder(BorderFactory.createTitledBorder("Alerts"));

        // Log area
        logTextArea = new JTextArea();
        logTextArea.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(logTextArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Logs"));

        // Split pane for alerts and logs
        JSplitPane splitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                tableScrollPane,
                logScrollPane);
        splitPane.setDividerLocation(300);

        // Add panels to main panel
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(detectorPanel, BorderLayout.WEST);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        // Add main panel to frame
        add(mainPanel);

        // Setup button actions
        startButton.addActionListener(e -> startMonitoring());
        stopButton.addActionListener(e -> stopMonitoring());
        clearButton.addActionListener(e -> {
            tableModel.setRowCount(0);
            resetDetectorStatuses();
        });
    }

    private void addDetectorStatus(JPanel panel, String key, String label) {
        JLabel statusLabel = new JLabel("● " + label + ": OK");
        statusLabel.setForeground(Color.GRAY);
        panel.add(statusLabel);
        detectorStatusMap.put(key, statusLabel);
    }

    private void startMonitoring() {
        // Stop any existing monitoring
        stopMonitoring();

        // Configure CheatDetect with all detectors enabled
        Configuration config = new Configuration.Builder()
                .enableProcessMonitoring(true)
                .enableClipboardMonitoring(true)
                .enableLeetCodeDetection(true)
                .enableScreenShareDetection(true)
                .enableVideoCallDetection(true)
                .enableSystemTrayMonitoring(true)
                .enableDeviceSwitchDetection(true)
                .enableDetailedLogging(true)
                .setScanInterval(1000) // Faster scanning for testing
                .build();

        // Create instance using API
        instanceId = CheatDetectAPI.createInstance(config);

        // Enable detailed logging
        CheatDetectAPI.setDetailedLoggingEnabled(true);

        // Set custom logger
        Logger.setCustomLogger((level, message) -> {
            SwingUtilities.invokeLater(() -> {
                String logEntry = timeFormat.format(new Date()) + " [" + level + "] " + message + "\n";
                logTextArea.append(logEntry);
                logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
            });
        });

        // Register alert callback
        CheatDetectAPI.registerAlertCallback(instanceId, (alertType, details) -> {
            SwingUtilities.invokeLater(() -> {
                // Add to alert table
                tableModel.addRow(new Object[]{
                        timeFormat.format(new Date()),
                        alertType,
                        details
                });

                // Update detector status
                updateDetectorStatus(alertType, details);
            });
        });

        // Start monitoring
        CheatDetectAPI.startMonitoring(instanceId);

        // Update UI
        statusLabel.setText("Status: Monitoring Active");
        statusLabel.setForeground(new Color(0, 128, 0));

        // Start enhanced detection thread
        startEnhancedDetection();
    }

    private void startEnhancedDetection() {
        Thread enhancedDetectionThread = new Thread(() -> {
            while (instanceId != null && CheatDetectAPI.isMonitoring(instanceId)) {
                try {
                    // Check for Google Meet in browser
                    checkForGoogleMeet();

                    // Check for screen sharing
                    checkForScreenSharing();

                    // Check for Zoom
                    checkForZoom();

                    // Check for Microsoft Teams
                    checkForTeams();

                    // Sleep for a short time before next check
                    Thread.sleep(1500);
                } catch (Exception e) {
                    // Ignore errors and continue
                }
            }
        });

        enhancedDetectionThread.setDaemon(true);
        enhancedDetectionThread.start();
    }

    private void stopMonitoring() {
        if (instanceId != null) {
            CheatDetectAPI.stopMonitoring(instanceId);
            CheatDetectAPI.releaseInstance(instanceId);
            instanceId = null;

            // Update UI
            statusLabel.setText("Status: Monitoring Stopped");
            statusLabel.setForeground(Color.RED);
        }
    }

    private void updateDetectorStatus(String alertType, String details) {
        // Map alert type to category
        String category = mapAlertToCategory(alertType);

        JLabel label = detectorStatusMap.get(category);
        if (label != null) {
            label.setText("● " + label.getText().substring(2));
            label.setForeground(Color.RED);
        }
    }

    private String mapAlertToCategory(String alertType) {
        if (alertType.contains("CODING_PLATFORM")) return "CODING_PLATFORM";
        if (alertType.contains("SUSPICIOUS_PROCESS")) return "SUSPICIOUS_PROCESS";
        if (alertType.contains("CLIPBOARD")) return "CLIPBOARD";
        if (alertType.contains("SCREEN_SHARING")) return "SCREEN_SHARING";
        if (alertType.contains("VIDEO_CALL")) return "VIDEO_CALL";
        if (alertType.contains("MULTI_VIDEO")) return "MULTI_VIDEO";
        if (alertType.contains("SYSTEM_TRAY")) return "SYSTEM_TRAY";
        if (alertType.contains("BROWSER")) {
            if (alertType.contains("TABS")) return "BROWSER_TABS";
            return "MULTIPLE_BROWSERS";
        }
        if (alertType.contains("AI")) return "AI_TOOLS";

        // Default
        return "SUSPICIOUS_PROCESS";
    }

    private void resetDetectorStatuses() {
        for (JLabel label : detectorStatusMap.values()) {
            label.setForeground(Color.GRAY);
        }
    }

    // Enhanced detection methods

    private void checkForGoogleMeet() {
        try {
            // Check for Google Meet in Chrome
            Process process = Runtime.getRuntime().exec(
                    "tasklist /v | findstr /i \"chrome.exe meet.google\"");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            int meetCount;
            meetCount = (int) reader.lines().filter(line -> line.toLowerCase().contains("meet.google")).count();
            reader.close();

            if (meetCount > 0) {
                SwingUtilities.invokeLater(() -> {
                    tableModel.addRow(new Object[]{
                            timeFormat.format(new Date()),
                            "ENHANCED_VIDEO_CALL",
                            "Google Meet detected: " + meetCount + " instance(s)"
                    });
                    updateDetectorStatus("VIDEO_CALL", "");

                    if (meetCount > 1) {
                        tableModel.addRow(new Object[]{
                                timeFormat.format(new Date()),
                                "ENHANCED_MULTI_VIDEO",
                                "Multiple Google Meet instances: " + meetCount
                        });
                        updateDetectorStatus("MULTI_VIDEO", "");
                    }
                });

                // Check if presenting in Meet
                Process presentCheck = Runtime.getRuntime().exec(
                        "tasklist /v | findstr /i \"presenting you are presenting screen sharing\"");
                BufferedReader presentReader = new BufferedReader(new InputStreamReader(presentCheck.getInputStream()));

                if (presentReader.readLine() != null) {
                    SwingUtilities.invokeLater(() -> {
                        tableModel.addRow(new Object[]{
                                timeFormat.format(new Date()),
                                "ENHANCED_SCREEN_SHARING",
                                "Screen sharing detected in Google Meet"
                        });
                        updateDetectorStatus("SCREEN_SHARING", "");
                    });
                }
                presentReader.close();
            }
        } catch (Exception e) {
            // Ignore errors
        }
    }

    private void checkForScreenSharing() {
        try {
            // More aggressive screen sharing checks
            String[] checkTerms = {
                    "sharing your screen", "screen sharing active", "is sharing", "presenting now",
                    "you are presenting", "screen is being shared", "cast screen", "casting screen"
            };

            for (String term : checkTerms) {
                Process process = Runtime.getRuntime().exec(
                        "tasklist /v | findstr /i \"" + term + "\"");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                if (reader.readLine() != null) {
                    SwingUtilities.invokeLater(() -> {
                        tableModel.addRow(new Object[]{
                                timeFormat.format(new Date()),
                                "ENHANCED_SCREEN_SHARING",
                                "Screen sharing detected: " + term
                        });
                        updateDetectorStatus("SCREEN_SHARING", "");
                    });
                    reader.close();
                    break;
                }
                reader.close();
            }

            // Check if any known screen sharing executables are running
            Process screenShareProcess = Runtime.getRuntime().exec(
                    "tasklist | findstr /i \"ScreenShare.exe ScreenCapture ScreenCast TeamViewer AnyDesk\"");
            BufferedReader screenShareReader = new BufferedReader(new InputStreamReader(screenShareProcess.getInputStream()));

            if (screenShareReader.readLine() != null) {
                SwingUtilities.invokeLater(() -> {
                    tableModel.addRow(new Object[]{
                            timeFormat.format(new Date()),
                            "ENHANCED_SCREEN_SHARING",
                            "Screen sharing application detected"
                    });
                    updateDetectorStatus("SCREEN_SHARING", "");
                });
            }
            screenShareReader.close();
        } catch (Exception e) {
            // Ignore errors
        }
    }

    private void checkForZoom() {
        try {
            Process process = Runtime.getRuntime().exec("tasklist | findstr /i \"Zoom.exe\"");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            int zoomCount = 0;
            while (reader.readLine() != null) {
                zoomCount++;
            }
            reader.close();

            if (zoomCount > 0) {
                final int finalZoomCount = zoomCount;
                SwingUtilities.invokeLater(() -> {
                    tableModel.addRow(new Object[]{
                            timeFormat.format(new Date()),
                            "ENHANCED_VIDEO_CALL",
                            "Zoom detected: " + finalZoomCount + " instance(s)"
                    });
                    updateDetectorStatus("VIDEO_CALL", "");
                });

                // Check for screen sharing in Zoom
                Process shareProcess = Runtime.getRuntime().exec(
                        "tasklist /v | findstr /i \"Zoom share screen sharing\"");
                BufferedReader shareReader = new BufferedReader(new InputStreamReader(shareProcess.getInputStream()));

                if (shareReader.readLine() != null) {
                    SwingUtilities.invokeLater(() -> {
                        tableModel.addRow(new Object[]{
                                timeFormat.format(new Date()),
                                "ENHANCED_SCREEN_SHARING",
                                "Screen sharing detected in Zoom"
                        });
                        updateDetectorStatus("SCREEN_SHARING", "");
                    });
                }
                shareReader.close();
            }
        } catch (Exception e) {
            // Ignore errors
        }
    }

    private void checkForTeams() {
        try {
            Process process = Runtime.getRuntime().exec("tasklist | findstr /i \"Teams.exe\"");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            int teamsCount = 0;
            while (reader.readLine() != null) {
                teamsCount++;
            }
            reader.close();

            if (teamsCount > 0) {
                final int finalTeamsCount = teamsCount;
                SwingUtilities.invokeLater(() -> {
                    tableModel.addRow(new Object[]{
                            timeFormat.format(new Date()),
                            "ENHANCED_VIDEO_CALL",
                            "Microsoft Teams detected: " + finalTeamsCount + " instance(s)"
                    });
                    updateDetectorStatus("VIDEO_CALL", "");
                });

                // Check for screen sharing in Teams
                Process shareProcess = Runtime.getRuntime().exec(
                        "tasklist /v | findstr /i \"Teams sharing presenting\"");
                BufferedReader shareReader = new BufferedReader(new InputStreamReader(shareProcess.getInputStream()));

                if (shareReader.readLine() != null) {
                    SwingUtilities.invokeLater(() -> {
                        tableModel.addRow(new Object[]{
                                timeFormat.format(new Date()),
                                "ENHANCED_SCREEN_SHARING",
                                "Screen sharing detected in Microsoft Teams"
                        });
                        updateDetectorStatus("SCREEN_SHARING", "");
                    });
                }
                shareReader.close();
            }
        } catch (Exception e) {
            // Ignore errors
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            new CheatDetectTester().setVisible(true);
        });
    }
}