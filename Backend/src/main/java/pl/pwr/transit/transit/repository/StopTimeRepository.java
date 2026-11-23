package pl.pwr.transit.transit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.pwr.transit.transit.entity.StopTime;
import pl.pwr.transit.transit.entity.StopTimeId;

import java.util.Collection;
import java.util.List;

public interface StopTimeRepository extends JpaRepository<StopTime, StopTimeId> {

    @Query(value = """
            select st.departure_time,
                   t.trip_headsign,
                   t.trip_id,
                   r.route_id,
                   r.short_name,
                   r.vehicle_type_code,
                   t.direction_id
            from stop_times st
            join trips t on t.trip_id = st.trip_id
            join routes r on r.route_id = t.route_id
            where st.stop_id = :stopId
              and r.is_active = true
              and t.service_id in (:serviceIds)
              and st.departure_time >= cast(:fromTime as interval)
            order by st.departure_time
            limit :limit
            """, nativeQuery = true)
    List<Object[]> findUpcomingDeparturesForStop(
            @Param("stopId") String stopId,
            @Param("serviceIds") Collection<String> serviceIds,
            @Param("fromTime") String fromTime,
            @Param("limit") int limit
    );

    @Query(value = """
            select st.departure_time,
                   t.trip_headsign,
                   t.trip_id,
                   r.route_id,
                   r.short_name,
                   r.vehicle_type_code,
                   t.direction_id
            from stop_times st
            join trips t on t.trip_id = st.trip_id
            join routes r on r.route_id = t.route_id
            where st.stop_id = :stopId
              and t.route_id = :routeId
              and t.service_id in (:serviceIds)
              and st.departure_time >= cast(:fromTime as interval)
            order by st.departure_time
            limit :limit
            """, nativeQuery = true)
    List<Object[]> findUpcomingDeparturesForRoute(
            @Param("stopId") String stopId,
            @Param("routeId") String routeId,
            @Param("serviceIds") Collection<String> serviceIds,
            @Param("fromTime") String fromTime,
            @Param("limit") int limit
    );

    @Query(value = """
            select st.departure_time,
                   t.trip_headsign,
                   t.trip_id,
                   r.route_id,
                   r.short_name,
                   r.vehicle_type_code,
                   t.direction_id
            from stop_times st
            join trips t on t.trip_id = st.trip_id
            join routes r on r.route_id = t.route_id
            where st.stop_id = :stopId
              and t.route_id = :routeId
              and t.direction_id = :directionId
              and t.service_id in (:serviceIds)
              and st.departure_time >= cast(:fromTime as interval)
            order by st.departure_time
            limit :limit
            """, nativeQuery = true)
    List<Object[]> findUpcomingDepartures(
            @Param("stopId") String stopId,
            @Param("routeId") String routeId,
            @Param("directionId") short directionId,
            @Param("serviceIds") Collection<String> serviceIds,
            @Param("fromTime") String fromTime,
            @Param("limit") int limit
    );

    @Query(value = """
            select st.stop_sequence,
                   s.stop_id,
                   s.stop_name,
                   s.stop_lat,
                   s.stop_lon,
                   coalesce(st.arrival_time, st.departure_time) as arrival_time,
                   coalesce(st.departure_time, st.arrival_time) as departure_time
            from stop_times st
            join stops s on s.stop_id = st.stop_id
            where st.trip_id = :tripId
            order by st.stop_sequence
            """, nativeQuery = true)
    List<Object[]> findStopsForTrip(@Param("tripId") String tripId);
}
