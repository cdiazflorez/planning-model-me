package com.mercadolibre.planning.model.me.clients.rest;

import com.mercadolibre.restclient.MockInterceptor;
import com.mercadolibre.restclient.Request;
import com.mercadolibre.restclient.Response;
import com.mercadolibre.restclient.exception.RestException;
import com.mercadolibre.restclient.http.HTTP;
import lombok.Getter;

@Getter
public final class TestInterceptor implements MockInterceptor {

  private String requestBody;

  @Override
  public void intercept(final Request request, final Response response, final RestException e) {
    this.requestBody = new String(request.getBody(), HTTP.DEFAULT_CONTENT_TYPE_CHARSET);
  }

}
