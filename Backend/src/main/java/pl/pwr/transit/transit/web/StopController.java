package pl.pwr.transit.transit.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.pwr.transit.transit.application.StopQueryService;
import pl.pwr.transit.transit.web.dto.StopResponse;

import java.util.List;

@RestController
@RequestMapping("/api/stops")
public class StopController {

    private final StopQueryService stopQueryService;

    public StopController(StopQueryService stopQueryService) {
        this.stopQueryService = stopQueryService;
    }

    @GetMapping
    public List<StopResponse> listStops() {
        return stopQueryService.findActiveStops().stream()
                .map(StopResponse::from)
                .toList();
    }
}
