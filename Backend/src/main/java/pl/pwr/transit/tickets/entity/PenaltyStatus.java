package pl.pwr.transit.tickets.entity;

import jakarta.persistence.EnumeratedValue;

public enum PenaltyStatus {
    UNPAID("unpaid"),
    PAID("paid"),
    APPEALED("appealed"),
    CANCELLED("cancelled");

    @EnumeratedValue
    private final String dbValue;

    PenaltyStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }
}
