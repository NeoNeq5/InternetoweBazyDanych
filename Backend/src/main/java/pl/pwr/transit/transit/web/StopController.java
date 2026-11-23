package pl.pwr.transit.transit.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.pwr.transit.transit.service.TransitQueryService;
import pl.pwr.transit.transit.web.dto.DepartureResponse;
import pl.pwr.transit.transit.web.dto.StopResponse;
import pl.pwr.transit.transit.web.dto.StopRouteResponse;

import java.util.List;

@RestController
@RequestMapping("/api/stops")
public class StopController {

    private final TransitQueryService transitQueryService;

    public StopController(TransitQueryService transitQueryService) {
        this.transitQueryService = transitQueryService;
    }

    @GetMapping
    public List<StopResponse> listStops() {
        return transitQueryService.findActiveStops().stream()
                .map(StopResponse::from)
                .toList();
    }

    @GetMapping("/{stopId}/routes")
    public List<StopRouteResponse> routesForStop(@PathVariable String stopId) {
        return transitQueryService.routesForStop(stopId);
    }

    @GetMapping("/{stopId}/departures")
    public List<DepartureResponse> departures(
            @PathVariable String stopId,
            @RequestParam(required = false) String routeId,
            @RequestParam(required = false) Short directionId,
            @RequestParam(defaultValue = "12") int limit
    ) {
        return transitQueryService.upcomingDepartures(stopId, routeId, directionId, limit);
    }
}
