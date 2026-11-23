package pl.pwr.transit.transit.gtfs;

import java.time.LocalDate;

public record ParsedServiceDate(String serviceId, LocalDate date) {
}
