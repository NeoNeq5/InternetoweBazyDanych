package pl.pwr.transit.transit.web.dto;

import pl.pwr.transit.transit.service.GtfsImportResult;

public record GtfsImportResponse(
        Long importId,
        boolean skipped,
        int routeCount,
        int stopCount,
        String checksum
) {
    public static GtfsImportResponse from(GtfsImportResult result) {
        return new GtfsImportResponse(
                result.importId(),
                result.skipped(),
                result.routeCount(),
                result.stopCount(),
                result.checksum()
        );
    }
}
