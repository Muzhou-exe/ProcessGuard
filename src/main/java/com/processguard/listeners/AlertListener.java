package com.processguard.listeners;

import com.processguard.models.AlertEvent;

/**
 * Listener interface for alert events.
 * AlertEngine notifies implementations of this interface.
 * As defined in section 2.3 of the SDD (Interface Segregation).
 *
 * Single minimal method only.
 */
public interface AlertListener {

    /**
     * Called when a new alert is generated.
     */
    void onAlert(AlertEvent alert);
}