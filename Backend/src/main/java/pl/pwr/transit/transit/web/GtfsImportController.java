package pl.pwr.transit.transit.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.pwr.transit.transit.application.GtfsImportService;
import pl.pwr.transit.transit.web.dto.GtfsImportResponse;

@RestController
@RequestMapping("/api/admin/gtfs")
public class GtfsImportController {

    private final GtfsImportService gtfsImportService;

    public GtfsImportController(GtfsImportService gtfsImportService) {
        this.gtfsImportService = gtfsImportService;
    }

    @PostMapping("/import")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public GtfsImportResponse importFeed() {
        return GtfsImportResponse.from(gtfsImportService.importFeed());
    }
}
