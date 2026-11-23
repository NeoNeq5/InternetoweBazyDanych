package pl.pwr.transit.transit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pwr.transit.transit.entity.GtfsService;

public interface ServiceRepository extends JpaRepository<GtfsService, String> {
}
