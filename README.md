# CheatDetect

A lightweight, open-source Java library designed to detect cheating in coding interviews by monitoring system activities in real-time.

## Features

- **LeetCode Detection**: Monitor active windows to detect coding platforms
- **System Tray Monitoring**: Detect hidden applications
- **Video Call Detection**: Identify parallel video calls
- **Screen Sharing Detection**: Detect unauthorized screen sharing
- **Process Monitoring**: Track running applications for unauthorized tools
- **Clipboard Monitoring**: Identify copy-paste activity from external sources
- **Multi-Device Detection**: Detect device switching during interviews
- **Browser Monitoring**: Track multiple browsers and excessive tab usage
- **AI Tool Detection**: Identify usage of AI assistants during interviews

## Requirements

- Java 11 or higher
- Supports Windows, macOS, and Linux

## Installation

### Maven

```xml
<dependency>
    <groupId>com.cheatdetect</groupId>
    <artifactId>cheatdetect</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.cheatdetect:cheatdetect:1.0.0'
```

## Quick Start

Using the core library directly:

```java
import com.cheatdetect.core.CheatDetect;
import com.cheatdetect.core.Configuration;
import com.cheatdetect.api.AlertCallback;

public class Demo {
    public static void main(String[] args) {
        // Configure CheatDetect
        Configuration config = new Configuration.Builder()
            .enableProcessMonitoring(true)
            .enableClipboardMonitoring(true)
            .enableLeetCodeDetection(true)
            .enableScreenShareDetection(true)
            .enableVideoCallDetection(true)
            .enableSystemTrayMonitoring(true)
            .enableDeviceSwitchDetection(true)
            .enableBrowserMonitoring(true)
            .build();
            
        // Initialize CheatDetect
        CheatDetect cheatDetect = new CheatDetect(config);
        
        // Register alert callback
        cheatDetect.registerAlertCallback(new AlertCallback() {
            @Override
            public void onAlert(String alertType, String details) {
                System.out.println("ALERT: " + alertType + " - " + details);
            }
        });
        
        // Start monitoring
        cheatDetect.start();
        
        // ... Interview session ...
        
        // Stop monitoring
        cheatDetect.stop();
    }
}
```

Alternatively, using the API for easier integration:

```java
import com.cheatdetect.api.CheatDetectAPI;
import com.cheatdetect.core.Configuration;
import com.cheatdetect.api.AlertCallback;

public class ApiDemo {
    public static void main(String[] args) {
        // Configure CheatDetect
        Configuration config = new Configuration.Builder()
            .enableProcessMonitoring(true)
            .enableClipboardMonitoring(true)
            .enableLeetCodeDetection(true)
            .enableBrowserMonitoring(true)
            .setScanInterval(2000) // Check every 2 seconds
            .build();
            
        // Create an instance
        String instanceId = CheatDetectAPI.createInstance(config);
        
        // Register alert callback
        CheatDetectAPI.registerAlertCallback(instanceId, (alertType, details) -> {
            System.out.println("ALERT: " + alertType + " - " + details);
        });
        
        // Start monitoring
        CheatDetectAPI.startMonitoring(instanceId);
        
        // ... Interview session ...
        
        // Stop monitoring
        CheatDetectAPI.stopMonitoring(instanceId);
        CheatDetectAPI.releaseInstance(instanceId);
    }
}
```

## Customization

CheatDetect is designed to be highly customizable. You can:

- Enable/disable specific detection modules
- Define custom detection rules
- Configure logging level and output
- Add custom alert handlers
- Set scan intervals to balance performance and detection speed

### Configuration Options

```java
Configuration config = new Configuration.Builder()
    // Enable/disable specific detectors
    .enableProcessMonitoring(true)
    .enableClipboardMonitoring(true)
    .enableLeetCodeDetection(true)
    .enableScreenShareDetection(true)
    .enableVideoCallDetection(true)
    .enableSystemTrayMonitoring(true)
    .enableDeviceSwitchDetection(true)
    .enableBrowserMonitoring(true)
    
    // Set scanning frequency
    .setScanInterval(2000) // milliseconds
    
    // Configure detailed logging
    .enableDetailedLogging(true)
    
    // Add custom items to monitor
    .addSuspiciousProcess("chatgpt.exe")
    .addPlatformIdentifier("codingplatform.com")
    .addVideoCallApplication("customvideoapp.exe")
    
    .build();
```

## API Documentation

For complete API documentation, see the [JavaDoc](https://cheatdetect.github.io/docs).

## Testing UI

A testing UI is included to help verify CheatDetect functionality:

```java
import com.cheatdetect.test.CheatDetectTester;

public class TestApp {
    public static void main(String[] args) {
        new CheatDetectTester().setVisible(true);
    }
}
```

The tester provides real-time visibility into detections and allows easy verification of all monitoring features.

## OS Support Notes

- **Windows**: Full feature support with native process and window monitoring
- **macOS**: Supported through AppleScript and native process monitoring
- **Linux**: Support varies by desktop environment, full support on X11-based systems

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## Sample
![image](https://github.com/user-attachments/assets/3807d688-ab18-4742-94a7-e16bb9de42b9)

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Security and Privacy Notes

CheatDetect is designed to balance security needs with privacy concerns:

- All monitoring is performed locally on the candidate's machine
- No data is transmitted externally unless explicitly configured
- Only relevant system events are monitored, not personal information
- All detected events are logged with timestamps for audit purposes

## Disclaimer

This library is intended for ethical use in supervised coding interview environments with proper disclosure to candidates. Use responsibly and in compliance with all applicable laws and regulations regarding monitoring and privacy.