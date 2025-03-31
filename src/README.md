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

## Customization

CheatDetect is designed to be highly customizable. You can:

- Enable/disable specific detection modules
- Define custom detection rules
- Configure logging level and output
- Add custom alert handlers

## API Documentation

For complete API documentation, see the [JavaDoc](https://cheatdetect.github.io/docs).

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

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