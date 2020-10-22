package com.mercadolibre.planning.model.me.logging;

import com.mercadolibre.fbm.wms.outbound.commons.web.filter.LoggingFilter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Set;
import java.util.function.Function;

@Slf4j
@Component
public class ApiLoggingFilter extends LoggingFilter {

    private static final Set<String> METHOD_NAMES = Set.of("POST", "PUT");

    @Override
    protected Logger getLog() {
        return log;
    }

    @Override
    protected boolean shouldLogError(final HttpServletRequest httpServletRequest,
                                     final HttpServletResponse httpServletResponse) {
        return isServerError(httpServletResponse.getStatus());
    }

    @Override
    protected boolean shouldLogWarn(final HttpServletRequest httpServletRequest,
                                    final HttpServletResponse httpServletResponse) {
        return isClientError(httpServletResponse.getStatus());
    }

    @Override
    protected boolean shouldLogInfo(final HttpServletRequest httpServletRequest,
                                    final HttpServletResponse httpServletResponse) {
        return isPostOrPut(httpServletRequest.getMethod());
    }

    @Override
    protected boolean shouldLogDebug(final HttpServletRequest httpServletRequest,
                                     final HttpServletResponse httpServletResponse) {
        return false;
    }

    private boolean isServerError(final int status) {
        return isStatus(status, HttpStatus::is5xxServerError);
    }

    private boolean isClientError(final int status) {
        return isStatus(status, HttpStatus::is4xxClientError);
    }

    private boolean isStatus(final int responseStatus,
                             final Function<HttpStatus, Boolean> mapper) {

        final HttpStatus httpStatus = HttpStatus.resolve(responseStatus);
        return httpStatus != null && mapper.apply(httpStatus);
    }

    private boolean isPostOrPut(final String method) {
        return METHOD_NAMES.contains(method.toUpperCase());
    }
}
