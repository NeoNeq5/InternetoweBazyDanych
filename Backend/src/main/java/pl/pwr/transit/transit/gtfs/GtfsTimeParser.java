package pl.pwr.transit.transit.gtfs;

import java.time.Duration;

final class GtfsTimeParser {

    private GtfsTimeParser() {
    }

    static Duration parse(String value) {
        String[] parts = value.trim().split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid GTFS time: " + value);
        }
        long hours = Long.parseLong(parts[0]);
        long minutes = Long.parseLong(parts[1]);
        long seconds = Long.parseLong(parts[2]);
        return Duration.ofSeconds(hours * 3600 + minutes * 60 + seconds);
    }

    static String toIntervalLiteral(Duration duration) {
        return duration.getSeconds() + " seconds";
    }
}
