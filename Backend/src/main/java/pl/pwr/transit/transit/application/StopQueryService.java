package pl.pwr.transit.transit.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pwr.transit.transit.entity.Stop;
import pl.pwr.transit.transit.repository.StopRepository;

import java.util.List;

@Service
public class StopQueryService {

    private final StopRepository stopRepository;

    public StopQueryService(StopRepository stopRepository) {
        this.stopRepository = stopRepository;
    }

    @Transactional(readOnly = true)
    public List<Stop> findActiveStops() {
        return stopRepository.findByActiveTrueOrderByNameAsc();
    }
}
