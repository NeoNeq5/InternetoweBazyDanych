package pl.pwr.transit.transit.gtfs;

import org.junit.jupiter.api.Test;
import pl.pwr.transit.transit.entity.VehicleType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZipCsvGtfsFeedParserTest {

    @Test
    void parsesRoutesStopsShapesTripsAndLinks() throws IOException {
        byte[] archive = zip(
                "routes.txt",
                """
                route_id,agency_id,route_short_name,route_long_name,route_desc,route_type
                4,1,"4","","BISKUPIN - ... - GRABISZYNEK|GRABISZYNEK - ... - BISKUPIN",0
                """,
                "stops.txt",
                """
                stop_id,stop_name,stop_lat,stop_lon,parent_station
                1,Plac Grunwaldzki,51.11,17.06,
                2,Biskupin,51.10,17.10,
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
                """,
                "stop_times.txt",
                """
                trip_id,arrival_time,departure_time,stop_id,stop_sequence
                t1,08:00:00,08:00:00,1,0
                t1,08:10:00,08:10:00,2,1
                t2,09:00:00,09:00:00,2,0
                t2,09:10:00,09:10:00,1,1
                """,
                "calendar.txt",
                """
                service_id,monday,tuesday,wednesday,thursday,friday,saturday,sunday,start_date,end_date
                1,1,1,1,1,1,1,1,20261101,20261103
                """,
                "calendar_dates.txt",
                """
                service_id,date,exception_type
                """
        );

        ParsedGtfsFeed feed = new ZipCsvGtfsFeedParser().parse(archive);

        assertEquals(1, feed.routes().size());
        assertEquals(VehicleType.TRAM, feed.routes().getFirst().vehicleType());
        assertEquals(2, feed.stops().size());
        assertEquals(2, feed.shapes().size());
        assertEquals(2, feed.trips().size());
        assertEquals(4, feed.routeStops().size());
        assertFalse(feed.serviceDates().isEmpty());
        assertTrue(feed.routeStops().stream().anyMatch(link ->
                link.routeId().equals("4") && link.stopId().equals("1") && link.directionId() == 0));
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
