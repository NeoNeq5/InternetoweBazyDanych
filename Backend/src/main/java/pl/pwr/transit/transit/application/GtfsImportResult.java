package pl.pwr.transit.transit.application;

public record GtfsImportResult(
        Long importId,
        boolean skipped,
        int routeCount,
        int stopCount,
        String checksum
) {
}
