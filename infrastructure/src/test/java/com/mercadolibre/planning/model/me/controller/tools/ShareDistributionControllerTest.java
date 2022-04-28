package com.mercadolibre.planning.model.me.controller.tools;

import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SaveUnitsResponse;
import com.mercadolibre.planning.model.me.usecases.sharedistribution.SaveShareDistribution;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/** Tests of ShareDistributionController. */
@Slf4j
@WebMvcTest(controllers = ShareDistributionController.class)
public class ShareDistributionControllerTest {

  private static final String URL = "/planning/model/middleend/share_distribution";

  private static final String WH = "ARTW01";

  private static final int DAYS = 3;

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private SaveShareDistribution saveShareDistribution;

  @Test
  public void runTest() {

    //GIVE

    List<String> warehouseIds = List.of(WH);

    when(saveShareDistribution.execute(warehouseIds, null, DAYS, Instant.now())).thenReturn(List.of(SaveUnitsResponse.builder().build()));


    //WHEN
    final ResultActions result;
    try {
      result = mockMvc.perform(MockMvcRequestBuilders
          .post(URL + "/execute")
          .param("warehouse_ids", warehouseIds.toString())
          .param("days", "3")
          .contentType(APPLICATION_JSON)
      );

      //THEN
      result.andExpect(status().isOk());

    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }


  }

}
