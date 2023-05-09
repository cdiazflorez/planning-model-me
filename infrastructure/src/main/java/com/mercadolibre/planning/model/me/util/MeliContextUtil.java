package com.mercadolibre.planning.model.me.util;

import static com.mercadolibre.fbm.wms.outbound.commons.interceptor.MeliContextInterceptor.MELI_CONTEXT_ATTRIBUTE_KEY;
import static com.mercadolibre.restclient.util.Constants.X_REQUEST_ID;
import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;

import com.mercadolibre.restclient.util.MeliContext;
import com.mercadolibre.restclient.util.MeliContextBuilder;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
public class MeliContextUtil {

    private MeliContextUtil() {}

    public static MeliContext createOrRetrieveMeliContext() {
        final RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        return Optional.ofNullable(requestAttributes)
                .map(contextAttributes ->
                        contextAttributes.getAttribute(MELI_CONTEXT_ATTRIBUTE_KEY, SCOPE_REQUEST))

                .filter(contextAttribute -> contextAttribute instanceof MeliContext)
                .map(MeliContext.class::cast)
                .orElseGet(() -> createNewMeliContext(requestAttributes));
    }

    private static MeliContext createNewMeliContext(RequestAttributes requestAttributes) {
        MeliContext meliContext = Optional.ofNullable((ServletRequestAttributes) requestAttributes)
                .map(attributes ->
                        MeliContextBuilder.build(attributes.getRequest()))
                .orElseGet(MeliContextBuilder::buildFlowStarterContext);

        log.warn("The meli context is empty or null, creating new meli context. requestId: "
                + "{}", meliContext.getHeaders().getHeader(X_REQUEST_ID));

        return meliContext;
    }

}
