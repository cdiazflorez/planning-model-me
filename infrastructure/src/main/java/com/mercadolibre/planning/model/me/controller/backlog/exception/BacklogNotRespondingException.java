package com.mercadolibre.planning.model.me.controller.backlog.exception;

import com.mercadolibre.fbm.wms.outbound.commons.rest.exception.ClientException;

public class BacklogNotRespondingException extends RuntimeException{

  public BacklogNotRespondingException(final String message, final ClientException e) {
    super(message, e);
  }

}
