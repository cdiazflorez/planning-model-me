package com.mercadolibre.planning.model.me.controller.external;

import static com.mercadolibre.planning.model.me.utils.TestUtils.getResourceAsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.me.services.backlog.PackingRatioCalculator;
import com.mercadolibre.planning.model.me.services.backlog.RatioService;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(controllers = PackingRatioController.class)
class PackingRatioControllerTest {

  private static final String BASE_URL = "/planning/model/middleend/packing_ratio";

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private RatioService ratioService;

  @Test
  void testExecute() throws Exception {

    final Map<Instant, PackingRatioCalculator.PackingRatio> serviceResponse = Map.of(
        Instant.parse("2022-09-10T08:00:00Z"), new PackingRatioCalculator.PackingRatio(0.5, 0.5),
        Instant.parse("2022-09-10T09:00:00Z"), new PackingRatioCalculator.PackingRatio(0.5, 0.5),
        Instant.parse("2022-09-10T10:00:00Z"), new PackingRatioCalculator.PackingRatio(0.5, 0.5));

    when(ratioService.getPackingRatio("ARBA01",
                                      Instant.parse("2022-09-10T08:00:00Z"),
                                      Instant.parse("2022-09-10T10:00:00Z"),
                                      Instant.parse("2022-09-10T08:00:00Z"),
                                      Instant.parse("2022-09-10T10:00:00Z")))
        .thenReturn(serviceResponse);

    // WHEN
    final ResultActions result = mockMvc.perform(
        MockMvcRequestBuilders.get(BASE_URL)
            .param("logistic_center_id", "ARBA01")
            .param("date_from", "2022-09-10T08:00:00Z")
            .param("date_to", "2022-09-10T10:00:00Z")
            .param("sla_date_from", "2022-09-10T08:00:00Z")
            .param("sla_date_to", "2022-09-10T10:00:00Z"));

    // THEN
    result.andExpect(status().isOk());
    result.andExpect(content().json(
        getResourceAsString("get_packing_ratio_response.json")));
  }
}
