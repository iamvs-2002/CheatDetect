package com.cheatdetect.api;

/**
 * Interface for receiving events from the CheatDetect library.
 * Events can be alerts, information, or debug messages.
 */
public interface EventListener {

    /**
     * Called when an event occurs.
     *
     * @param eventType the type of the event
     * @param details   details about the event
     */
    void onEvent(String eventType, String details);
}