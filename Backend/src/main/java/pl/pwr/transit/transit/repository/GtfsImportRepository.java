package pl.pwr.transit.transit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pwr.transit.transit.entity.GtfsImport;
import pl.pwr.transit.transit.entity.ImportStatus;

import java.util.Optional;

public interface GtfsImportRepository extends JpaRepository<GtfsImport, Long> {

    Optional<GtfsImport> findFirstByStatusOrderByFetchedAtDesc(ImportStatus status);
}
