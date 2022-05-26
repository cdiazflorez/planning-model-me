package com.mercadolibre.planning.model.me.usecases.projection;

import com.mercadolibre.planning.model.me.entities.projection.ColumnHeader;
import com.mercadolibre.planning.model.me.entities.projection.Content;
import com.mercadolibre.planning.model.me.entities.projection.Data;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.entities.projection.SimpleTable;
import com.mercadolibre.planning.model.me.entities.projection.Tab;
import com.mercadolibre.planning.model.me.entities.projection.chart.Chart;
import com.mercadolibre.planning.model.me.entities.projection.chart.ChartData;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTable;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTableAction;
import com.mercadolibre.planning.model.me.entities.projection.dateselector.Date;
import com.mercadolibre.planning.model.me.entities.projection.dateselector.DateSelector;
import com.mercadolibre.planning.model.me.entities.projection.simulationmode.ErrorMessage;
import com.mercadolibre.planning.model.me.entities.projection.simulationmode.SimulationMode;
import com.mercadolibre.planning.model.me.entities.projection.simulationmode.Snackbar;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetSlaProjectionTest {

    @InjectMocks
    private GetSlaProjection getSlaProjection;

    @Mock
    private GetSlaProjectionInbound getSlaProjectionInbound;

    @Mock
    private GetSlaProjectionOutbound getSlaProjectionOutbound;

    @Test
    public void testGetSla() {

        final GetProjectionInputDto getProjectionInputDtoInbound = GetProjectionInputDto.builder()
                .workflow(Workflow.FBM_WMS_INBOUND)
                .build();

        final GetProjectionInputDto getProjectionInputDtoOutbound = GetProjectionInputDto.builder()
                .workflow(Workflow.FBM_WMS_OUTBOUND)
                .build();

        final Projection projectionMock = mockProjection();

        when(getSlaProjectionInbound.execute(getProjectionInputDtoInbound))
                .thenReturn(projectionMock);

        when(getSlaProjectionOutbound.execute(getProjectionInputDtoOutbound))
                .thenReturn(projectionMock);

        final Projection projectionInbound = getSlaProjection.execute(getProjectionInputDtoInbound);
        final Projection projectionOutbound = getSlaProjection.execute(getProjectionInputDtoOutbound);

        assertEquals(projectionMock, projectionInbound);
        assertEquals(projectionMock, projectionOutbound);


    }


    private Projection mockProjection() {
        final Date[] dates = {new Date("", "", true)};

        final List<Map<String, Object>> map = List.of(Map.of("", new Object()));
        final List<ColumnHeader> columnHeader = List.of(new ColumnHeader("", "", ""));

        final SimpleTable simpleTable = new SimpleTable("", columnHeader, map);
        List<Map<String, Content>> content = List.of(Map.of("",
                new Content("", ZonedDateTime.now(), Map.of("", ""), "", true)));

        final ComplexTable complexTable = new ComplexTable(columnHeader,
                List.of(
                        new com.mercadolibre.planning.model.me.entities.projection.complextable.Data(
                                "", "", true, content)),
                new ComplexTableAction(",", "", ""), "");

        final Chart chart = new Chart(List.of(
                ChartData.builder().build()
        ));

        return new Projection("title",
                new DateSelector("title Date", dates),
                new Data(simpleTable, complexTable, simpleTable, chart),
                List.of(new Tab("", "")),
                new SimulationMode("", new Snackbar("", "", ""),
                        new ErrorMessage("", ""))
        );
    }
}
