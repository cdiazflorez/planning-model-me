package com.mercadolibre.planning.model.me.controller.deviation;

import static com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission.OUTBOUND_SIMULATION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.TestUtils.USER_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.getResourceAsString;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.me.enums.DeviationType;
import com.mercadolibre.planning.model.me.enums.ShipmentType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.DeviationResponse;
import com.mercadolibre.planning.model.me.metric.DatadogMetricService;
import com.mercadolibre.planning.model.me.usecases.authorization.AuthorizeUser;
import com.mercadolibre.planning.model.me.usecases.authorization.dtos.AuthorizeUserDto;
import com.mercadolibre.planning.model.me.usecases.authorization.exceptions.UserNotAuthorizedException;
import com.mercadolibre.planning.model.me.usecases.deviation.DisableDeviation;
import com.mercadolibre.planning.model.me.usecases.deviation.SaveDeviation;
import com.mercadolibre.planning.model.me.usecases.deviation.SaveOutboundDeviation;
import com.mercadolibre.planning.model.me.usecases.deviation.dtos.DisableDeviationInput;
import com.mercadolibre.planning.model.me.usecases.deviation.dtos.SaveDeviationInput;
import java.time.ZonedDateTime;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(controllers = DeviationController.class)
public class DeviationControllerTest {

  private static final String URL = "/planning/model/middleend/workflows/%s/deviations";

  @Autowired
  private MockMvc mvc;

  @MockBean
  private SaveOutboundDeviation saveOutboundDeviation;

  @MockBean
  private SaveDeviation saveDeviation;

  @MockBean
  private DisableDeviation disableDeviation;

  @MockBean
  private AuthorizeUser authorizeUser;

  @MockBean
  private DatadogMetricService datadogMetricService;

  private ResultActions result;

  @BeforeEach
  public void setUp() {
    Mockito.reset(authorizeUser);
    Mockito.reset(saveOutboundDeviation);
    Mockito.reset(disableDeviation);
  }

  private void thenStatusAndMessageAreCorrect(final HttpStatus status,
                                              final String message) throws Exception {
    result.andExpect(status == OK ? status().isOk() : status().isInternalServerError())
        .andExpect(content().json(new JSONObject()
            .put("status", status.value())
            .put("message", message)
            .toString()));
  }

  @Nested
  @DisplayName("Test save deviation")
  class SaveOutboundDeviationController {

    @Test
    void saveDeviationOk() throws Exception {
      // GIVEN
      when(saveOutboundDeviation.execute(any()))
          .thenReturn(DeviationResponse.builder()
              .status(OK.value())
              .message("Forecast deviation saved")
              .build());

      // WHEN
      whenSaveDeviation();

      // THEN
      verify(authorizeUser).execute(new AuthorizeUserDto(USER_ID,
          singletonList(OUTBOUND_SIMULATION)));

      thenStatusAndMessageAreCorrect(OK, "Forecast deviation saved");
    }

    @Test
    void saveDeviationErrorApi() throws Exception {
      // GIVEN
      when(saveOutboundDeviation.execute(any(SaveDeviationInput.class)))
          .thenThrow(RuntimeException.class);

      // WHEN
      whenSaveDeviation();

      // THEN
      verify(authorizeUser).execute(new AuthorizeUserDto(USER_ID,
          singletonList(OUTBOUND_SIMULATION)));
      thenStatusAndMessageAreCorrect(INTERNAL_SERVER_ERROR,
          "Error persisting forecast deviation");
    }

    @Test
    void saveDeviationUserNoAuthorized() throws Exception {
      // GIVEN
      when(authorizeUser.execute(any(AuthorizeUserDto.class)))
          .thenThrow(UserNotAuthorizedException.class);

      // WHEN
      whenSaveDeviation();

      // THEN
      result.andExpect(status().isForbidden());
      verifyNoInteractions(saveOutboundDeviation);
    }

