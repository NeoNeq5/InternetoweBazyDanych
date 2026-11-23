package pl.pwr.transit.transit.gtfs;

public record ParsedRouteStop(
        String routeId,
        String stopId,
        short directionId,
        int stopSequence,
        String headsign
) {
}
