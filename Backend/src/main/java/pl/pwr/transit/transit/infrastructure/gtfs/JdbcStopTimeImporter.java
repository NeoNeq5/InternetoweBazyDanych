package pl.pwr.transit.transit.infrastructure.gtfs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class JdbcStopTimeImporter {

    private static final Logger log = LoggerFactory.getLogger(JdbcStopTimeImporter.class);
    private static final int BATCH_SIZE = 2_000;

    private final JdbcTemplate jdbcTemplate;

    public JdbcStopTimeImporter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void clear() {
        jdbcTemplate.update("delete from stop_times");
    }

    public int importFromArchive(byte[] archiveBytes) {
        clear();
        byte[] content = readEntry(archiveBytes, "stop_times.txt");
        int total = 0;
        List<Object[]> batch = new ArrayList<>(BATCH_SIZE);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return 0;
            }
            if (!headerLine.isEmpty() && headerLine.charAt(0) == '\uFEFF') {
                headerLine = headerLine.substring(1);
            }
            Map<String, Integer> index = GtfsCsv.headerIndex(GtfsCsv.parseLine(headerLine));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> fields = GtfsCsv.parseLine(line);
                String tripId = GtfsCsv.required(fields, index, "trip_id");
                String stopId = GtfsCsv.required(fields, index, "stop_id");
                int sequence = Integer.parseInt(GtfsCsv.required(fields, index, "stop_sequence"));
                Duration arrival = GtfsTimeParser.parse(GtfsCsv.required(fields, index, "arrival_time"));
                Duration departure = GtfsTimeParser.parse(GtfsCsv.required(fields, index, "departure_time"));
                batch.add(new Object[]{
                        tripId,
                        sequence,
                        stopId,
                        GtfsTimeParser.toIntervalLiteral(arrival),
                        GtfsTimeParser.toIntervalLiteral(departure)
                });
                if (batch.size() >= BATCH_SIZE) {
                    flush(batch);
                    total += batch.size();
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                flush(batch);
                total += batch.size();
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to import stop_times.txt", ex);
        }

        log.info("Imported {} stop_times rows", total);
        return total;
    }

    private void flush(List<Object[]> batch) {
        jdbcTemplate.batchUpdate(
                """
                insert into stop_times (trip_id, stop_sequence, stop_id, arrival_time, departure_time)
                values (?, ?, ?, cast(? as interval), cast(? as interval))
                """,
                batch
        );
    }

    private static byte[] readEntry(byte[] archiveBytes, String fileName) {
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
                if (name.equalsIgnoreCase(fileName)) {
                    return zip.readAllBytes();
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read " + fileName + " from GTFS archive", ex);
        }
        throw new IllegalStateException("GTFS archive is missing " + fileName);
    }
}
