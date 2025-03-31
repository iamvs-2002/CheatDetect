package com.cheatdetect.api;

/**
 * Interface for receiving alerts from the CheatDetect library.
 */
public interface AlertCallback {

    /**
     * Called when an alert is triggered.
     *
     * @param alertType the type of the alert
     * @param details   details about the alert
     */
    void onAlert(String alertType, String details);
}