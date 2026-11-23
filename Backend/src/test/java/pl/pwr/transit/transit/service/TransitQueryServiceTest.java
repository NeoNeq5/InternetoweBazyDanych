package pl.pwr.transit.transit.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.pwr.transit.transit.entity.RouteShape;
import pl.pwr.transit.transit.repository.RouteRepository;
import pl.pwr.transit.transit.repository.RouteShapeRepository;
import pl.pwr.transit.transit.repository.RouteStopRepository;
import pl.pwr.transit.transit.repository.ServiceDateRepository;
import pl.pwr.transit.transit.repository.StopRepository;
import pl.pwr.transit.transit.repository.StopTimeRepository;
import pl.pwr.transit.transit.repository.TripRepository;
import pl.pwr.transit.transit.web.dto.RouteShapeResponse;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransitQueryServiceTest {

    @Mock
    private StopRepository stopRepository;
    @Mock
    private RouteRepository routeRepository;
    @Mock
    private RouteShapeRepository routeShapeRepository;
    @Mock
    private RouteStopRepository routeStopRepository;
    @Mock
    private ServiceDateRepository serviceDateRepository;
    @Mock
    private StopTimeRepository stopTimeRepository;
    @Mock
    private TripRepository tripRepository;

    @Test
    void readsShapePointsUsingJacksonObjectMapper() {
        TransitQueryService service = new TransitQueryService(
                stopRepository,
                routeRepository,
                routeShapeRepository,
                routeStopRepository,
                serviceDateRepository,
                stopTimeRepository,
                tripRepository,
                JsonMapper.builder().build()
        );

        RouteShape shape = new RouteShape();
        shape.setRouteId("4");
        shape.setDirectionId((short) 0);
        shape.setPointsJson("[[51.100000,17.010000],[51.110000,17.020000]]");

        when(routeRepository.existsById("4")).thenReturn(true);
        when(routeShapeRepository.findByRouteIdOrderByDirectionIdAsc("4")).thenReturn(List.of(shape));

        List<RouteShapeResponse> responses = service.routeShapes("4");

        assertEquals(1, responses.size());
        assertEquals((short) 0, responses.getFirst().directionId());
        assertEquals(List.of(List.of(51.1, 17.01), List.of(51.11, 17.02)), responses.getFirst().points());
    }
}
