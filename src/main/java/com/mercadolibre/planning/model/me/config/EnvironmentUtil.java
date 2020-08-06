package com.mercadolibre.planning.model.me.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.AbstractEnvironment;

import java.util.TimeZone;

@Slf4j
public final class EnvironmentUtil {

    private static final String SCOPE_ENV_VARIABLE = "SCOPE";

    private EnvironmentUtil() {}

    public static void setup() {
        final Scope scope = getScope();

        log.info("Current scope: {}", scope);

        setDefaultTimeZone("UTC");
        setActiveProfile(scope.toString());
    }

    public static Scope getScope() {
        return Scope.fromName(System.getenv(SCOPE_ENV_VARIABLE));
    }

    private static void setDefaultTimeZone(final String timeZoneId) {
        TimeZone.setDefault(TimeZone.getTimeZone(timeZoneId));
    }

    private static void setActiveProfile(final String profile) {
        System.setProperty(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME, profile);
    }
}
