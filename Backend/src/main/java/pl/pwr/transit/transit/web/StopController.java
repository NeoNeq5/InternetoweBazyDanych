package pl.pwr.transit.transit.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.pwr.transit.transit.application.NetworkQueryService;
import pl.pwr.transit.transit.application.StopQueryService;
import pl.pwr.transit.transit.web.dto.DepartureResponse;
import pl.pwr.transit.transit.web.dto.StopResponse;
import pl.pwr.transit.transit.web.dto.StopRouteResponse;

import java.util.List;

@RestController
@RequestMapping("/api/stops")
public class StopController {

    private final StopQueryService stopQueryService;
    private final NetworkQueryService networkQueryService;

    public StopController(StopQueryService stopQueryService, NetworkQueryService networkQueryService) {
        this.stopQueryService = stopQueryService;
        this.networkQueryService = networkQueryService;
    }

    @GetMapping
    public List<StopResponse> listStops() {
        return stopQueryService.findActiveStops().stream()
                .map(StopResponse::from)
                .toList();
    }

    @GetMapping("/{stopId}/routes")
    public List<StopRouteResponse> routesForStop(@PathVariable String stopId) {
        return networkQueryService.routesForStop(stopId);
    }

    @GetMapping("/{stopId}/departures")
    public List<DepartureResponse> departures(
            @PathVariable String stopId,
            @RequestParam String routeId,
            @RequestParam short directionId,
            @RequestParam(defaultValue = "12") int limit
    ) {
        return networkQueryService.upcomingDepartures(stopId, routeId, directionId, limit);
    }
}
