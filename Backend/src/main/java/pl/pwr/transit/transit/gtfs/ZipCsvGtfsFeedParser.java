package pl.pwr.transit.transit.gtfs;

import org.springframework.stereotype.Component;
import pl.pwr.transit.transit.entity.VehicleType;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class ZipCsvGtfsFeedParser {

    private static final int MAX_SHAPE_POINTS = 400;
    private static final DateTimeFormatter GTFS_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    public ParsedGtfsFeed parse(byte[] archiveBytes) {
        Map<String, byte[]> files = readEntries(archiveBytes);
        byte[] tripsContent = requireFile(files, "trips.txt");
        List<ParsedTrip> trips = parseTrips(tripsContent);
        Map<String, ParsedTrip> tripsById = new HashMap<>(trips.size() * 2);
        for (ParsedTrip trip : trips) {
            tripsById.put(trip.id(), trip);
        }

        return new ParsedGtfsFeed(
                List.copyOf(parseRoutes(requireFile(files, "routes.txt"))),
                List.copyOf(parseStops(requireFile(files, "stops.txt"))),
                List.copyOf(parseRouteShapes(requireFile(files, "shapes.txt"), tripsContent)),
                List.copyOf(trips),
                List.copyOf(parseRouteStops(requireFile(files, "stop_times.txt"), tripsById)),
                List.copyOf(parseServiceDates(
                        files.get("calendar.txt"),
                        files.get("calendar_dates.txt")
                ))
        );
    }

    private static Map<String, byte[]> readEntries(byte[] archiveBytes) {
        Map<String, byte[]> files = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archiveBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                int slash = name.lastIndexOf('/');
                if (slash >= 0) {
                    name = name.substring(slash + 1);
                }
                files.put(name.toLowerCase(), zip.readAllBytes());
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read GTFS ZIP archive", ex);
        }
        return files;
    }

    private static byte[] requireFile(Map<String, byte[]> files, String name) {
        byte[] content = files.get(name.toLowerCase());
        if (content == null) {
            throw new IllegalStateException("GTFS archive is missing " + name);
        }
        return content;
    }

    private static List<ParsedRoute> parseRoutes(byte[] content) {
        List<ParsedRoute> routes = new ArrayList<>();
        try (BufferedReader reader = reader(content)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return routes;
            }
            Map<String, Integer> index = GtfsCsv.headerIndex(GtfsCsv.parseLine(stripBom(headerLine)));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> fields = GtfsCsv.parseLine(line);
                String id = GtfsCsv.required(fields, index, "route_id");
                String shortName = GtfsCsv.optional(fields, index, "route_short_name");
                if (shortName == null) {
                    shortName = id;
                }
                String longNameRaw = firstNonBlank(
                        GtfsCsv.optional(fields, index, "route_long_name"),
                        GtfsCsv.optional(fields, index, "route_desc")
                );
                String longName = summarizeLongName(longNameRaw, shortName);
                int routeType = Integer.parseInt(GtfsCsv.required(fields, index, "route_type"));
                routes.add(new ParsedRoute(id, shortName, longName, toVehicleType(routeType)));
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to parse routes.txt", ex);
        }
        return routes;
    }

    private static List<ParsedStop> parseStops(byte[] content) {
        List<ParsedStop> stops = new ArrayList<>();
        try (BufferedReader reader = reader(content)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return stops;
            }
            Map<String, Integer> index = GtfsCsv.headerIndex(GtfsCsv.parseLine(stripBom(headerLine)));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> fields = GtfsCsv.parseLine(line);
                String id = GtfsCsv.required(fields, index, "stop_id");
                String name = GtfsCsv.required(fields, index, "stop_name");
                double lat = Double.parseDouble(GtfsCsv.required(fields, index, "stop_lat"));
                double lon = Double.parseDouble(GtfsCsv.required(fields, index, "stop_lon"));
                String parent = GtfsCsv.optional(fields, index, "parent_station");
                stops.add(new ParsedStop(id, name, lat, lon, parent));
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to parse stops.txt", ex);
        }
        return stops;
    }

    private static List<ParsedTrip> parseTrips(byte[] content) {
        List<ParsedTrip> trips = new ArrayList<>();
        try (BufferedReader reader = reader(content)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return trips;
            }
            Map<String, Integer> index = GtfsCsv.headerIndex(GtfsCsv.parseLine(stripBom(headerLine)));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> fields = GtfsCsv.parseLine(line);
                String directionRaw = GtfsCsv.optional(fields, index, "direction_id");
                short directionId = directionRaw == null ? 0 : Short.parseShort(directionRaw);
                trips.add(new ParsedTrip(
                        GtfsCsv.required(fields, index, "trip_id"),
                        GtfsCsv.required(fields, index, "route_id"),
                        GtfsCsv.required(fields, index, "service_id"),
                        directionId,
                        GtfsCsv.optional(fields, index, "trip_headsign")
                ));
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to parse trips.txt", ex);
        }
        return trips;
    }

    private static List<ParsedRouteStop> parseRouteStops(byte[] stopTimesContent, Map<String, ParsedTrip> tripsById) {
        Map<String, ParsedRouteStop> links = new LinkedHashMap<>();
        try (BufferedReader reader = reader(stopTimesContent)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return List.of();
            }
            Map<String, Integer> index = GtfsCsv.headerIndex(GtfsCsv.parseLine(stripBom(headerLine)));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> fields = GtfsCsv.parseLine(line);
                String tripId = GtfsCsv.required(fields, index, "trip_id");
                ParsedTrip trip = tripsById.get(tripId);
                if (trip == null) {
                    continue;
                }
                String stopId = GtfsCsv.required(fields, index, "stop_id");
                int sequence = Integer.parseInt(GtfsCsv.required(fields, index, "stop_sequence"));
                String key = trip.routeId() + "|" + stopId + "|" + trip.directionId();
                ParsedRouteStop existing = links.get(key);
                if (existing == null || sequence < existing.stopSequence()) {
                    links.put(key, new ParsedRouteStop(
                            trip.routeId(),
                            stopId,
                            trip.directionId(),
                            sequence,
                            trip.headsign()
                    ));
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to parse stop_times.txt for route-stop links", ex);
        }
        return new ArrayList<>(links.values());
    }

    private static List<ParsedServiceDate> parseServiceDates(byte[] calendarContent, byte[] calendarDatesContent) {
        Set<String> dates = new HashSet<>();
        Map<String, boolean[]> weekMask = new HashMap<>();
        Map<String, LocalDate> starts = new HashMap<>();
        Map<String, LocalDate> ends = new HashMap<>();

        if (calendarContent != null) {
            try (BufferedReader reader = reader(calendarContent)) {
                String headerLine = reader.readLine();
                if (headerLine != null) {
                    Map<String, Integer> index = GtfsCsv.headerIndex(GtfsCsv.parseLine(stripBom(headerLine)));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.isBlank()) {
                            continue;
                        }
                        List<String> fields = GtfsCsv.parseLine(line);
                        String serviceId = GtfsCsv.required(fields, index, "service_id");
                        boolean[] mask = {
                                "1".equals(GtfsCsv.required(fields, index, "monday")),
                                "1".equals(GtfsCsv.required(fields, index, "tuesday")),
                                "1".equals(GtfsCsv.required(fields, index, "wednesday")),
                                "1".equals(GtfsCsv.required(fields, index, "thursday")),
                                "1".equals(GtfsCsv.required(fields, index, "friday")),
                                "1".equals(GtfsCsv.required(fields, index, "saturday")),
                                "1".equals(GtfsCsv.required(fields, index, "sunday"))
                        };
                        weekMask.put(serviceId, mask);
                        starts.put(serviceId, LocalDate.parse(GtfsCsv.required(fields, index, "start_date"), GTFS_DATE));
                        ends.put(serviceId, LocalDate.parse(GtfsCsv.required(fields, index, "end_date"), GTFS_DATE));
                    }
                }
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to parse calendar.txt", ex);
            }
        }

        for (Map.Entry<String, boolean[]> entry : weekMask.entrySet()) {
            String serviceId = entry.getKey();
            LocalDate cursor = starts.get(serviceId);
            LocalDate end = ends.get(serviceId);
            boolean[] mask = entry.getValue();
            while (!cursor.isAfter(end)) {
                if (mask[dayIndex(cursor.getDayOfWeek())]) {
                    dates.add(serviceId + "|" + cursor);
                }
                cursor = cursor.plusDays(1);
            }
        }

        if (calendarDatesContent != null) {
            try (BufferedReader reader = reader(calendarDatesContent)) {
                String headerLine = reader.readLine();
                if (headerLine != null) {
                    Map<String, Integer> index = GtfsCsv.headerIndex(GtfsCsv.parseLine(stripBom(headerLine)));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.isBlank()) {
                            continue;
                        }
                        List<String> fields = GtfsCsv.parseLine(line);
                        String serviceId = GtfsCsv.required(fields, index, "service_id");
                        LocalDate date = LocalDate.parse(GtfsCsv.required(fields, index, "date"), GTFS_DATE);
                        int exceptionType = Integer.parseInt(GtfsCsv.required(fields, index, "exception_type"));
                        String key = serviceId + "|" + date;
                        if (exceptionType == 1) {
                            dates.add(key);
                        } else if (exceptionType == 2) {
                            dates.remove(key);
                        }
                    }
                }
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to parse calendar_dates.txt", ex);
            }
        }

        List<ParsedServiceDate> result = new ArrayList<>(dates.size());
        for (String key : dates) {
            int sep = key.indexOf('|');
            result.add(new ParsedServiceDate(key.substring(0, sep), LocalDate.parse(key.substring(sep + 1))));
        }
        return result;
    }

    private static int dayIndex(DayOfWeek day) {
        return day.getValue() - 1;
    }

    private static List<ParsedRouteShape> parseRouteShapes(byte[] shapesContent, byte[] tripsContent) {
        Map<String, List<LatLon>> shapesById = parseShapes(shapesContent);
        Map<String, String> shapeIdByRouteDirection = pickShapeIds(tripsContent);

        List<ParsedRouteShape> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : shapeIdByRouteDirection.entrySet()) {
            String key = entry.getKey();
            int sep = key.lastIndexOf('|');
            String routeId = key.substring(0, sep);
            short directionId = Short.parseShort(key.substring(sep + 1));
            List<LatLon> points = shapesById.get(entry.getValue());
            if (points == null || points.size() < 2) {
                continue;
            }
            result.add(new ParsedRouteShape(routeId, directionId, simplify(points, MAX_SHAPE_POINTS)));
        }
        return result;
    }

    private static Map<String, List<LatLon>> parseShapes(byte[] content) {
        Map<String, List<ShapePoint>> buffered = new HashMap<>();
        try (BufferedReader reader = reader(content)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return Map.of();
            }
            Map<String, Integer> index = GtfsCsv.headerIndex(GtfsCsv.parseLine(stripBom(headerLine)));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> fields = GtfsCsv.parseLine(line);
                String shapeId = GtfsCsv.required(fields, index, "shape_id");
                double lat = Double.parseDouble(GtfsCsv.required(fields, index, "shape_pt_lat"));
                double lon = Double.parseDouble(GtfsCsv.required(fields, index, "shape_pt_lon"));
                int sequence = Integer.parseInt(GtfsCsv.required(fields, index, "shape_pt_sequence"));
                buffered.computeIfAbsent(shapeId, ignored -> new ArrayList<>())
                        .add(new ShapePoint(sequence, new LatLon(lat, lon)));
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to parse shapes.txt", ex);
        }

        Map<String, List<LatLon>> shapes = new HashMap<>(buffered.size());
        for (Map.Entry<String, List<ShapePoint>> entry : buffered.entrySet()) {
            List<ShapePoint> points = entry.getValue();
            points.sort(Comparator.comparingInt(ShapePoint::sequence));
            List<LatLon> coords = new ArrayList<>(points.size());
            for (ShapePoint point : points) {
                coords.add(point.coord());
            }
            shapes.put(entry.getKey(), coords);
        }
        return shapes;
    }

    private record ShapePoint(int sequence, LatLon coord) {
    }

    private static Map<String, String> pickShapeIds(byte[] tripsContent) {
        Map<String, String> chosen = new LinkedHashMap<>();
        try (BufferedReader reader = reader(tripsContent)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return chosen;
            }
            Map<String, Integer> index = GtfsCsv.headerIndex(GtfsCsv.parseLine(stripBom(headerLine)));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> fields = GtfsCsv.parseLine(line);
                String routeId = GtfsCsv.required(fields, index, "route_id");
                String shapeId = GtfsCsv.optional(fields, index, "shape_id");
                if (shapeId == null) {
                    continue;
                }
                String directionRaw = GtfsCsv.optional(fields, index, "direction_id");
                short directionId = directionRaw == null ? 0 : Short.parseShort(directionRaw);
                chosen.putIfAbsent(routeId + "|" + directionId, shapeId);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to parse trips.txt", ex);
        }
        return chosen;
    }

    static List<LatLon> simplify(List<LatLon> points, int maxPoints) {
        if (points.size() <= maxPoints) {
            return List.copyOf(points);
        }
        int step = (int) Math.ceil((double) (points.size() - 1) / (maxPoints - 1));
        List<LatLon> simplified = new ArrayList<>(maxPoints);
        for (int i = 0; i < points.size(); i += step) {
            simplified.add(points.get(i));
        }
        LatLon last = points.getLast();
        if (!simplified.getLast().equals(last)) {
            simplified.add(last);
        }
        return List.copyOf(simplified);
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }

    static String summarizeLongName(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String firstDirection = raw.split("\\|", 2)[0].trim();
        String[] parts = firstDirection.split("\\s+-\\s+");
        if (parts.length <= 1) {
            return trimTo(firstDirection, 512);
        }
        return trimTo(parts[0] + " – " + parts[parts.length - 1], 512);
    }

    private static String trimTo(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private static VehicleType toVehicleType(int routeType) {
        return switch (routeType) {
            case 0, 1, 2, 5, 7, 12 -> VehicleType.TRAM;
            default -> VehicleType.BUS;
        };
    }

    private static BufferedReader reader(byte[] content) {
        return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8));
    }

    private static String stripBom(String value) {
        if (!value.isEmpty() && value.charAt(0) == '\uFEFF') {
            return value.substring(1);
        }
        return value;
    }
}
