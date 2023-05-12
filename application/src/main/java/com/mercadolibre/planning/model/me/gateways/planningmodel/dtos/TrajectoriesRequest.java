package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import com.mercadolibre.planning.model.me.enums.ProcessName;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@EqualsAndHashCode
public class TrajectoriesRequest {

    /**
     * Only trajectories of variables corresponding to this `workflow` will be included in the
     * response.
     */
    private Workflow workflow;

    /**
     * Only trajectories of variables whose {@link MagnitudeType}s is the specified here will be
     * included in the response.
     * TODO rename to `magnitudeType`
     */
    private MagnitudeType entityType;

    /**
     * Only trajectories of variables corresponding to this `warehouse` will be included in the
     * response.
     */
    private String warehouseId;

    /** Trajectory points before this date will be cropped out from the response. */
    private ZonedDateTime dateFrom;

    /** Trajectory points after this date will be cropped out from the response. */
    private ZonedDateTime dateTo;

    private Source source;

    /**
     * Only trajectories of variables corresponding to the processes listed here will be included
     * in the response.
     */
    private List<ProcessName> processName;

    private List<ProcessingType> processingType;

    private List<Simulation> simulations;
}
