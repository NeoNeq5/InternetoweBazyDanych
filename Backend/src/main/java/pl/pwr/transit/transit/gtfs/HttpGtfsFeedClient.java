package pl.pwr.transit.transit.gtfs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import pl.pwr.transit.config.GtfsProperties;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class HttpGtfsFeedClient {

    private static final Logger log = LoggerFactory.getLogger(HttpGtfsFeedClient.class);
    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    private static final DateTimeFormatter GTFS_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final String CUI_DOWNLOAD_TEMPLATE = "https://open-data.cui.wroclaw.pl/hdb/download/%d/";
    private static final int MAX_CANDIDATES = 3;

    private final RestClient restClient;
    private final GtfsProperties properties;
    private final ObjectMapper objectMapper;

    public HttpGtfsFeedClient(
            RestClient.Builder restClientBuilder,
            GtfsProperties properties,
            ObjectMapper objectMapper
    ) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public byte[] downloadFeed() {
        byte[] body = fetchBytes(properties.url());
        if (isZip(body)) {
            return body;
        }

        List<Long> fileIds = extractFileIds(body);
        if (fileIds.isEmpty()) {
            throw new IllegalStateException("GTFS feed endpoint did not return a ZIP archive or file list");
        }

        LocalDate today = LocalDate.now(WARSAW);
        byte[] fallbackArchive = null;
        Long fallbackId = null;

        int limit = Math.min(fileIds.size(), MAX_CANDIDATES);
        for (int i = 0; i < limit; i++) {
            long fileId = fileIds.get(i);
            String downloadUrl = String.format(CUI_DOWNLOAD_TEMPLATE, fileId);
            byte[] archive = fetchBytes(downloadUrl);
            if (!isZip(archive)) {
                continue;
            }
            if (fallbackArchive == null) {
                fallbackArchive = archive;
                fallbackId = fileId;
            }
            DateRange range = readFeedDateRange(archive);
            if (range == null || range.covers(today)) {
                log.info("Selected GTFS archive id={} ({})", fileId, range == null ? "unknown range" : range);
                return archive;
            }
            log.info("Skipping GTFS archive id={} ({}) as it does not cover {}", fileId, range, today);
        }

        if (fallbackArchive != null) {
            log.warn("No GTFS archive strictly covers {}; using latest archive id={}", today, fallbackId);
            return fallbackArchive;
        }
        throw new IllegalStateException("Failed to download a valid GTFS ZIP archive from " + properties.url());
    }

    private byte[] fetchBytes(String url) {
        byte[] body = restClient.get()
                .uri(url)
                .header("User-Agent", properties.userAgent())
                .accept(MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON, MediaType.ALL)
                .retrieve()
                .body(byte[].class);

        if (body == null || body.length == 0) {
            throw new IllegalStateException("GTFS feed download returned an empty body from " + url);
        }
        return body;
    }

    private List<Long> extractFileIds(byte[] jsonBytes) {
        JsonNode root = objectMapper.readTree(jsonBytes);
        JsonNode files = root.get("pliki");
        if (files == null || !files.isArray()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>(files.size());
        for (JsonNode item : files) {
            if (item.canConvertToLong()) {
                ids.add(item.asLong());
            }
        }
        return ids;
    }

    static DateRange readFeedDateRange(byte[] archiveBytes) {
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
                if (!name.equalsIgnoreCase("feed_info.txt")) {
                    continue;
                }
                byte[] content = zip.readAllBytes();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8))) {
                    String headerLine = reader.readLine();
                    String dataLine = reader.readLine();
                    if (headerLine == null || dataLine == null) {
                        return null;
                    }
                    if (!headerLine.isEmpty() && headerLine.charAt(0) == '\uFEFF') {
                        headerLine = headerLine.substring(1);
                    }
                    Map<String, Integer> index = GtfsCsv.headerIndex(GtfsCsv.parseLine(headerLine));
                    List<String> fields = GtfsCsv.parseLine(dataLine);
                    String startRaw = GtfsCsv.optional(fields, index, "feed_start_date");
                    String endRaw = GtfsCsv.optional(fields, index, "feed_end_date");
                    if (startRaw == null || endRaw == null) {
                        return null;
                    }
                    return new DateRange(
                            LocalDate.parse(startRaw, GTFS_DATE),
                            LocalDate.parse(endRaw, GTFS_DATE)
                    );
                }
            }
        } catch (IOException | RuntimeException ex) {
            return null;
        }
        return null;
    }

    private static boolean isZip(byte[] body) {
        return body.length >= 4 && body[0] == 'P' && body[1] == 'K';
    }

    record DateRange(LocalDate start, LocalDate end) {
        boolean covers(LocalDate date) {
            return !date.isBefore(start) && !date.isAfter(end);
        }

        @Override
        public String toString() {
            return start + ".." + end;
        }
    }
}
