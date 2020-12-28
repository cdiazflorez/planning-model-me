package com.mercadolibre.planning.model.me.clients.groot;

import com.mercadolibre.kraken.client.KrakenLibrary;
import com.mercadolibre.kraken.client.config.KrakenClientConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("kraken.client")
public class KrakenConfig {

    private int connectionTimeout = 1000;
    private int socketTimeout = 1000;
    private int maxRetries = 1;
    private String scope = "sandbox";

    @Bean
    public KrakenClient getClient() {
        KrakenClientConfiguration configuration = KrakenClientConfiguration.builder()
                .withConnectionTimeout(connectionTimeout)
                .withSocketTimeout(socketTimeout)
                .withMaxRetries(maxRetries)
                .withScope(scope)
                .build();
        KrakenLibrary library = KrakenLibrary.build(configuration);
        return new KrakenClient(library.krakenAttributesClient(),
                library.krakenUserClient());
    }
}
