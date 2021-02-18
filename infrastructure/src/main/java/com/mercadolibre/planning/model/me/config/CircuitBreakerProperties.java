package com.mercadolibre.planning.model.me.config;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "CircuitBreaker builder needs a double[] weights property"
)
public class CircuitBreakerProperties {
    private String resource = "CircuitBreaker";
    private int buckets = 10;
    private long bucketWidthMs = 100;
    private double minScore = 0.90;
    private long interval = 200;
    private long tryWindow = 200;
    private int minMeasures = 10;
    private int staleInterval = 100;
    private double coefficient = 0.90;
    private double[] weights = {1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
    private Type type = Type.DUMMY;

    public enum Type {
        DUMMY,
        EXPONENTIAL,
        FIXED
    }
}
