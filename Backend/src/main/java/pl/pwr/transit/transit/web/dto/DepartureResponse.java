package pl.pwr.transit.transit.web.dto;

public record DepartureResponse(
        String tripId,
        String headsign,
        String departureTime,
        long minutesUntil
) {
}
