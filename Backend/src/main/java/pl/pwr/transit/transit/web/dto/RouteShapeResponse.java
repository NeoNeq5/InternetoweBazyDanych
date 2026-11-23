package pl.pwr.transit.transit.web.dto;

import java.util.List;

public record RouteShapeResponse(
        short directionId,
        List<List<Double>> points
) {
}
