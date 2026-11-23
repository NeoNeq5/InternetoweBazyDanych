package pl.pwr.transit.transit.application.port;

/**
 * Port for downloading the raw GTFS archive from an external source.
 */
public interface GtfsFeedClient {

    byte[] downloadFeed();
}
