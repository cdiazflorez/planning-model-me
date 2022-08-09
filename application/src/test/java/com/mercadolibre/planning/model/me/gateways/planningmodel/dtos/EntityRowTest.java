package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import static com.mercadolibre.planning.model.me.enums.ProcessName.CHECK_IN;
import static com.mercadolibre.planning.model.me.enums.ProcessName.GLOBAL;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PUT_AWAY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getNextHour;
import static com.mercadolibre.planning.model.me.utils.TestUtils.A_DATE;

import com.mercadolibre.planning.model.me.enums.ProcessName;
import java.time.ZonedDateTime;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EntityRowTest {
  private static final ZonedDateTime firstDate = getNextHour(A_DATE);


  @ParameterizedTest
  @MethodSource("mockParameterizedConfiguration")
  void fromEntity(Workflow workflow, ProcessName processName, String title) {
    // Given
    MagnitudePhoto input = MagnitudePhoto.builder()
        .workflow(workflow)
        .processName(processName)
        .date(firstDate)
        .value(150)
        .build();
    // When
    EntityRow result = EntityRow.fromEntity(input);

    // Then
    Assertions.assertEquals(title, result.getRowName().getTitle());
  }

  public static Stream<Arguments> mockParameterizedConfiguration() {
    return Stream.of(
        Arguments.of(FBM_WMS_INBOUND, PUT_AWAY, "Put Away"),
        Arguments.of(FBM_WMS_INBOUND, CHECK_IN, "Check in"),
        Arguments.of(FBM_WMS_OUTBOUND, PICKING, "Picking"),
        Arguments.of(FBM_WMS_OUTBOUND, PACKING, "Packing"),
        Arguments.of(FBM_WMS_OUTBOUND, PACKING_WALL, "Wall"),
        Arguments.of(FBM_WMS_OUTBOUND, GLOBAL, "Capacidad Máxima")
    );
  }
}
