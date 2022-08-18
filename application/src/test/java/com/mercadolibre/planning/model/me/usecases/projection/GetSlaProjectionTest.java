package com.mercadolibre.planning.model.me.usecases.projection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.entities.projection.ColumnHeader;
import com.mercadolibre.planning.model.me.entities.projection.Content;
import com.mercadolibre.planning.model.me.entities.projection.PlanningView;
import com.mercadolibre.planning.model.me.entities.projection.Projection;
import com.mercadolibre.planning.model.me.entities.projection.ResultData;
import com.mercadolibre.planning.model.me.entities.projection.SimpleTable;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTable;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTableAction;
import com.mercadolibre.planning.model.me.entities.projection.dateselector.Date;
import com.mercadolibre.planning.model.me.entities.projection.dateselector.DateSelector;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GetSlaProjectionTest {
  private static final Boolean PROJECTIONS_VERSION = true;

  private static final ZonedDateTime CURRENT_DATE = Instant.parse("2022-07-13T13:00:00Z").atZone(ZoneId.of("UTC"));

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

    final PlanningView planningViewMock = mockPlanningView();

    when(getSlaProjectionInbound.execute(getProjectionInputDtoInbound))
        .thenReturn(planningViewMock);

    when(getSlaProjectionOutbound.execute(getProjectionInputDtoOutbound))
        .thenReturn(planningViewMock);

    final PlanningView planningViewInbound = getSlaProjection.execute(getProjectionInputDtoInbound);
    final PlanningView planningViewOutbound = getSlaProjection.execute(getProjectionInputDtoOutbound);

    assertEquals(planningViewMock, planningViewInbound);
    assertEquals(planningViewMock, planningViewOutbound);


  }

  private PlanningView mockPlanningView() {
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

    final List<Projection> projections = List.of(
        new Projection(
            Instant.parse("2022-07-13T13:00:00Z"),
            Instant.parse("2022-07-12T14:00:00Z"),
            21,
            0L,
            0,
            0,
            false,
            false,
            0.0,
            null,
            null,
            0,
            null)
    );

    return PlanningView.builder()
        .isNewVersion(PROJECTIONS_VERSION)
        .currentDate(CURRENT_DATE)
        .dateSelector(new DateSelector("title Date", dates))
        .data(new ResultData(simpleTable, complexTable, projections))
        .build();
  }
}
