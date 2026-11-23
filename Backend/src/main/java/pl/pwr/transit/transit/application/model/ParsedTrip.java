package pl.pwr.transit.transit.application.model;

public record ParsedTrip(
        String id,
        String routeId,
        String serviceId,
        short directionId,
        String headsign
) {
}
