package pl.pwr.transit.transit.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pl.pwr.transit.transit.application.NetworkQueryService;
import pl.pwr.transit.transit.application.RouteQueryService;
import pl.pwr.transit.transit.application.RouteShapeQueryService;
import pl.pwr.transit.transit.repository.RouteRepository;
import pl.pwr.transit.transit.web.dto.RouteResponse;
import pl.pwr.transit.transit.web.dto.RouteShapeResponse;
import pl.pwr.transit.transit.web.dto.StopResponse;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final RouteQueryService routeQueryService;
    private final RouteShapeQueryService routeShapeQueryService;
    private final NetworkQueryService networkQueryService;
    private final RouteRepository routeRepository;

    public RouteController(
            RouteQueryService routeQueryService,
            RouteShapeQueryService routeShapeQueryService,
            NetworkQueryService networkQueryService,
            RouteRepository routeRepository
    ) {
        this.routeQueryService = routeQueryService;
        this.routeShapeQueryService = routeShapeQueryService;
        this.networkQueryService = networkQueryService;
        this.routeRepository = routeRepository;
    }

    @GetMapping
    public List<RouteResponse> listRoutes() {
        return routeQueryService.findActiveRoutes().stream()
                .map(RouteResponse::from)
                .toList();
    }

    @GetMapping("/{routeId}/shapes")
    public List<RouteShapeResponse> routeShapes(@PathVariable String routeId) {
        if (!routeRepository.existsById(routeId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Route not found: " + routeId);
        }
        return routeShapeQueryService.findByRouteId(routeId).stream()
                .map(RouteShapeResponse::from)
                .toList();
    }

    @GetMapping("/{routeId}/stops")
    public List<StopResponse> routeStops(
            @PathVariable String routeId,
            @RequestParam short directionId
    ) {
        return networkQueryService.stopsForRouteDirection(routeId, directionId);
    }
}
