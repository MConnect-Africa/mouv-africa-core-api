
package org.core.backend.models;

/**
 * The status enum.
 */
public enum Status {

    /**
     * The active status.
     */
    ACTIVE,

    /**
     * The pending status.
     */
    PENDING,

    /**
     * The disabled status.
     */
    DISABLED,

    /** Checked out booking status. */
    CHECKED_OUT,

    /** Checked in booking status. */
    CHECKED_IN,

    /** cleaning out booking status. */
    CLEANING,

    /** suspended out booking status. */
    SUSPENDED,

    /** booked booking status. */
    BOOKED
}
