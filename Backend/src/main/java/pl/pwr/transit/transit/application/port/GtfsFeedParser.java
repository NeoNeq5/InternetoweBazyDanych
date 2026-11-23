package pl.pwr.transit.transit.application.port;

import pl.pwr.transit.transit.application.model.ParsedGtfsFeed;

/**
 * Port for turning a GTFS ZIP archive into domain-oriented records.
 */
public interface GtfsFeedParser {

    ParsedGtfsFeed parse(byte[] archiveBytes);
}
