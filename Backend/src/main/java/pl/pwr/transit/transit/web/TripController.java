package pl.pwr.transit.transit.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.pwr.transit.transit.service.TransitQueryService;
import pl.pwr.transit.transit.web.dto.TripStopResponse;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TransitQueryService transitQueryService;

    public TripController(TransitQueryService transitQueryService) {
        this.transitQueryService = transitQueryService;
    }

    @GetMapping("/{tripId}/stops")
    public List<TripStopResponse> stopsForTrip(@PathVariable String tripId) {
        return transitQueryService.stopsForTrip(tripId);
    }
}
