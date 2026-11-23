package pl.pwr.transit.transit.entity;

import jakarta.persistence.EnumeratedValue;

public enum ImportStatus {
    IN_PROGRESS("in_progress"),
    SUCCESS("success"),
    FAILED("failed");

    @EnumeratedValue
    private final String dbValue;

    ImportStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }
}
