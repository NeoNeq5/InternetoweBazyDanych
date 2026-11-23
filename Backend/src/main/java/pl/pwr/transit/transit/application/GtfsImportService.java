package pl.pwr.transit.transit.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pwr.transit.transit.application.model.ParsedGtfsFeed;
import pl.pwr.transit.transit.application.model.ParsedRoute;
import pl.pwr.transit.transit.application.model.ParsedRouteShape;
import pl.pwr.transit.transit.application.model.ParsedRouteStop;
import pl.pwr.transit.transit.application.model.ParsedServiceDate;
import pl.pwr.transit.transit.application.model.ParsedStop;
import pl.pwr.transit.transit.application.model.ParsedTrip;
import pl.pwr.transit.transit.application.port.GtfsFeedClient;
import pl.pwr.transit.transit.application.port.GtfsFeedParser;
import pl.pwr.transit.transit.entity.GtfsImport;
import pl.pwr.transit.transit.entity.ImportStatus;
import pl.pwr.transit.transit.entity.Route;
import pl.pwr.transit.transit.entity.RouteShape;
import pl.pwr.transit.transit.entity.RouteStop;
import pl.pwr.transit.transit.entity.ServiceDate;
import pl.pwr.transit.transit.entity.ServiceDateId;
import pl.pwr.transit.transit.entity.Stop;
import pl.pwr.transit.transit.entity.Trip;
import pl.pwr.transit.transit.infrastructure.gtfs.JdbcStopTimeImporter;
import pl.pwr.transit.transit.repository.GtfsImportRepository;
import pl.pwr.transit.transit.repository.RouteRepository;
import pl.pwr.transit.transit.repository.RouteShapeRepository;
import pl.pwr.transit.transit.repository.RouteStopRepository;
import pl.pwr.transit.transit.repository.ServiceDateRepository;
import pl.pwr.transit.transit.repository.StopRepository;
import pl.pwr.transit.transit.repository.TripRepository;

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
    private final TripRepository tripRepository;
    private final RouteStopRepository routeStopRepository;
    private final ServiceDateRepository serviceDateRepository;
    private final JdbcStopTimeImporter stopTimeImporter;

    public GtfsImportService(
            GtfsFeedClient feedClient,
            GtfsFeedParser feedParser,
            GtfsImportRepository importRepository,
            RouteRepository routeRepository,
            StopRepository stopRepository,
            RouteShapeRepository routeShapeRepository,
            TripRepository tripRepository,
            RouteStopRepository routeStopRepository,
            ServiceDateRepository serviceDateRepository,
            JdbcStopTimeImporter stopTimeImporter
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
            stopTimeImporter.clear();
            persistTrips(feed, currentImport);
            persistRouteStops(feed);
            persistServiceDates(feed);
            int stopTimeCount = stopTimeImporter.importFromArchive(archive);

            currentImport.setStatus(ImportStatus.SUCCESS);
            currentImport.setErrorMessage(null);
            importRepository.save(currentImport);

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

    private void persistTrips(ParsedGtfsFeed feed, GtfsImport importRecord) {
        tripRepository.deleteAllTrips();
        Map<String, Route> routes = new HashMap<>();
        java.util.ArrayList<Trip> batch = new java.util.ArrayList<>(500);
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
        java.util.ArrayList<RouteStop> batch = new java.util.ArrayList<>(500);
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
        java.util.ArrayList<ServiceDate> batch = new java.util.ArrayList<>(500);
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
