package pl.pwr.transit.transit.web.dto;

import pl.pwr.transit.transit.entity.VehicleType;

public record DepartureResponse(
        String tripId,
        String routeId,
        String routeShortName,
        VehicleType vehicleType,
        short directionId,
        String headsign,
        String departureTime,
        long minutesUntil
) {
}
