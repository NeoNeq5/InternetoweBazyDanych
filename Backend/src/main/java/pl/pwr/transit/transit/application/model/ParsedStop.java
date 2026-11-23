package pl.pwr.transit.transit.application.model;

public record ParsedStop(
        String id,
        String name,
        double lat,
        double lon,
        String parentStationId
) {
}
