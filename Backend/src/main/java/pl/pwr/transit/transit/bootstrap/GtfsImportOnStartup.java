package pl.pwr.transit.transit.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import pl.pwr.transit.config.GtfsProperties;
import pl.pwr.transit.transit.application.GtfsImportService;

@Component
public class GtfsImportOnStartup implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GtfsImportOnStartup.class);

    private final GtfsProperties properties;
    private final GtfsImportService gtfsImportService;

    public GtfsImportOnStartup(GtfsProperties properties, GtfsImportService gtfsImportService) {
        this.properties = properties;
        this.gtfsImportService = gtfsImportService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.importOnStartup()) {
            return;
        }

        try {
            gtfsImportService.importFeed();
        } catch (RuntimeException ex) {
            log.error("Startup GTFS import failed; API will start with empty transit data", ex);
        }
    }
}
