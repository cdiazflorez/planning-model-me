package com.mercadolibre.planning.model.me.usecases.projection.simulation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.entities.projection.simulationmode.ValidatedMagnitude;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.QuantityByDate;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Simulation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SimulationEntity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.TrajectoriesRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.TimeZone;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests {@link ValidateSimulationService}.
 */
@ExtendWith(MockitoExtension.class)
public class ValidateSimulationServiceTest {

    private static final String WAREHOUSE_ID = "ARTW01";

    private static final String ZONE = "-03:00";

    private static final ZonedDateTime HOUR = ZonedDateTime.parse("2022-06-01T17:00:00Z");

    @InjectMocks
    private ValidateSimulationService validateSimulationService;

    @Mock
    private PlanningModelGateway planningModelGateway;

    @Mock
    private LogisticCenterGateway logisticCenterGateway;

    @Test
    public void executeTest() {
        //GIVEN
        when(logisticCenterGateway.getConfiguration(WAREHOUSE_ID)).thenReturn(mockLogisticCenterConfiguration());
        when(planningModelGateway.getTrajectories(any(TrajectoriesRequest.class))).thenReturn(mockGetTrajectories());

        //WHEN
        List<ValidatedMagnitude> response = validateSimulationService.execute(mockRequest(Workflow.FBM_WMS_OUTBOUND));
        List<ValidatedMagnitude> responseEmpty = validateSimulationService.execute(mockRequest(Workflow.FBM_WMS_INBOUND));

        Assertions.assertNotNull(response);
        Assertions.assertNotNull(responseEmpty);
        Assertions.assertEquals(0, responseEmpty.size());

        response.forEach(magnitudeValidate ->
                magnitudeValidate.getValues()
                        .forEach(value -> Assertions.assertFalse(value.isValid()))
        );
    }

    private LogisticCenterConfiguration mockLogisticCenterConfiguration() {
        return new LogisticCenterConfiguration(TimeZone.getTimeZone(ZoneOffset.of(ZONE)));
    }

    private List<MagnitudePhoto> mockGetTrajectories() {
        return List.of(MagnitudePhoto.builder().date(HOUR)
                        .value(2000)
                        .processName(ProcessName.PACKING)
                        .build(),
                MagnitudePhoto.builder().date(HOUR)
                        .value(2000)
                        .processName(ProcessName.PACKING_WALL)
                        .build()
        );

    }

    private GetProjectionInputDto mockRequest(Workflow workflow) {
        return GetProjectionInputDto.builder()
                .warehouseId(WAREHOUSE_ID)
                .workflow(workflow)
                .simulations(List.of(
                        new Simulation(ProcessName.GLOBAL,
                                List.of(
                                        new SimulationEntity(MagnitudeType.THROUGHPUT,
                                                List.of(new QuantityByDate(HOUR, 3000))
                                        )
                                )
                        )
                )).build();
    }
}
