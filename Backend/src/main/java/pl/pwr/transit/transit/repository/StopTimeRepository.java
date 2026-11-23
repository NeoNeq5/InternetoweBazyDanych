package pl.pwr.transit.transit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
                   t.trip_id
            from stop_times st
            join trips t on t.trip_id = st.trip_id
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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from StopTime st")
    int deleteAllStopTimes();
}
