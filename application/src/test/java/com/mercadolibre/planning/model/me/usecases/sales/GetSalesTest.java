package com.mercadolibre.planning.model.me.usecases.sales;

import com.mercadolibre.planning.model.me.entities.projection.Backlog;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogFilters;
import com.mercadolibre.planning.model.me.gateways.outboundunit.UnitSearchGateway;
import com.mercadolibre.planning.model.me.usecases.sales.dtos.GetSalesInputDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetSalesTest {

    @InjectMocks
    private GetSales getSales;

    @Mock
    private UnitSearchGateway unitSearchGateway;

    @Test
    public void testExecuteOk() {
        final ZonedDateTime currentTime = ZonedDateTime.now(UTC).withMinute(0).withNano(0);
        final GetSalesInputDto input = GetSalesInputDto.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId(WAREHOUSE_ID)
                .dateOutFrom(getDateFrom())
                .dateOutTo(currentTime)
                .timeZone(ZoneId.of("America/Bogota"))
                .fromDS(true)
                .build();

        when(unitSearchGateway.getSalesByCpt(any(BacklogFilters.class)))
                .thenReturn(mockList(currentTime));
        // WHEN
        final List<Backlog> sales = getSales.execute(input);

        // THEN
        assertEquals(3, sales.size());

        assertIterableEquals(getExpectedSales(currentTime), sales);
    }

    private List<Backlog> mockList(ZonedDateTime currentTime) {
        return List.of(
                new Backlog(currentTime, 2231),
                new Backlog(currentTime.plusHours(1), 1442),
                new Backlog(currentTime.plusHours(1), 725)
        );
    }

    private Iterable<?> getExpectedSales(final ZonedDateTime currentTime) {
        return mockList(currentTime);
    }

    private ZonedDateTime getDateFrom() {
        return ZonedDateTime.now(UTC).minusHours(28).withSecond(0).withNano(0);
    }
}
