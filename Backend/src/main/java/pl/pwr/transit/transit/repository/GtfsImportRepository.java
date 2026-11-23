package pl.pwr.transit.transit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pwr.transit.transit.entity.GtfsImport;

public interface GtfsImportRepository extends JpaRepository<GtfsImport, Long> {
}
