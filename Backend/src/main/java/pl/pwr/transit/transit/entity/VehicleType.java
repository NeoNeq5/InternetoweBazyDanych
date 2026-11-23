package pl.pwr.transit.transit.entity;

import java.util.Locale;

public enum VehicleType {
    BUS("bus"),
    TRAM("tram"),
    METRO("metro");

    private final String code;

    VehicleType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static VehicleType fromCode(String code) {
        if (code == null) {
            return BUS;
        }
        return switch (code.trim().toLowerCase(Locale.ROOT)) {
            case "tram" -> TRAM;
            case "metro" -> METRO;
            default -> BUS;
        };
    }
}
