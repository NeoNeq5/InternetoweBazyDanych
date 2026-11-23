package pl.pwr.transit.transit.application.model;

import pl.pwr.transit.transit.entity.VehicleType;

public record ParsedRoute(
        String id,
        String shortName,
        String longName,
        VehicleType vehicleType
) {
}
