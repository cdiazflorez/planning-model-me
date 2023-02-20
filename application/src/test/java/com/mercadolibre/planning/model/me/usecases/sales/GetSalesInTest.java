package com.mercadolibre.planning.model.me.usecases.sales;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogCurrentRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogFilters;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.outboundunit.UnitSearchGateway;
import com.mercadolibre.planning.model.me.gateways.toogle.FeatureSwitches;
import com.mercadolibre.planning.model.me.services.sales.GetSalesByDateIn;
import com.mercadolibre.planning.model.me.services.sales.dtos.GetSalesInputDto;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GetSalesInTest {

    @InjectMocks
    private GetSalesByDateIn getSalesByDateIn;

    @Mock
    private UnitSearchGateway unitSearchGateway;

    @Mock
    private FeatureSwitches featureSwitches;

    @Mock
    private BacklogApiGateway backlogApiGateway;

    @Test
    public void testExecuteOkCallOutboundUnit() {
        final ZonedDateTime currentTime = ZonedDateTime.now(UTC).withMinute(0).withNano(0);
        final GetSalesInputDto input = GetSalesInputDto.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId(WAREHOUSE_ID)
                .dateOutFrom(getDateFrom())
                .dateOutTo(currentTime)
                .timeZone(ZoneId.of("America/Bogota"))
                .fromDS(true)
                .build();

        when(featureSwitches.shouldCallBacklogApi()).thenReturn(false);

        when(unitSearchGateway.getSalesByCpt(any(BacklogFilters.class)))
                .thenReturn(mockList(currentTime));
        // WHEN
        final List<Backlog> sales = getSalesByDateIn.execute(input);

        // THEN
        assertEquals(3, sales.size());

        assertIterableEquals(getExpectedSales(currentTime), sales);
    }

    @Test
    public void testExecuteOkCallBacklogApi() {
        final ZonedDateTime currentTime = ZonedDateTime.now(UTC).withNano(0);
        final GetSalesInputDto input = GetSalesInputDto.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId(WAREHOUSE_ID)
                .dateCreatedFrom(getDateFrom())
                .dateCreatedTo(currentTime)
                .dateOutFrom(getDateFrom())
                .dateOutTo(currentTime)
                .fromDS(true)
                .build();

        when(featureSwitches.shouldCallBacklogApi()).thenReturn(true);

        when(backlogApiGateway.getCurrentBacklog(any(BacklogCurrentRequest.class)))
                .thenReturn(mockListBa(currentTime));
        // WHEN
        final List<Backlog> sales = getSalesByDateIn.execute(input);

        // THEN
        assertEquals(3, sales.size());

        assertIterableEquals(getExpectedSalesBa(currentTime), sales);
    }

    private List<Backlog> mockList(final ZonedDateTime currentTime) {
        return List.of(
                new Backlog(currentTime, 2231),
                new Backlog(currentTime.plusHours(1), 1442),
                new Backlog(currentTime.plusHours(1), 725)
        );
    }

    private List<Consolidation> mockListBa(final ZonedDateTime currentTime) {
        return List.of(new Consolidation(currentTime.toInstant(), getRequestKeys(currentTime.toInstant()), 2231, false),
                new Consolidation(currentTime.toInstant(), getRequestKeys(currentTime.toInstant()), 1442, false),
                new Consolidation(currentTime.toInstant(), getRequestKeys(currentTime.toInstant()), 725, false));
    }

    private Iterable<?> getExpectedSales(final ZonedDateTime currentTime) {
        return mockList(currentTime);
    }

    private Iterable<?> getExpectedSalesBa(final ZonedDateTime currentTime) {
        final List<Consolidation> backlogConsolidation = mockListBa(currentTime);

        return backlogConsolidation.stream().map(item -> new Backlog(
                ZonedDateTime.ofInstant(item.getDate(), UTC),
                item.getTotal())).collect(Collectors.toList());
    }

    private ZonedDateTime getDateFrom() {
        return ZonedDateTime.now(UTC).minusHours(28).withSecond(0).withNano(0);
    }

    private Map<String, String> getRequestKeys(final Instant cpt) {
        final Map<String, String> result = new ConcurrentHashMap<>();
        result.put("date_in", cpt.toString());
        return result;
    }
}
