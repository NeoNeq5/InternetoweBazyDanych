package pl.pwr.transit.transit.application;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.pwr.transit.transit.entity.Route;
import pl.pwr.transit.transit.entity.RouteStop;
import pl.pwr.transit.transit.entity.VehicleType;
import pl.pwr.transit.transit.repository.RouteRepository;
import pl.pwr.transit.transit.repository.RouteStopRepository;
import pl.pwr.transit.transit.repository.ServiceDateRepository;
import pl.pwr.transit.transit.repository.StopRepository;
import pl.pwr.transit.transit.repository.StopTimeRepository;
import pl.pwr.transit.transit.repository.TripRepository;
import pl.pwr.transit.transit.web.dto.DepartureResponse;
import pl.pwr.transit.transit.web.dto.StopResponse;
import pl.pwr.transit.transit.web.dto.StopRouteResponse;
import pl.pwr.transit.transit.web.dto.TripStopResponse;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class NetworkQueryService {

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");

    private final StopRepository stopRepository;
    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final ServiceDateRepository serviceDateRepository;
    private final StopTimeRepository stopTimeRepository;
    private final TripRepository tripRepository;

    public NetworkQueryService(
            StopRepository stopRepository,
            RouteRepository routeRepository,
            RouteStopRepository routeStopRepository,
            ServiceDateRepository serviceDateRepository,
            StopTimeRepository stopTimeRepository,
            TripRepository tripRepository
    ) {
        this.stopRepository = stopRepository;
        this.routeRepository = routeRepository;
        this.routeStopRepository = routeStopRepository;
        this.serviceDateRepository = serviceDateRepository;
        this.stopTimeRepository = stopTimeRepository;
        this.tripRepository = tripRepository;
    }

    @Transactional(readOnly = true)
    public List<StopRouteResponse> routesForStop(String stopId) {
        requireStop(stopId);
        List<RouteStop> links = routeStopRepository.findByStopIdOrderByRouteIdAscDirectionIdAsc(stopId);
        Map<String, List<RouteStop>> byRoute = new LinkedHashMap<>();
        for (RouteStop link : links) {
            byRoute.computeIfAbsent(link.getRouteId(), ignored -> new ArrayList<>()).add(link);
        }

        List<StopRouteResponse> result = new ArrayList<>();
        for (Map.Entry<String, List<RouteStop>> entry : byRoute.entrySet()) {
            Route route = routeRepository.findById(entry.getKey()).orElse(null);
            if (route == null || !route.isActive()) {
                continue;
            }
            List<StopRouteResponse.DirectionOption> directions = entry.getValue().stream()
                    .sorted(Comparator.comparing(RouteStop::getDirectionId))
                    .map(link -> new StopRouteResponse.DirectionOption(
                            link.getDirectionId(),
                            link.getHeadsign() == null || link.getHeadsign().isBlank()
                                    ? "Kierunek " + link.getDirectionId()
                                    : link.getHeadsign()
                    ))
                    .toList();
            result.add(new StopRouteResponse(
                    route.getId(),
                    route.getShortName(),
                    route.getLongName(),
                    route.getVehicleType(),
                    directions
            ));
        }
        result.sort(Comparator
                .comparing((StopRouteResponse item) -> item.vehicleType().name())
                .thenComparing(StopRouteResponse::shortName));
        return result;
    }

    @Transactional(readOnly = true)
    public List<StopResponse> stopsForRouteDirection(String routeId, short directionId) {
        if (!routeRepository.existsById(routeId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Route not found: " + routeId);
        }
        return routeStopRepository.findStopsByRouteAndDirection(routeId, directionId).stream()
                .map(StopResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DepartureResponse> upcomingDepartures(
            String stopId,
            String routeId,
            Short directionId,
            int limit
    ) {
        requireStop(stopId);
        boolean hasRoute = routeId != null && !routeId.isBlank();
        if (hasRoute && !routeRepository.existsById(routeId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Route not found: " + routeId);
        }

        ZonedDateTime now = ZonedDateTime.now(WARSAW);
        LocalDate today = now.toLocalDate();
        LocalDate serviceDay = resolveServiceDay(today);
        if (serviceDay == null) {
            return List.of();
        }
        List<String> serviceIds = serviceDateRepository.findServiceIdsByDate(serviceDay);
        if (serviceIds.isEmpty()) {
            return List.of();
        }

        // Use wall-clock "now" even when falling back to the nearest GTFS service day.
        Duration from = Duration.between(today.atStartOfDay(WARSAW), now);
        String fromLiteral = from.getSeconds() + " seconds";
        int clampedLimit = Math.min(Math.max(limit, 1), 30);

        List<Object[]> rows;
        if (hasRoute && directionId != null) {
            rows = stopTimeRepository.findUpcomingDepartures(
                    stopId,
                    routeId,
                    directionId,
                    serviceIds,
                    fromLiteral,
                    clampedLimit
            );
        } else if (hasRoute) {
            rows = stopTimeRepository.findUpcomingDeparturesForRoute(
                    stopId,
                    routeId,
                    serviceIds,
                    fromLiteral,
                    clampedLimit
            );
        } else {
            rows = stopTimeRepository.findUpcomingDeparturesForStop(
                    stopId,
                    serviceIds,
                    fromLiteral,
                    clampedLimit
            );
        }

        List<DepartureResponse> departures = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            Duration departure = toDuration(row[0]);
            String headsign = row[1] == null ? "" : String.valueOf(row[1]);
            String tripId = String.valueOf(row[2]);
            String rowRouteId = String.valueOf(row[3]);
            String routeShortName = String.valueOf(row[4]);
            VehicleType vehicleType = VehicleType.valueOf(String.valueOf(row[5]));
            short rowDirectionId = ((Number) row[6]).shortValue();
            long minutesUntil = Math.max(0, (departure.getSeconds() - from.getSeconds() + 59) / 60);
            departures.add(new DepartureResponse(
                    tripId,
                    rowRouteId,
                    routeShortName,
                    vehicleType,
                    rowDirectionId,
                    headsign,
                    formatClock(departure),
                    minutesUntil
            ));
        }
        return departures;
    }

    @Transactional(readOnly = true)
    public List<TripStopResponse> stopsForTrip(String tripId) {
        if (!tripRepository.existsById(tripId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found: " + tripId);
        }

        ZonedDateTime now = ZonedDateTime.now(WARSAW);
        LocalDate today = now.toLocalDate();
        Duration from = Duration.between(today.atStartOfDay(WARSAW), now);

        List<Object[]> rows = stopTimeRepository.findStopsForTrip(tripId);
        List<TripStopResponse> stops = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            int stopSequence = ((Number) row[0]).intValue();
            String stopId = String.valueOf(row[1]);
            String stopName = String.valueOf(row[2]);
            double lat = ((Number) row[3]).doubleValue();
            double lon = ((Number) row[4]).doubleValue();
            Duration arrival = toDuration(row[5]);
            Duration departure = toDuration(row[6]);
            long diffSeconds = arrival.getSeconds() - from.getSeconds();
            long minutesUntil = diffSeconds >= 0
                    ? (diffSeconds + 59) / 60
                    : Math.floorDiv(diffSeconds, 60);
            stops.add(new TripStopResponse(
                    stopSequence,
                    stopId,
                    stopName,
                    lat,
                    lon,
                    formatClock(arrival),
                    formatClock(departure),
                    minutesUntil
            ));
        }
        return stops;
    }

    private LocalDate resolveServiceDay(LocalDate today) {
        List<String> todayServices = serviceDateRepository.findServiceIdsByDate(today);
        if (!todayServices.isEmpty()) {
            return today;
        }
        LocalDate previous = serviceDateRepository.findLatestDateOnOrBefore(today);
        if (previous != null) {
            return previous;
        }
        return serviceDateRepository.findEarliestDateOnOrAfter(today);
    }

    private void requireStop(String stopId) {
        if (!stopRepository.existsById(stopId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stop not found: " + stopId);
        }
    }

    private static Duration toDuration(Object value) {
        if (value instanceof Duration duration) {
            return duration;
        }
        if (value instanceof Number number) {
            return Duration.ofSeconds(number.longValue());
        }
        // org.postgresql.util.PGInterval is runtime-only; avoid a compile-time dependency.
        if (value.getClass().getName().equals("org.postgresql.util.PGInterval")) {
            try {
                long days = ((Number) value.getClass().getMethod("getDays").invoke(value)).longValue();
                long hours = ((Number) value.getClass().getMethod("getHours").invoke(value)).longValue();
                long minutes = ((Number) value.getClass().getMethod("getMinutes").invoke(value)).longValue();
                long seconds = ((Number) value.getClass().getMethod("getSeconds").invoke(value)).longValue();
                return Duration.ofSeconds(days * 86_400L + hours * 3_600L + minutes * 60L + seconds);
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("Cannot read PGInterval", ex);
            }
        }
        String text = String.valueOf(value);
        if (text.contains(":")) {
            String[] parts = text.split(":");
            if (parts.length >= 3) {
                long hours = Long.parseLong(parts[0].replaceAll("[^0-9-]", ""));
                long minutes = Long.parseLong(parts[1]);
                long seconds = Long.parseLong(parts[2].replaceAll("[^0-9].*", ""));
                return Duration.ofSeconds(hours * 3600 + minutes * 60 + seconds);
            }
        }
        throw new IllegalStateException("Unsupported departure time type: " + value.getClass());
    }

    private static String formatClock(Duration duration) {
        long total = duration.getSeconds();
        long hours = (total / 3600) % 24;
        long minutes = (total % 3600) / 60;
        return String.format("%02d:%02d", hours, minutes);
    }
}
