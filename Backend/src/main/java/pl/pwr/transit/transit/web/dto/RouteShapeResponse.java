package pl.pwr.transit.transit.web.dto;

import pl.pwr.transit.transit.application.ShapePointsCodec;
import pl.pwr.transit.transit.entity.RouteShape;

import java.util.List;

public record RouteShapeResponse(
        short directionId,
        List<List<Double>> points
) {
    public static RouteShapeResponse from(RouteShape shape) {
        return new RouteShapeResponse(
                shape.getDirectionId(),
                ShapePointsCodec.read(shape.getPointsJson())
        );
    }
}
