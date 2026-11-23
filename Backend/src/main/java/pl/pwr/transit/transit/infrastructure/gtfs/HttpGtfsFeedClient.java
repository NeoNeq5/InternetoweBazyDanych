package pl.pwr.transit.transit.infrastructure.gtfs;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import pl.pwr.transit.config.GtfsProperties;
import pl.pwr.transit.transit.application.port.GtfsFeedClient;

@Component
public class HttpGtfsFeedClient implements GtfsFeedClient {

    private final RestClient restClient;
    private final GtfsProperties properties;

    public HttpGtfsFeedClient(RestClient.Builder restClientBuilder, GtfsProperties properties) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    @Override
    public byte[] downloadFeed() {
        byte[] body = restClient.get()
                .uri(properties.url())
                .header("User-Agent", properties.userAgent())
                .accept(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL)
                .retrieve()
                .body(byte[].class);

        if (body == null || body.length == 0) {
            throw new IllegalStateException("GTFS feed download returned an empty body");
        }
        if (body.length < 4 || body[0] != 'P' || body[1] != 'K') {
            throw new IllegalStateException("GTFS feed download did not return a ZIP archive");
        }
        return body;
    }
}
