package com.mercadolibre.planning.model.me.clients.rest.config;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RestClientProperties {
    private String baseUrl = "";
    private long connectionTimeout = 500;
    private long maxIdleTime = 2000;
    private long maxPoolWait = 2000;
    private long socketTimeout = 1000;
    private int validationOnInactivity = 2000;
    private int workerThreads = 5;
    private int maxRetries = 1;
    private long retriesDelay = 500;
    private int cacheSize;
}
