package pl.pwr.transit.transit.application.model;

import java.time.LocalDate;

public record ParsedServiceDate(String serviceId, LocalDate date) {
}
