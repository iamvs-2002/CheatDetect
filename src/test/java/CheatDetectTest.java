import com.cheatdetect.api.AlertCallback;
import com.cheatdetect.core.CheatDetect;
import com.cheatdetect.core.Configuration;

public class CheatDetectTest {
    public static void main(String[] args) {
        // Configure CheatDetect
        Configuration config = new Configuration.Builder()
                .enableProcessMonitoring(true)
                .enableClipboardMonitoring(true)
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

        // Keep the program running for testing
        try {
            Thread.sleep(60000); // Run for 1 minute
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Stop monitoring
        cheatDetect.stop();
    }
}