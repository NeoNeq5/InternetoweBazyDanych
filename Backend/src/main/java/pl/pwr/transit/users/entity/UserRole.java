package pl.pwr.transit.users.entity;

import jakarta.persistence.EnumeratedValue;

public enum UserRole {
    PASSENGER("passenger"),
    INSPECTOR("inspector"),
    ADMIN("admin");

    @EnumeratedValue
    private final String dbValue;

    UserRole(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }
}
