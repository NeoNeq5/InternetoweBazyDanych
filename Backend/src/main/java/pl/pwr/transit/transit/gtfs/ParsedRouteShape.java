package pl.pwr.transit.transit.gtfs;

import java.util.List;

public record ParsedRouteShape(
        String routeId,
        short directionId,
        List<LatLon> points
) {
}
