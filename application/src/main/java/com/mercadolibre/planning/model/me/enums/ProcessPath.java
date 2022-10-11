package com.mercadolibre.planning.model.me.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

public enum ProcessPath {
    NON_TOT_MONO,
    SIOC,
    PP_DEFAULT_MULTI,
    NON_TOT_MULTI_ORDER,
    TOT_MULTI_BATCH,
    TOT_MULTI_ORDER,
    TOT_MONO,
    PP_DEFAULT_MONO,
    AMBIENT,
    REFRIGERATED,
    GLOBAL;

    @JsonCreator
    public static ProcessPath from(final String value) {
        return valueOf(value.toUpperCase(Locale.getDefault()));
    }

    @JsonValue
    public String getName() {
        return name().toLowerCase(Locale.getDefault());
    }
}
