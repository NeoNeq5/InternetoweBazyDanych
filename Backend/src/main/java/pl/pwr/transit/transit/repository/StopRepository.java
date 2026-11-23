package pl.pwr.transit.transit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.pwr.transit.transit.entity.Stop;

import java.util.Collection;
import java.util.List;

public interface StopRepository extends JpaRepository<Stop, String> {

    List<Stop> findByActiveTrueOrderByNameAsc();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Stop s set s.active = false where s.id not in :ids")
    int deactivateMissing(@Param("ids") Collection<String> ids);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Stop s set s.active = false")
    int deactivateAll();
}
