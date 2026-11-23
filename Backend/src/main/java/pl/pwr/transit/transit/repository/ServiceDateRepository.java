package pl.pwr.transit.transit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.pwr.transit.transit.entity.ServiceDate;
import pl.pwr.transit.transit.entity.ServiceDateId;

import java.time.LocalDate;
import java.util.List;

public interface ServiceDateRepository extends JpaRepository<ServiceDate, ServiceDateId> {

    @Query("""
            select sd.id.serviceId
            from ServiceDate sd
            where sd.id.serviceDate = :date
            """)
    List<String> findServiceIdsByDate(@Param("date") LocalDate date);

    @Query("""
            select distinct sd.id.serviceDate
            from ServiceDate sd
            order by sd.id.serviceDate asc
            """)
    List<LocalDate> findDistinctServiceDates();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ServiceDate sd")
    int deleteAllServiceDates();
}
