package pl.pwr.transit.transit.service;

public record GtfsImportResult(
        Long importId,
        boolean skipped,
        int routeCount,
        int stopCount,
        String checksum
) {
}
