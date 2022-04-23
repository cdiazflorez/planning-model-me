package com.mercadolibre.planning.model.me.controller.tools;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SaveUnitsResponse;
import com.mercadolibre.planning.model.me.usecases.sharedistribution.SaveShareDistribution;
import com.mercadolibre.planning.model.me.utils.DateUtils;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;


import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ShareDistributionController.class)
public class ShareDistributionControllerTest {

  private final static String URL = "/planning/model/middleend/share_distribution";

  private final static String WH = "ARTW01";

  private final static int DAYS = 3;

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private SaveShareDistribution saveShareDistribution;

  @Test
  public void runTest() throws Exception {

    //GIVE

    List<String> warehouseIds = List.of(WH);
    ZonedDateTime from = DateUtils.getCurrentUtcDate().truncatedTo(ChronoUnit.DAYS);

    when(saveShareDistribution.execute(warehouseIds, from, from.plusDays(DAYS))).thenReturn(List.of(SaveUnitsResponse.builder().build()));


    //WHEN
    final ResultActions result = mockMvc.perform(MockMvcRequestBuilders
        .post(URL + "/execute")
        .param("warehouse_ids", warehouseIds.toString())
        .param("days", "3")
        .contentType(APPLICATION_JSON)
    );

    //THEN
    result.andExpect(status().isOk());

  }

}
