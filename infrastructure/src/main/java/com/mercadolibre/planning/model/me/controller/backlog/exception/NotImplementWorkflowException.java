package com.mercadolibre.planning.model.me.controller.backlog.exception;

public class NotImplementWorkflowException extends RuntimeException {
    public static final String MESSAGE = "Functionality no implement to this Workflow";

    public NotImplementWorkflowException() {
        super(MESSAGE);
    }
}
