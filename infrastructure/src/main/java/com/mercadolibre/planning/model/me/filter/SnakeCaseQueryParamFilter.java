package com.mercadolibre.planning.model.me.filter;

import static java.util.stream.Collectors.toConcurrentMap;

import com.google.common.base.CaseFormat;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SnakeCaseQueryParamFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final FilterChain filterChain)
            throws ServletException, IOException {

        final Map<String, String[]> parameters =
                request.getParameterMap()
                        .entrySet().stream()
                        .collect(toConcurrentMap(this::toCamelCase, Map.Entry::getValue));

        filterChain.doFilter(new HttpServletRequestWrapper(request) {
            @Override
            public String getParameter(final String name) {
                return parameters.get(name) == null ? null : parameters.get(name)[0];
            }

            @Override
            public Enumeration<String> getParameterNames() {
                return Collections.enumeration(parameters.keySet());
            }

            @Override
            public String[] getParameterValues(final String name) {
                return parameters.get(name);
            }

            @Override
            public Map<String, String[]> getParameterMap() {
                return parameters;
            }
        }, response);
    }

    private String toCamelCase(final Map.Entry<String, String[]> entry) {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, entry.getKey());
    }
}
