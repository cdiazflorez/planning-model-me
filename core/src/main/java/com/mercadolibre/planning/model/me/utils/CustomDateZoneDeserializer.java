package com.mercadolibre.planning.model.me.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class CustomDateZoneDeserializer extends JsonDeserializer<ZonedDateTime>  {

    @Override
    public ZonedDateTime deserialize(
            final JsonParser jsonParser,
            final DeserializationContext deserializationContext) throws IOException {
        return ZonedDateTime.parse(jsonParser.getText(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
