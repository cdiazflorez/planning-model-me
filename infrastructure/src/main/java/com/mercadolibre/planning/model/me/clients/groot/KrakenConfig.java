package com.mercadolibre.planning.model.me.clients.groot;

import com.mercadolibre.kraken.client.KrakenLibrary;
import com.mercadolibre.kraken.client.config.KrakenClientConfiguration;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("kraken.client")
@Slf4j
public class KrakenConfig {

    private int connectionTimeout = 1000;
    private int socketTimeout = 1000;
    private int maxRetries = 1;
    private String scope = "sandbox";

    @Bean
    public KrakenClient getClient() {
        log.info("Initializing Kraken library. scope {}", scope);
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
