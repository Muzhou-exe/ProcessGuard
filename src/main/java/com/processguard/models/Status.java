package com.processguard.models;

/**
 * Represents the classification status of a process in ProcessGuard.
 *
 * As defined in section 4.1 of the Software Design Document (SDD):
 * - NORMAL:        Process is considered safe and normal operation.
 * - SUSPICIOUS:    Process requires attention but is not yet critical.
 * - BLOCKED:       Process matches blacklist or violates critical rules.
 * - WHITELISTED:   Process is explicitly allowed by the user.
 */
public enum Status {
    NORMAL,
    SUSPICIOUS,
    BLOCKED,
    WHITELISTED
}