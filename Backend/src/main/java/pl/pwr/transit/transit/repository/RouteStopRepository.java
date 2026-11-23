package pl.pwr.transit.transit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.pwr.transit.transit.entity.RouteStop;
import pl.pwr.transit.transit.entity.RouteStopId;
import pl.pwr.transit.transit.entity.Stop;

import java.util.List;

public interface RouteStopRepository extends JpaRepository<RouteStop, RouteStopId> {

    @Query("""
            select distinct rs.routeId
            from RouteStop rs
            where rs.stopId = :stopId
            order by rs.routeId
            """)
    List<String> findRouteIdsByStopId(@Param("stopId") String stopId);

    List<RouteStop> findByStopIdOrderByRouteIdAscDirectionIdAsc(String stopId);

    @Query("""
            select s
            from RouteStop rs
            join Stop s on s.id = rs.stopId
            where rs.routeId = :routeId and rs.directionId = :directionId and s.active = true
            order by rs.stopSequence asc, s.name asc
            """)
    List<Stop> findStopsByRouteAndDirection(
            @Param("routeId") String routeId,
            @Param("directionId") short directionId
    );

    @Query("""
            select distinct rs.directionId
            from RouteStop rs
            where rs.routeId = :routeId and rs.stopId = :stopId
            order by rs.directionId
            """)
    List<Short> findDirectionIds(@Param("routeId") String routeId, @Param("stopId") String stopId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from RouteStop rs")
    int deleteAllLinks();
}
