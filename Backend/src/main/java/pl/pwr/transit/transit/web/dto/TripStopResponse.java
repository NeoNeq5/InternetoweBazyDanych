package pl.pwr.transit.transit.web.dto;

public record TripStopResponse(
        int stopSequence,
        String stopId,
        String stopName,
        double lat,
        double lon,
        String arrivalTime,
        String departureTime,
        long minutesUntil
) {
}
