package com.mercadolibre.planning.model.me.config;

import com.mercadolibre.resilience.breaker.CircuitBreaker;
import com.mercadolibre.resilience.breaker.CircuitBreakers;
import com.mercadolibre.resilience.breaker.DummyCircuitBreaker;
import com.mercadolibre.resilience.breaker.control.CircuitControl;
import com.mercadolibre.resilience.breaker.control.FixedWeightsControl;
import lombok.AllArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@AllArgsConstructor
@Configuration
@EnableConfigurationProperties({
        CircuitBreakerConfig.UnitCircuitBreakerProperties.class,
        CircuitBreakerConfig.OutboundWaveCircuitBreakerProperties.class
})
public class CircuitBreakerConfig {

    private final UnitCircuitBreakerProperties unitsCircuitBreakerProperties;
    private final OutboundWaveCircuitBreakerProperties outboundWaveCircuitBreakerProperties;

    @Bean
    public CircuitBreaker unitCircuitBreaker() {
        return getCircuitBreakerConfig(outboundWaveCircuitBreakerProperties);
    }

    @Bean
    public CircuitBreaker outboundWaveCircuitBreaker() {
        return getCircuitBreakerConfig(unitsCircuitBreakerProperties);
    }

    private CircuitBreaker getCircuitBreakerConfig(final CircuitBreakerProperties props) {
        switch (props.getType()) {
            case FIXED:
                return CircuitBreakers.newFixedWeightsBreaker(
                        props.getResource(),
                        props.getInterval(),
                        props.getTryWindow(),
                        props.getWeights(),
                        props.getMinScore(),
                        props.getStaleInterval(),
                        props.getBucketWidthMs(),
                        props.getMinMeasures());
            case EXPONENTIAL:
                return CircuitBreakers.newExponentialBreaker(
                        props.getResource(),
                        props.getInterval(),
                        props.getTryWindow(),
                        props.getCoefficient(),
                        props.getBuckets(),
                        props.getBucketWidthMs(),
                        props.getMinScore(),
                        props.getStaleInterval(),
                        props.getMinMeasures()
                );
            case DUMMY:
            default:
                final CircuitControl control =
                        new FixedWeightsControl(props.getWeights(),
                                props.getMinScore(),
                                props.getStaleInterval(),
                                props.getBucketWidthMs(),
                                TimeUnit.MILLISECONDS,
                                props.getMinMeasures());
                return new DummyCircuitBreaker(
                        props.getResource(),
                        props.getInterval(),
                        props.getTryWindow(),
                        TimeUnit.MILLISECONDS,
                        control);
        }
    }

    @ConfigurationProperties("circuit-breaker.outbound-unit")
    public static class UnitCircuitBreakerProperties extends CircuitBreakerProperties {
    }

    @ConfigurationProperties("circuit-breaker.outbound-wave")
    public static class OutboundWaveCircuitBreakerProperties extends CircuitBreakerProperties {
    }
}
