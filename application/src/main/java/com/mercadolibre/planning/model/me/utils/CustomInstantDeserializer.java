package com.mercadolibre.planning.model.me.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;

public class CustomInstantDeserializer  extends JsonDeserializer<Instant>  {
    @Override
    public Instant deserialize(
            final JsonParser jsonParser,
            final DeserializationContext deserializationContext) throws IOException {
        return OffsetDateTime.parse(jsonParser.getText()).toInstant();
    }

}
