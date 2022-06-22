package com.mercadolibre.planning.model.me.usecases.projection.simulation;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.THROUGHPUT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getCurrentUtcDate;

import com.mercadolibre.planning.model.me.entities.projection.simulationmode.DateValidate;
import com.mercadolibre.planning.model.me.entities.projection.simulationmode.ValidatedMagnitude;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.LogisticCenterGateway;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudePhoto;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessingType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Simulation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.TrajectoriesRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.projection.dtos.GetProjectionInputDto;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Named;
import lombok.AllArgsConstructor;

@Named
@AllArgsConstructor
public class ValidateSimulation {

    private static final long PLUS_DAYS = 3;

    private final PlanningModelGateway planningModelGateway;
    private final LogisticCenterGateway logisticCenterGateway;

    public List<ValidatedMagnitude> execute(GetProjectionInputDto input) {

        final ZonedDateTime dateFrom = getCurrentUtcDate();
        final ZonedDateTime dateTo = dateFrom.plusDays(PLUS_DAYS);
        if (input.getWorkflow().equals(Workflow.FBM_WMS_OUTBOUND)) {

            final LogisticCenterConfiguration config = logisticCenterGateway.getConfiguration(
                    input.getWarehouseId());

            final List<MagnitudePhoto> throughputOutbound = planningModelGateway.getTrajectories(
                    TrajectoriesRequest.builder()
                            .warehouseId(input.getWarehouseId())
                            .workflow(input.getWorkflow())
                            .entityType(THROUGHPUT)
                            .dateFrom(dateFrom)
                            .dateTo(dateTo)
                            .processName(List.of(PACKING, PACKING_WALL))
                            .processingType(List.of(ProcessingType.THROUGHPUT))
                            .build()
            );

            final Map<ZonedDateTime, Integer> throughputOutboundByHours = throughputOutbound.stream()
                    .collect(Collectors.groupingBy(MagnitudePhoto::getDate))
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            values -> values.getValue().stream().mapToInt(MagnitudePhoto::getValue).sum()));

            return input.getSimulations()
                    .stream()
                    .filter(simulation -> simulation.getProcessName().equals(ProcessName.GLOBAL))
                    .map(simulation -> getValidatedCapacity(simulation, throughputOutboundByHours, config)).collect(Collectors.toList());

        } else {
            return Collections.emptyList();
        }
    }

    private ValidatedMagnitude getValidatedCapacity(Simulation simulation,
                                                    Map<ZonedDateTime, Integer> throughputOutboundByHours,
                                                    LogisticCenterConfiguration config) {

        List<DateValidate> tphValidated = simulation.getEntities().stream()
                .filter(simulationEntity -> simulationEntity.getType().equals(THROUGHPUT))
                .flatMap(simulationEntity -> simulationEntity.getValues().stream()
                        .map(values -> {
                            int tphOutbound = throughputOutboundByHours.getOrDefault(values.getDate().withZoneSameInstant(
                                    ZoneOffset.UTC), 0);
                            return new DateValidate(
                                    values.getDate().withZoneSameInstant(config.getZoneId()),
                                    values.getQuantity() >= tphOutbound
                            );
                        }))
                .collect(Collectors.toList());

        return new ValidatedMagnitude(THROUGHPUT.getName(), tphValidated);
    }
}
