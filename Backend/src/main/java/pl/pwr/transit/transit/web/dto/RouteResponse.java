package pl.pwr.transit.transit.web.dto;

import pl.pwr.transit.transit.entity.Route;
import pl.pwr.transit.transit.entity.VehicleType;

public record RouteResponse(
        String id,
        String shortName,
        String longName,
        VehicleType vehicleType
) {
    public static RouteResponse from(Route route) {
        return new RouteResponse(
                route.getId(),
                route.getShortName(),
                route.getLongName(),
                route.getVehicleType()
        );
    }
}
