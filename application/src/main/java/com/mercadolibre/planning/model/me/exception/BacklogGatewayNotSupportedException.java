package com.mercadolibre.planning.model.me.exception;

import static java.lang.String.format;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import lombok.Builder;

public class BacklogGatewayNotSupportedException extends RuntimeException {

    public static final String MESSAGE_PATTERN =
            "There is not a backlog gateway configured for workflow:%s";

    private Workflow workflow;

    @Builder
    public BacklogGatewayNotSupportedException(final Workflow workflow) {
        super();
        this.workflow = workflow;
    }

    @Override
    public String getMessage() {
        return format(MESSAGE_PATTERN, workflow);
    }
}
