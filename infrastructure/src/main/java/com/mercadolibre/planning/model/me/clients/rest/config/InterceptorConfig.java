package com.mercadolibre.planning.model.me.clients.rest.config;

import com.mercadolibre.fbm.wms.outbound.commons.interceptor.MeliContextInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ComponentScan(basePackages = "com.mercadolibre.fbm.wms.outbound.commons.interceptor")
public class InterceptorConfig implements WebMvcConfigurer {

    @Autowired
    public MeliContextInterceptor meliContextInterceptor;

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(meliContextInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/ping");
    }

}
