package pl.pwr.transit.transit.gtfs;

import java.util.List;

public record ParsedGtfsFeed(
        List<ParsedRoute> routes,
        List<ParsedStop> stops,
        List<ParsedRouteShape> shapes,
        List<ParsedTrip> trips,
        List<ParsedRouteStop> routeStops,
        List<ParsedServiceDate> serviceDates
) {
}
