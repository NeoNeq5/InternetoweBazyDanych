package pl.pwr.transit.transit.gtfs;

public record ParsedStop(
        String id,
        String name,
        double lat,
        double lon,
        String parentStationId
) {
}
