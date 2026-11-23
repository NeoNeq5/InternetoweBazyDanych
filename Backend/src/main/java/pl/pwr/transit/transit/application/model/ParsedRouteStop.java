package pl.pwr.transit.transit.application.model;

public record ParsedRouteStop(
        String routeId,
        String stopId,
        short directionId,
        int stopSequence,
        String headsign
) {
}
