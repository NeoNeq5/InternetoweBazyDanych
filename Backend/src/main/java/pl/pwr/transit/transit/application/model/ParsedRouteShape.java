package pl.pwr.transit.transit.application.model;

import java.util.List;

public record ParsedRouteShape(
        String routeId,
        short directionId,
        List<LatLon> points
) {
}
