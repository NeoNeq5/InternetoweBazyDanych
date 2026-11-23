package pl.pwr.transit.transit.gtfs;

public record ParsedTrip(
        String id,
        String routeId,
        String serviceId,
        short directionId,
        String headsign
) {
}
