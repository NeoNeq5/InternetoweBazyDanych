package pl.pwr.transit.transit.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pwr.transit.transit.entity.RouteShape;
import pl.pwr.transit.transit.repository.RouteShapeRepository;

import java.util.List;

@Service
public class RouteShapeQueryService {

    private final RouteShapeRepository routeShapeRepository;

    public RouteShapeQueryService(RouteShapeRepository routeShapeRepository) {
        this.routeShapeRepository = routeShapeRepository;
    }

    @Transactional(readOnly = true)
    public List<RouteShape> findByRouteId(String routeId) {
        return routeShapeRepository.findByRouteIdOrderByDirectionIdAsc(routeId);
    }
}
