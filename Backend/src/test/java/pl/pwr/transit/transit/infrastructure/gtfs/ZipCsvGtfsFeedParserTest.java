package pl.pwr.transit.transit.infrastructure.gtfs;

import org.junit.jupiter.api.Test;
import pl.pwr.transit.transit.application.model.ParsedGtfsFeed;
import pl.pwr.transit.transit.entity.VehicleType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZipCsvGtfsFeedParserTest {

    @Test
    void parsesRoutesStopsAndShapesFromZip() throws IOException {
        byte[] archive = zip(
                "routes.txt",
                """
                route_id,agency_id,route_short_name,route_long_name,route_desc,route_type
                4,1,"4","","BISKUPIN - ... - GRABISZYNEK|GRABISZYNEK - ... - BISKUPIN",0
                A,1,"A","","SOŁTYSOWICE - ... - RACŁAWICKA|RACŁAWICKA - ... - SOŁTYSOWICE",3
                """,
                "stops.txt",
                """
                stop_id,stop_name,stop_lat,stop_lon,parent_station
                1,Plac Grunwaldzki,51.11,17.06,
                2,Plac Grunwaldzki 21,51.1101,17.0602,1
                """,
                "shapes.txt",
                """
                shape_id,shape_pt_lat,shape_pt_lon,shape_pt_sequence
                s4a,51.10,17.01,0
                s4a,51.11,17.02,1
                s4a,51.12,17.03,2
                s4b,51.12,17.03,0
                s4b,51.10,17.01,1
                """,
                "trips.txt",
                """
                route_id,service_id,trip_id,trip_headsign,direction_id,shape_id
                4,1,t1,Biskupin,0,s4a
                4,1,t2,Oporów,1,s4b
                4,1,t3,Biskupin,0,s4a
                """
        );

        ParsedGtfsFeed feed = new ZipCsvGtfsFeedParser().parse(archive);

        assertEquals(2, feed.routes().size());
        assertEquals(VehicleType.TRAM, feed.routes().getFirst().vehicleType());
        assertEquals("BISKUPIN – GRABISZYNEK", feed.routes().getFirst().longName());
        assertEquals(VehicleType.BUS, feed.routes().get(1).vehicleType());
        assertEquals(2, feed.stops().size());
        assertEquals("1", feed.stops().get(1).parentStationId());
        assertEquals(2, feed.shapes().size());
        assertEquals(3, feed.shapes().getFirst().points().size());
    }

    @Test
    void summarizeLongNameUsesTerminals() {
        assertEquals(
                "ZOO – DWORZEC GŁÓWNY",
                ZipCsvGtfsFeedParser.summarizeLongName(
                        "ZOO - Wróblewskiego - Skłodowskiej-Curie - DWORZEC GŁÓWNY|DWORZEC GŁÓWNY - ... - ZOO",
                        "0"
                )
        );
        assertTrue(ZipCsvGtfsFeedParser.summarizeLongName(null, "X").equals("X"));
    }

    private static byte[] zip(String... nameAndContent) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(buffer)) {
            for (int i = 0; i < nameAndContent.length; i += 2) {
                zip.putNextEntry(new ZipEntry(nameAndContent[i]));
                zip.write(nameAndContent[i + 1].getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return buffer.toByteArray();
    }
}
