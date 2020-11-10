package com.mercadolibre.planning.model.me.clients.rest.config;

import com.mercadolibre.restclient.Request;
import com.mercadolibre.restclient.Response;
import com.mercadolibre.restclient.exception.RestException;
import com.mercadolibre.restclient.interceptor.RequestResponseInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;

import static java.lang.String.format;

@Slf4j
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public enum RestClientLoggingInterceptor implements RequestResponseInterceptor {
    INSTANCE;

    @Override
    public void intercept(final Request request, final Response response) {
        log.debug(buildSuccessLog(request, response));
    }

    @Override
    public void intercept(final Request request, final RestException restException) {
        log.debug(buildErrorLog(request, restException), restException);
    }

    public String buildSuccessLog(final Request request, final Response response) {
        return format("[api:%s] Api Request"
                        + "%n\tRequest:"
                        + "%n\t\tMethod = %s"
                        + "%n\t\tURI = %s"
                        + "%n\t\tQuery = %s"
                        + "%n\t\tBody = %s"
                        + "%n\tResponse:"
                        + "%n\t\tStatus = %s"
                        + "%n\t\tTime = %s"
                        + "%n\t\tHeaders = %s"
                        + "%n\t\tBody = %s"
                        + "%n",
                request.getPool().getName().toLowerCase(),
                request.getMethod(),
                request.getPlainURL(),
                request.getParameters(),
                request.getBody() == null
                        ? null : new String(request.getBody(), StandardCharsets.UTF_8),
                HttpStatus.valueOf(response.getStatus()),
                System.currentTimeMillis() - (Long) request.getAttribute("requestTime"),
                response.getHeaders(),
                response.getString()
        );
    }

    public String buildErrorLog(final Request request, final RestException exception) {
        return format("[api:%s] Api Request Error"
                        + "%n\tRequest:"
                        + "%n\t\tMethod = %s"
                        + "%n\t\tURI = %s"
                        + "%n\t\tQuery = %s"
                        + "%n\t\tBody = %s"
                        + "%n\tError:"
                        + "%n\t\tName = %s"
                        + "%n\t\tMessage = %s"
                        + "%n\t\tBody = %s"
                        + "%n",
                request.getPool().getName().toLowerCase(),
                request.getMethod(),
                request.getPlainURL(),
                request.getParameters(),
                request.getBody() == null
                        ? null : new String(request.getBody(), StandardCharsets.UTF_8),
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                exception.getBody()
        );
    }
}

