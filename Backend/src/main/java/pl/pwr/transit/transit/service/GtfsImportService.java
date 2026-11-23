package pl.pwr.transit.transit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import pl.pwr.transit.transit.entity.GtfsImport;
import pl.pwr.transit.transit.entity.ImportStatus;
import pl.pwr.transit.transit.entity.Route;
import pl.pwr.transit.transit.entity.RouteShape;
import pl.pwr.transit.transit.entity.RouteStop;
import pl.pwr.transit.transit.entity.ServiceDate;
import pl.pwr.transit.transit.entity.ServiceDateId;
import pl.pwr.transit.transit.entity.Stop;
import pl.pwr.transit.transit.entity.Trip;
import pl.pwr.transit.transit.gtfs.HttpGtfsFeedClient;
import pl.pwr.transit.transit.gtfs.JdbcStopTimeImporter;
import pl.pwr.transit.transit.gtfs.LatLon;
import pl.pwr.transit.transit.gtfs.ParsedGtfsFeed;
import pl.pwr.transit.transit.gtfs.ParsedRoute;
import pl.pwr.transit.transit.gtfs.ParsedRouteShape;
import pl.pwr.transit.transit.gtfs.ParsedRouteStop;
import pl.pwr.transit.transit.gtfs.ParsedServiceDate;
import pl.pwr.transit.transit.gtfs.ParsedStop;
import pl.pwr.transit.transit.gtfs.ParsedTrip;
import pl.pwr.transit.transit.gtfs.ZipCsvGtfsFeedParser;
import pl.pwr.transit.transit.repository.GtfsImportRepository;
import pl.pwr.transit.transit.repository.RouteRepository;
import pl.pwr.transit.transit.repository.RouteShapeRepository;
import pl.pwr.transit.transit.repository.RouteStopRepository;
import pl.pwr.transit.transit.repository.ServiceDateRepository;
import pl.pwr.transit.transit.repository.StopRepository;
import pl.pwr.transit.transit.repository.TripRepository;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class GtfsImportService {

    private static final Logger log = LoggerFactory.getLogger(GtfsImportService.class);

    private final HttpGtfsFeedClient feedClient;
    private final ZipCsvGtfsFeedParser feedParser;
    private final GtfsImportRepository importRepository;
    private final RouteRepository routeRepository;
    private final StopRepository stopRepository;
    private final RouteShapeRepository routeShapeRepository;
    private final TripRepository tripRepository;
    private final RouteStopRepository routeStopRepository;
    private final ServiceDateRepository serviceDateRepository;
    private final JdbcStopTimeImporter stopTimeImporter;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    public GtfsImportService(
            HttpGtfsFeedClient feedClient,
            ZipCsvGtfsFeedParser feedParser,
            GtfsImportRepository importRepository,
            RouteRepository routeRepository,
            StopRepository stopRepository,
            RouteShapeRepository routeShapeRepository,
            TripRepository tripRepository,
            RouteStopRepository routeStopRepository,
            ServiceDateRepository serviceDateRepository,
            JdbcStopTimeImporter stopTimeImporter,
            TransactionTemplate transactionTemplate,
            ObjectMapper objectMapper
    ) {
        this.feedClient = feedClient;
        this.feedParser = feedParser;
        this.importRepository = importRepository;
        this.routeRepository = routeRepository;
        this.stopRepository = stopRepository;
        this.routeShapeRepository = routeShapeRepository;
        this.tripRepository = tripRepository;
        this.routeStopRepository = routeStopRepository;
        this.serviceDateRepository = serviceDateRepository;
        this.stopTimeImporter = stopTimeImporter;
        this.transactionTemplate = transactionTemplate;
        this.objectMapper = objectMapper;
    }

    public GtfsImportResult importFeed() {
        GtfsImport importRecord = new GtfsImport();
        importRecord.setFetchedAt(Instant.now());
        importRecord.setChecksum("pending");
        importRecord.setStatus(ImportStatus.IN_PROGRESS);
        GtfsImport currentImport = importRepository.save(importRecord);

        try {
            byte[] archive = feedClient.downloadFeed();
            String checksum = sha256(archive);
            currentImport.setChecksum(checksum);

            ParsedGtfsFeed feed = feedParser.parse(archive);
            int stopTimeCount = persistFeed(feed, archive, currentImport.getId(), checksum);

            log.info(
                    "GTFS import #{} saved {} routes, {} stops, {} shapes, {} trips, {} route-stops, {} service-dates, {} stop-times",
                    currentImport.getId(),
                    feed.routes().size(),
                    feed.stops().size(),
                    feed.shapes().size(),
                    feed.trips().size(),
                    feed.routeStops().size(),
                    feed.serviceDates().size(),
                    stopTimeCount
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

    private int persistFeed(ParsedGtfsFeed feed, byte[] archive, Long importId, String checksum) {
        Integer count = transactionTemplate.execute(status -> {
            GtfsImport managedImport = importRepository.findById(importId)
                    .orElseThrow(() -> new IllegalStateException("GTFS import record not found: " + importId));
            managedImport.setChecksum(checksum);

            persistRoutes(feed, managedImport);
            persistStops(feed, managedImport);
            persistShapes(feed, managedImport);
            stopTimeImporter.clear();
            persistTrips(feed, managedImport);
            persistRouteStops(feed);
            persistServiceDates(feed);
            stopRepository.flush();
            tripRepository.flush();
            int stopTimeCount = stopTimeImporter.importFromArchive(archive);

            managedImport.setStatus(ImportStatus.SUCCESS);
            managedImport.setErrorMessage(null);
            importRepository.save(managedImport);
            return stopTimeCount;
        });
        return count == null ? 0 : count;
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
            shape.setPointsJson(serializePoints(parsed.points()));
            shape.setLastImport(importRecord);
            routeShapeRepository.save(shape);
        }
    }

    private String serializePoints(List<LatLon> points) {
        List<double[]> coords = new ArrayList<>(points.size());
        for (LatLon point : points) {
            coords.add(new double[]{round6(point.lat()), round6(point.lon())});
        }
        return objectMapper.writeValueAsString(coords);
    }

    private static double round6(double value) {
        return Math.round(value * 1_000_000.0) / 1_000_000.0;
    }

    private void persistTrips(ParsedGtfsFeed feed, GtfsImport importRecord) {
        tripRepository.deleteAllTrips();
        Map<String, Route> routes = new HashMap<>();
        List<Trip> batch = new ArrayList<>(500);
        for (ParsedTrip parsed : feed.trips()) {
            Route route = routes.computeIfAbsent(
                    parsed.routeId(),
                    id -> routeRepository.findById(id).orElse(null)
            );
            if (route == null) {
                continue;
            }
            Trip trip = new Trip();
            trip.setId(parsed.id());
            trip.setRoute(route);
            trip.setServiceId(parsed.serviceId());
            trip.setDirectionId(parsed.directionId());
            trip.setHeadsign(parsed.headsign());
            trip.setActive(true);
            trip.setLastImport(importRecord);
            batch.add(trip);
            if (batch.size() >= 500) {
                tripRepository.saveAll(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            tripRepository.saveAll(batch);
        }
    }

    private void persistRouteStops(ParsedGtfsFeed feed) {
        routeStopRepository.deleteAllLinks();
        List<RouteStop> batch = new ArrayList<>(500);
        for (ParsedRouteStop parsed : feed.routeStops()) {
            if (!routeRepository.existsById(parsed.routeId()) || !stopRepository.existsById(parsed.stopId())) {
                continue;
            }
            RouteStop link = new RouteStop();
            link.setRouteId(parsed.routeId());
            link.setStopId(parsed.stopId());
            link.setDirectionId(parsed.directionId());
            link.setStopSequence(parsed.stopSequence());
            link.setHeadsign(parsed.headsign());
            batch.add(link);
            if (batch.size() >= 500) {
                routeStopRepository.saveAll(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            routeStopRepository.saveAll(batch);
        }
    }

    private void persistServiceDates(ParsedGtfsFeed feed) {
        serviceDateRepository.deleteAllServiceDates();
        List<ServiceDate> batch = new ArrayList<>(500);
        for (ParsedServiceDate parsed : feed.serviceDates()) {
            ServiceDate serviceDate = new ServiceDate();
            serviceDate.setId(new ServiceDateId(parsed.serviceId(), parsed.date()));
            batch.add(serviceDate);
            if (batch.size() >= 500) {
                serviceDateRepository.saveAll(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            serviceDateRepository.saveAll(batch);
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
