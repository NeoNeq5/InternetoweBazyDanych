package pl.pwr.transit.transit.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import pl.pwr.transit.transit.entity.GtfsImport;
import pl.pwr.transit.transit.entity.ImportStatus;
import pl.pwr.transit.transit.gtfs.HttpGtfsFeedClient;
import pl.pwr.transit.transit.gtfs.JdbcStopTimeImporter;
import pl.pwr.transit.transit.gtfs.ParsedGtfsFeed;
import pl.pwr.transit.transit.gtfs.ZipCsvGtfsFeedParser;
import pl.pwr.transit.transit.repository.GtfsImportRepository;
import pl.pwr.transit.transit.repository.RouteRepository;
import pl.pwr.transit.transit.repository.RouteShapeRepository;
import pl.pwr.transit.transit.repository.RouteStopRepository;
import pl.pwr.transit.transit.repository.ServiceDateRepository;
import pl.pwr.transit.transit.repository.ServiceRepository;
import pl.pwr.transit.transit.repository.StopRepository;
import pl.pwr.transit.transit.repository.TripRepository;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GtfsImportServiceTest {

    @Mock
    private HttpGtfsFeedClient feedClient;
    @Mock
    private ZipCsvGtfsFeedParser feedParser;
    @Mock
    private GtfsImportRepository importRepository;
    @Mock
    private RouteRepository routeRepository;
    @Mock
    private StopRepository stopRepository;
    @Mock
    private RouteShapeRepository routeShapeRepository;
    @Mock
    private TripRepository tripRepository;
    @Mock
    private RouteStopRepository routeStopRepository;
    @Mock
    private ServiceRepository serviceRepository;
    @Mock
    private ServiceDateRepository serviceDateRepository;
    @Mock
    private JdbcStopTimeImporter stopTimeImporter;

    @Test
    void persistsFailedStatusOutsideRolledBackDataTransaction() {
        AtomicBoolean inTransaction = new AtomicBoolean(false);
        AtomicBoolean rolledBack = new AtomicBoolean(false);
        AtomicBoolean failedSavedOutsideTx = new AtomicBoolean(false);

        TransactionTemplate txTemplate = new TransactionTemplate(new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                inTransaction.set(true);
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
                inTransaction.set(false);
            }

            @Override
            public void rollback(TransactionStatus status) {
                rolledBack.set(true);
                inTransaction.set(false);
            }
        });

        ObjectMapper objectMapper = JsonMapper.builder().build();
        GtfsImportService service = new GtfsImportService(
                feedClient,
                feedParser,
                importRepository,
                routeRepository,
                stopRepository,
                routeShapeRepository,
                tripRepository,
                routeStopRepository,
                serviceRepository,
                serviceDateRepository,
                stopTimeImporter,
                txTemplate,
                objectMapper
        );

        when(importRepository.save(any(GtfsImport.class))).thenAnswer(inv -> {
            GtfsImport rec = inv.getArgument(0);
            if (rec.getId() == null) {
                rec.setId(42L);
            }
            if (rec.getStatus() == ImportStatus.FAILED && !inTransaction.get()) {
                failedSavedOutsideTx.set(true);
            }
            return rec;
        });
        when(importRepository.findById(42L)).thenAnswer(inv -> {
            GtfsImport rec = new GtfsImport();
            rec.setId(42L);
            rec.setStatus(ImportStatus.IN_PROGRESS);
            return Optional.of(rec);
        });
        when(feedClient.downloadFeed()).thenReturn(new byte[]{'P', 'K', 3, 4});
        when(feedParser.parse(any())).thenReturn(new ParsedGtfsFeed(
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
        ));
        when(stopTimeImporter.importFromArchive(any())).thenThrow(new IllegalStateException("stop_times boom"));

        IllegalStateException ex = assertThrows(IllegalStateException.class, service::importFeed);
        assertEquals("stop_times boom", ex.getMessage());
        assertTrue(rolledBack.get(), "Data transaction should be rolled back on failure");
        assertTrue(failedSavedOutsideTx.get(), "FAILED status must be saved outside the rolled-back transaction");
        verify(stopTimeImporter, times(1)).clear();

        ArgumentCaptor<GtfsImport> saved = ArgumentCaptor.forClass(GtfsImport.class);
        verify(importRepository, times(2)).save(saved.capture());
        assertEquals(ImportStatus.FAILED, saved.getAllValues().getLast().getStatus());
        assertEquals("stop_times boom", saved.getAllValues().getLast().getErrorMessage());
    }
}
