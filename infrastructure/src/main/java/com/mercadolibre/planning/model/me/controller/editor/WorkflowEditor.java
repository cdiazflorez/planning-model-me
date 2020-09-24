package com.mercadolibre.planning.model.me.controller.editor;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;

import java.beans.PropertyEditorSupport;
import java.util.Arrays;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class WorkflowEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(final String text) {
        if (isBlank(text)) {
            throw new IllegalArgumentException("Value should not be blank");
        }

        final Workflow workflow = Workflow.from(text).orElseThrow(() -> {
            final String message = format("Value %s should be one of %s",
                    text,
                    Arrays.toString(Workflow.values()));

            return new IllegalArgumentException(message);
        });

        setValue(workflow);
    }
}