    private void whenSaveDeviation() throws Exception {
      result = mvc.perform(MockMvcRequestBuilders
          .post(format(URL, FBM_WMS_OUTBOUND.getName()) + "/save")
          .content(getResourceAsString("post_save_deviation_request.json"))
          .contentType(APPLICATION_JSON));
    }
  }

  @Nested
  @DisplayName("Test save deviation scheduling")
  class SaveDeviationShipmentController {

    @Test
    void saveDeviationShipmentOk() throws Exception {
      // GIVEN
      final SaveDeviationInput saveDeviationInput = SaveDeviationInput.builder()
          .workflow(FBM_WMS_INBOUND)
          .warehouseId(WAREHOUSE_ID)
          .dateFrom(ZonedDateTime.parse("2021-01-21T15:00Z[UTC]"))
          .dateTo(ZonedDateTime.parse("2021-01-21T17:00Z[UTC]"))
          .type(DeviationType.UNITS)
          .value(5.9)
          .userId(USER_ID)
          .build();

      // WHEN
      mvc.perform(MockMvcRequestBuilders
          .post(format(URL, FBM_WMS_INBOUND.getName()) + "/units/save")
          .content(getResourceAsString("post_save_deviation_request.json"))
          .contentType(APPLICATION_JSON));

      // THEN
      verify(saveDeviation).execute(saveDeviationInput);
    }
  }

  @Nested
  @DisplayName("Test disable deviation")
  class DisableDeviationController {
    @Test
    void disableDeviationOk() throws Exception {
      // GIVEN
      when(disableDeviation.execute(any(DisableDeviationInput.class)))
          .thenReturn(DeviationResponse.builder()
              .status(OK.value())
              .message("Forecast deviation disabled")
              .build());

      // WHEN
      whenDisableDeviation();

      // THEN
      verify(authorizeUser).execute(new AuthorizeUserDto(USER_ID,
          singletonList(OUTBOUND_SIMULATION)));

      thenStatusAndMessageAreCorrect(OK,
          "Forecast deviation disabled");
    }

    @Test
    void disableDeviationErrorApi() throws Exception {
      // GIVEN
      when(disableDeviation.execute(any(DisableDeviationInput.class)))
          .thenThrow(RuntimeException.class);

      // WHEN
      whenDisableDeviation();

      // THEN
      thenStatusAndMessageAreCorrect(INTERNAL_SERVER_ERROR,
          "Error disabling forecast deviation");
    }

    @Test
    void disableDeviationUserNoAuthorized() throws Exception {
      // GIVEN
      when(authorizeUser.execute(any(AuthorizeUserDto.class)))
          .thenThrow(UserNotAuthorizedException.class);

      // WHEN
      whenDisableDeviation();

      // THEN
      result.andExpect(status().isForbidden());
      verifyNoInteractions(disableDeviation);
    }

    private void whenDisableDeviation() throws Exception {
      result = mvc.perform(MockMvcRequestBuilders
          .post(format(URL, FBM_WMS_OUTBOUND.getName()) + "/disable")
          .param("caller.id", String.valueOf(USER_ID))
          .param("warehouse_id", WAREHOUSE_ID)
          .contentType(APPLICATION_JSON));
    }
  }

  @Nested
  @DisplayName("Test disable deviation Shipment")
  class DisableDeviationShipmentController {

    @Test
    void disableDeviationShipmentOk() throws Exception {
      // WHEN
      whenDisableDeviationInboundUnits();

      // THEN
      thenStatusAndMessageAreCorrect(OK, "Schedule deviation disable");
    }

    private void whenDisableDeviationInboundUnits() throws Exception {
      result = mvc.perform(MockMvcRequestBuilders
          .post(format(URL, FBM_WMS_INBOUND.getName()) + "/units/disable")
          .param("caller.id", String.valueOf(USER_ID))
          .param("logistic_center_id", WAREHOUSE_ID)
          .param("shipment_types", ShipmentType.COLLECT.getName(), ShipmentType.FTL.getName())
          .contentType(APPLICATION_JSON));
    }
  }

}
