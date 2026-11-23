package pl.pwr.transit.transit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import pl.pwr.transit.transit.entity.RouteShape;
import pl.pwr.transit.transit.entity.RouteShapeId;

import java.util.List;

public interface RouteShapeRepository extends JpaRepository<RouteShape, RouteShapeId> {

    List<RouteShape> findByRouteIdOrderByDirectionIdAsc(String routeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from RouteShape s")
    int deleteAllShapes();
}
