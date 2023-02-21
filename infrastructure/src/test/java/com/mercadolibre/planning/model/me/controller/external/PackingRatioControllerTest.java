package com.mercadolibre.planning.model.me.controller.external;

import static com.mercadolibre.planning.model.me.utils.TestUtils.getResourceAsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.PackingRatio;
import com.mercadolibre.planning.model.me.services.backlog.RatioService;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

  private static final String DATE_FROM = "2022-09-10T08:00:00Z";

  private static final String DATE_TO = "2022-09-10T10:00:00Z";

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private RatioService ratioService;

  @Test
  void testExecute() throws Exception {

    final Map<Instant, PackingRatio> serviceResponse = new ConcurrentHashMap<>();
        serviceResponse.put(Instant.parse(DATE_FROM), new PackingRatio(0.5, 0.5));
        serviceResponse.put(Instant.parse("2022-09-10T09:00:00Z"), new PackingRatio(0.5, 0.5));
        serviceResponse.put(Instant.parse(DATE_TO), new PackingRatio(0.5, 0.5));

    when(ratioService.getPackingRatio("ARBA01",
                                      Instant.parse(DATE_FROM),
                                      Instant.parse(DATE_TO)))
        .thenReturn(serviceResponse);

    // WHEN
    final ResultActions result = mockMvc.perform(
        MockMvcRequestBuilders.get(BASE_URL)
            .param("logistic_center_id", "ARBA01")
            .param("date_from", DATE_FROM)
            .param("date_to", DATE_TO)
            .param("sla_date_from", DATE_FROM)
            .param("sla_date_to", DATE_TO));

    // THEN
    result.andExpect(status().isOk());
    result.andExpect(content().json(
        getResourceAsString("get_packing_ratio_response.json")));
  }
}
