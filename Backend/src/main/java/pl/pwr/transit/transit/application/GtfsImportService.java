package pl.pwr.transit.transit.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pwr.transit.transit.application.model.ParsedGtfsFeed;
import pl.pwr.transit.transit.application.model.ParsedRoute;
import pl.pwr.transit.transit.application.model.ParsedRouteShape;
import pl.pwr.transit.transit.application.model.ParsedStop;
import pl.pwr.transit.transit.application.port.GtfsFeedClient;
import pl.pwr.transit.transit.application.port.GtfsFeedParser;
import pl.pwr.transit.transit.entity.GtfsImport;
import pl.pwr.transit.transit.entity.ImportStatus;
import pl.pwr.transit.transit.entity.Route;
import pl.pwr.transit.transit.entity.RouteShape;
import pl.pwr.transit.transit.entity.Stop;
import pl.pwr.transit.transit.repository.GtfsImportRepository;
import pl.pwr.transit.transit.repository.RouteRepository;
import pl.pwr.transit.transit.repository.RouteShapeRepository;
import pl.pwr.transit.transit.repository.StopRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;

@Service
public class GtfsImportService {

    private static final Logger log = LoggerFactory.getLogger(GtfsImportService.class);

    private final GtfsFeedClient feedClient;
    private final GtfsFeedParser feedParser;
    private final GtfsImportRepository importRepository;
    private final RouteRepository routeRepository;
    private final StopRepository stopRepository;
    private final RouteShapeRepository routeShapeRepository;

    public GtfsImportService(
            GtfsFeedClient feedClient,
            GtfsFeedParser feedParser,
            GtfsImportRepository importRepository,
            RouteRepository routeRepository,
            StopRepository stopRepository,
            RouteShapeRepository routeShapeRepository
    ) {
        this.feedClient = feedClient;
        this.feedParser = feedParser;
        this.importRepository = importRepository;
        this.routeRepository = routeRepository;
        this.stopRepository = stopRepository;
        this.routeShapeRepository = routeShapeRepository;
    }

    @Transactional
    public GtfsImportResult importFeed() {
        GtfsImport importRecord = new GtfsImport();
        importRecord.setFetchedAt(Instant.now());
        importRecord.setChecksum("pending");
        importRecord.setStatus(ImportStatus.IN_PROGRESS);
        final GtfsImport currentImport = importRepository.save(importRecord);

        try {
            byte[] archive = feedClient.downloadFeed();
            String checksum = sha256(archive);
            currentImport.setChecksum(checksum);

            ParsedGtfsFeed feed = feedParser.parse(archive);
            persistRoutes(feed, currentImport);
            persistStops(feed, currentImport);
            persistShapes(feed, currentImport);

            currentImport.setStatus(ImportStatus.SUCCESS);
            currentImport.setErrorMessage(null);
            importRepository.save(currentImport);

            log.info(
                    "GTFS import #{} saved {} routes, {} stops, {} shapes",
                    currentImport.getId(),
                    feed.routes().size(),
                    feed.stops().size(),
                    feed.shapes().size()
            );
            return new GtfsImportResult(
                    currentImport.getId(),
                    false,
                    feed.routes().size(),
                    feed.stops().size(),
                    checksum
            );
        } catch (RuntimeException ex) {
            currentImport.setStatus(ImportStatus.FAILED);
            currentImport.setErrorMessage(trimMessage(ex.getMessage()));
            importRepository.save(currentImport);
            log.error("GTFS import #{} failed", currentImport.getId(), ex);
            throw ex;
        }
    }

    private void persistRoutes(ParsedGtfsFeed feed, GtfsImport importRecord) {
        Set<String> ids = new HashSet<>();
        for (ParsedRoute parsed : feed.routes()) {
            ids.add(parsed.id());
            Route route = routeRepository.findById(parsed.id()).orElseGet(Route::new);
            route.setId(parsed.id());
            route.setShortName(parsed.shortName());
            route.setLongName(parsed.longName());
            route.setVehicleType(parsed.vehicleType());
            route.setActive(true);
            route.setLastImport(importRecord);
            routeRepository.save(route);
        }
        if (ids.isEmpty()) {
            routeRepository.deactivateAll();
        } else {
            routeRepository.deactivateMissing(ids);
        }
    }

    private void persistStops(ParsedGtfsFeed feed, GtfsImport importRecord) {
        Map<String, Stop> byId = new HashMap<>();

        for (ParsedStop parsed : feed.stops()) {
            Stop stop = stopRepository.findById(parsed.id()).orElseGet(Stop::new);
            stop.setId(parsed.id());
            stop.setName(parsed.name());
            stop.setLat(parsed.lat());
            stop.setLon(parsed.lon());
            stop.setActive(true);
            stop.setLastImport(importRecord);
            stop.setParentStation(null);
            byId.put(parsed.id(), stopRepository.save(stop));
        }

        for (ParsedStop parsed : feed.stops()) {
            if (parsed.parentStationId() == null) {
                continue;
            }
            Stop parent = byId.get(parsed.parentStationId());
            if (parent == null) {
                continue;
            }
            Stop child = byId.get(parsed.id());
            child.setParentStation(parent);
            stopRepository.save(child);
        }

        if (byId.isEmpty()) {
            stopRepository.deactivateAll();
        } else {
            stopRepository.deactivateMissing(byId.keySet());
        }
    }

    private void persistShapes(ParsedGtfsFeed feed, GtfsImport importRecord) {
        routeShapeRepository.deleteAllShapes();
        for (ParsedRouteShape parsed : feed.shapes()) {
            if (!routeRepository.existsById(parsed.routeId())) {
                continue;
            }
            RouteShape shape = new RouteShape();
            shape.setRouteId(parsed.routeId());
            shape.setDirectionId(parsed.directionId());
            shape.setPointsJson(ShapePointsCodec.write(parsed.points()));
            shape.setLastImport(importRecord);
            routeShapeRepository.save(shape);
        }
    }

    private static String sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private static String trimMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Unknown error";
        }
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= 1000) {
            return message;
        }
        return message.substring(0, Math.min(message.length(), 900));
    }
}
