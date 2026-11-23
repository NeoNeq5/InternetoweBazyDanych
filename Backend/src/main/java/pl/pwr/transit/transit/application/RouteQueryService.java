package pl.pwr.transit.transit.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pwr.transit.transit.entity.Route;
import pl.pwr.transit.transit.entity.VehicleType;
import pl.pwr.transit.transit.repository.RouteRepository;

import java.util.Comparator;
import java.util.List;

@Service
public class RouteQueryService {

    private final RouteRepository routeRepository;

    public RouteQueryService(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    @Transactional(readOnly = true)
    public List<Route> findActiveRoutes() {
        return routeRepository.findByActiveTrueOrderByShortNameAsc().stream()
                .sorted(routeDisplayOrder())
                .toList();
    }

    private static Comparator<Route> routeDisplayOrder() {
        return Comparator
                .comparingInt((Route route) -> route.getVehicleType() == VehicleType.TRAM ? 0 : 1)
                .thenComparing(route -> naturalKey(route.getShortName()))
                .thenComparing(Route::getShortName);
    }

    private static String naturalKey(String shortName) {
        if (shortName.chars().allMatch(Character::isDigit)) {
            return String.format("%05d", Integer.parseInt(shortName));
        }
        return shortName;
    }
}
