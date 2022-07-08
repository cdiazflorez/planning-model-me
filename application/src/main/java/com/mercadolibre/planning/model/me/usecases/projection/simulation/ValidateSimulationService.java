package com.mercadolibre.planning.model.me.usecases.projection.simulation;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.MAX_CAPACITY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.THROUGHPUT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.utils.DateUtils.convertToUtc;
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
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Named;
import lombok.AllArgsConstructor;

@Named
@AllArgsConstructor
public class ValidateSimulationService {

    private static final long PLUS_DAYS = 3;

    private final PlanningModelGateway planningModelGateway;
    private final LogisticCenterGateway logisticCenterGateway;

    public List<ValidatedMagnitude> execute(GetProjectionInputDto input) {

        final ZonedDateTime dateFrom = getCurrentUtcDate();
        final ZonedDateTime dateTo = dateFrom.plusDays(PLUS_DAYS);
        if (input.getWorkflow().equals(Workflow.FBM_WMS_OUTBOUND)) {

            final LogisticCenterConfiguration config = logisticCenterGateway.getConfiguration(
                    input.getWarehouseId());

            final Map<ZonedDateTime, Integer> throughputOutboundByHours = getThp(
                    input.getWarehouseId(),
                    input.getWorkflow(),
                    dateFrom,
                    dateTo);

            return input.getSimulations()
                    .stream()
                    .filter(simulation -> simulation.getProcessName().equals(ProcessName.GLOBAL))
                    .map(simulation -> getValidatedCapacity(simulation, throughputOutboundByHours, config)).collect(Collectors.toList());

        } else {
            return Collections.emptyList();
        }
    }

    /**
     * This method validates simulations for maximum capacity, although the Model API uses as Headcount in the simulationEntity
     * for the front it is a Throughput.
     * @param simulation                Simulation to validate.
     * @param throughputOutboundByHours Map with the reference values to validate the simulations.
     * @param config                    Configuration of the logistic center to obtain the time zone.
     * @return {@link ValidatedMagnitude} Object with validation.
     */
    private ValidatedMagnitude getValidatedCapacity(final Simulation simulation,
                                                    final Map<ZonedDateTime, Integer> throughputOutboundByHours,
                                                    final LogisticCenterConfiguration config) {

        final List<DateValidate> tphValidated = simulation.getEntities().stream()
                .filter(simulationEntity -> simulationEntity.getType().equals(MAX_CAPACITY))
                .flatMap(simulationEntity -> simulationEntity.getValues().stream()
                        .map(values -> {
                            int tphOutbound = throughputOutboundByHours.getOrDefault(convertToUtc(values.getDate()), 0);
                            return new DateValidate(
                                    values.getDate().withZoneSameInstant(config.getZoneId()),
                                    values.getQuantity() >= tphOutbound
                            );
                        }))
                .collect(Collectors.toList());

        return new ValidatedMagnitude(THROUGHPUT.getName(), tphValidated);
    }

    private Map<ZonedDateTime, Integer> getThp(final String warehouseId,
                                               final Workflow workflow,
                                               final ZonedDateTime dateFrom,
                                               final ZonedDateTime dateTo) {

        final List<MagnitudePhoto> throughputOutbound = planningModelGateway.getTrajectories(
                TrajectoriesRequest.builder()
                        .warehouseId(warehouseId)
                        .workflow(workflow)
                        .entityType(THROUGHPUT)
                        .dateFrom(dateFrom)
                        .dateTo(dateTo)
                        .processName(List.of(PACKING, PACKING_WALL))
                        .processingType(List.of(ProcessingType.THROUGHPUT))
                        .build()
        );

        return throughputOutbound.stream()
                .collect(Collectors.toMap(
                        MagnitudePhoto::getDate,
                        MagnitudePhoto::getValue,
                        Integer::sum));
    }
}
