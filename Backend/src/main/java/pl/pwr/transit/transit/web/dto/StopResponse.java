package pl.pwr.transit.transit.web.dto;

import pl.pwr.transit.transit.entity.Stop;

public record StopResponse(
        String id,
        String name,
        double lat,
        double lon
) {
    public static StopResponse from(Stop stop) {
        return new StopResponse(stop.getId(), stop.getName(), stop.getLat(), stop.getLon());
    }
}
