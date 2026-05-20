package com.att.tdp.issueflow.entity;

/**
 * Priority level of a {@link Ticket}.
 * <p>The escalation ladder follows the ordinal order: LOW → MEDIUM → HIGH → CRITICAL.</p>
 */
public enum TicketPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    /**
     * Returns the next higher priority, or the same value if already {@code CRITICAL}.
     */
    public TicketPriority escalate() {
        TicketPriority[] values = TicketPriority.values();
        int next = Math.min(this.ordinal() + 1, values.length - 1);
        return values[next];
    }
}
