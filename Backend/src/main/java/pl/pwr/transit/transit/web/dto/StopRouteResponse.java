package pl.pwr.transit.transit.web.dto;

import pl.pwr.transit.transit.entity.VehicleType;

import java.util.List;

public record StopRouteResponse(
        String id,
        String shortName,
        String longName,
        VehicleType vehicleType,
        List<DirectionOption> directions
) {
    public record DirectionOption(short directionId, String headsign) {
    }
}
