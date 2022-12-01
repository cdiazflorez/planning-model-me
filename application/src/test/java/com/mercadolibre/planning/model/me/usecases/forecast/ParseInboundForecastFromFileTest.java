package com.mercadolibre.planning.model.me.usecases.forecast;

import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.INBOUND_CHECKIN_PRODUCTIVITY_POLYVALENCES;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.INBOUND_PUTAWAY_PRODUCTIVITY_POLIVALENCES;
import static com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model.ForecastColumnName.INBOUND_RECEIVING_PRODUCTIVITY_POLYVALENCES;
import static com.mercadolibre.planning.model.me.usecases.forecast.utils.SpreadsheetUtils.createMeliDocumentFrom;
import static com.mercadolibre.planning.model.me.utils.TestUtils.getResource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Forecast;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Metadata;
import java.util.List;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParseInboundForecastFromFileTest {
  private static final String VALID_FILE_PATH = "inbound_planning_ok.xlsx";

  private static final LogisticCenterConfiguration CONFIG =
      new LogisticCenterConfiguration(TimeZone.getDefault());

  @Test
  void testUploadForecastOk() {
    // GIVEN
    var document = createMeliDocumentFrom(getResource(VALID_FILE_PATH));

    // WHEN
    final Forecast forecast = ParseInboundForecastFromFile.parse("ARBA01", document, 1234L, CONFIG, logisticCenter -> true);

    // THEN
    final List<Metadata> metadata = forecast.getMetadata();
    assertNotNull(forecast);
    assertEquals(5, metadata.size());
    assertEquals("ARBA01", metadata.get(0).getValue());
    assertEquals(INBOUND_CHECKIN_PRODUCTIVITY_POLYVALENCES.getName(), metadata.get(2).getKey());
    assertEquals(INBOUND_PUTAWAY_PRODUCTIVITY_POLIVALENCES.getName(), metadata.get(3).getKey());
    assertEquals(INBOUND_RECEIVING_PRODUCTIVITY_POLYVALENCES.getName(), metadata.get(4).getKey());
  }
}
