package pl.pwr.transit.transit.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.pwr.transit.transit.service.TransitQueryService;
import pl.pwr.transit.transit.web.dto.RouteResponse;
import pl.pwr.transit.transit.web.dto.RouteShapeResponse;
import pl.pwr.transit.transit.web.dto.StopResponse;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final TransitQueryService transitQueryService;

    public RouteController(TransitQueryService transitQueryService) {
        this.transitQueryService = transitQueryService;
    }

    @GetMapping
    public List<RouteResponse> listRoutes() {
        return transitQueryService.findActiveRoutes().stream()
                .map(RouteResponse::from)
                .toList();
    }

    @GetMapping("/{routeId}/shapes")
    public List<RouteShapeResponse> routeShapes(@PathVariable String routeId) {
        return transitQueryService.routeShapes(routeId);
    }

    @GetMapping("/{routeId}/stops")
    public List<StopResponse> routeStops(
            @PathVariable String routeId,
            @RequestParam short directionId
    ) {
        return transitQueryService.stopsForRouteDirection(routeId, directionId);
    }
}
