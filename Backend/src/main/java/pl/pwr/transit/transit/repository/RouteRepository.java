package pl.pwr.transit.transit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.pwr.transit.transit.entity.Route;

import java.util.Collection;
import java.util.List;

public interface RouteRepository extends JpaRepository<Route, String> {

    List<Route> findByActiveTrueOrderByShortNameAsc();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Route r set r.active = false where r.id not in :ids")
    int deactivateMissing(@Param("ids") Collection<String> ids);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Route r set r.active = false")
    int deactivateAll();
}
