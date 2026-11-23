package pl.pwr.transit.transit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import pl.pwr.transit.transit.entity.Trip;

public interface TripRepository extends JpaRepository<Trip, String> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Trip t")
    int deleteAllTrips();
}
