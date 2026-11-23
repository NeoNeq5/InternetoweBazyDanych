package pl.pwr.transit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pl.pwr.transit.transit.entity.GtfsImport;
import pl.pwr.transit.transit.entity.GtfsService;
import pl.pwr.transit.transit.entity.ImportStatus;
import pl.pwr.transit.transit.entity.Route;
import pl.pwr.transit.transit.entity.VehicleType;
import pl.pwr.transit.transit.repository.GtfsImportRepository;
import pl.pwr.transit.transit.repository.RouteRepository;
import pl.pwr.transit.transit.repository.ServiceRepository;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class TransitApplicationTests {

	@Autowired
	private GtfsImportRepository gtfsImportRepository;

	@Autowired
	private RouteRepository routeRepository;

	@Autowired
	private ServiceRepository serviceRepository;

	@Test
	void contextLoads() {
	}

	@Test
	@Transactional
	void persistsNativeEnumAndDictionaryRelations() {
		GtfsImport gtfsImport = new GtfsImport();
		gtfsImport.setFetchedAt(Instant.now());
		gtfsImport.setChecksum("test-checksum");
		gtfsImport.setStatus(ImportStatus.IN_PROGRESS);
		GtfsImport savedImport = gtfsImportRepository.saveAndFlush(gtfsImport);
		assertNotNull(savedImport.getId());
		assertEquals(ImportStatus.IN_PROGRESS, savedImport.getStatus());

		serviceRepository.saveAndFlush(new GtfsService("svc-test", "Test service"));

		Route route = new Route();
		route.setId("route-test");
		route.setShortName("T1");
		route.setLongName("Test Route");
		route.setVehicleType(VehicleType.TRAM);
		route.setActive(true);
		route.setLastImport(savedImport);
		Route savedRoute = routeRepository.saveAndFlush(route);
		assertEquals("tram", savedRoute.getVehicleTypeCode());
		assertEquals(VehicleType.TRAM, savedRoute.getVehicleType());
	}
}
