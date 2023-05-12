package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mercadolibre.planning.model.me.entities.workflows.BacklogWorkflow;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Workflow {
    FBM_WMS_INBOUND(List.of(BacklogWorkflow.INBOUND, BacklogWorkflow.INBOUND_TRANSFER)),
    FBM_WMS_OUTBOUND(List.of(BacklogWorkflow.OUTBOUND_ORDERS)),
    INBOUND_TRANSFER(List.of(BacklogWorkflow.INBOUND_TRANSFER)),
    INBOUND(List.of(BacklogWorkflow.INBOUND));

    private final List<BacklogWorkflow> backlogWorkflow;

    private static final Map<String, Workflow> LOOKUP = Arrays.stream(values()).collect(
            toMap(Workflow::toString, Function.identity())
    );

    @JsonCreator
    public static Optional<Workflow> from(final String value) {
        return Optional.ofNullable(LOOKUP.get(value.toUpperCase().replace('-', '_')));
    }

    @JsonValue
    public String getName() {
        return this.toString().toLowerCase().replace('_', '-');
    }
}
