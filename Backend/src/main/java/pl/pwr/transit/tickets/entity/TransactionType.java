package pl.pwr.transit.tickets.entity;

import jakarta.persistence.EnumeratedValue;

public enum TransactionType {
    TOP_UP("top_up"),
    TICKET_PURCHASE("ticket_purchase"),
    REFUND("refund"),
    PENALTY_PAYMENT("penalty_payment");

    @EnumeratedValue
    private final String dbValue;

    TransactionType(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }
}
