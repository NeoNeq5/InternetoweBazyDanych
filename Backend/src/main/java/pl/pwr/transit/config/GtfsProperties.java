package pl.pwr.transit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "transit.gtfs")
public record GtfsProperties(
        String url,
        boolean importOnStartup,
        String userAgent
) {
}
