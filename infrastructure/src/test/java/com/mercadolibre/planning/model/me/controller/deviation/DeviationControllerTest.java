package com.mercadolibre.planning.model.me.controller.deviation;

import static com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission.OUTBOUND_SIMULATION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.INBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.INBOUND_TRANSFER;
import static com.mercadolibre.planning.model.me.utils.TestUtils.USER_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID_ARTW01;
import static com.mercadolibre.planning.model.me.utils.TestUtils.getResourceAsString;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.doThrow;
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
import java.util.List;
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

  private static void thenVerifyStatusAndMessage(
      final ResultActions result,
      final HttpStatus status,
      final String message
  ) throws Exception {
    result.andExpect(status == OK ? status().isOk() : status().isInternalServerError())
        .andExpect(content().json(new JSONObject()
            .put("status", status.value())
            .put("message", message)
            .toString()));
  }

  @BeforeEach
  public void setUp() {
    Mockito.reset(authorizeUser);
    Mockito.reset(saveOutboundDeviation);
    Mockito.reset(disableDeviation);
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
      final var result = whenSaveDeviation();

      // THEN
      verify(authorizeUser).execute(new AuthorizeUserDto(USER_ID, singletonList(OUTBOUND_SIMULATION)));
      thenVerifyStatusAndMessage(result, OK, "Forecast deviation saved");
    }

    @Test
    void saveDeviationErrorApi() throws Exception {
      // GIVEN
      when(saveOutboundDeviation.execute(any(SaveDeviationInput.class)))
          .thenThrow(RuntimeException.class);

      // WHEN
      final var result = whenSaveDeviation();

      // THEN
      verify(authorizeUser).execute(new AuthorizeUserDto(USER_ID, singletonList(OUTBOUND_SIMULATION)));
      thenVerifyStatusAndMessage(result, INTERNAL_SERVER_ERROR, "Error persisting forecast deviation");
    }

    @Test
    void saveDeviationUserNoAuthorized() throws Exception {
      // GIVEN
      when(authorizeUser.execute(any(AuthorizeUserDto.class)))
          .thenThrow(UserNotAuthorizedException.class);

      // WHEN
      final var result = whenSaveDeviation();

      // THEN
      result.andExpect(status().isForbidden());
      verifyNoInteractions(saveOutboundDeviation);
    }

    private ResultActions whenSaveDeviation() throws Exception {
      return mvc.perform(MockMvcRequestBuilders
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
          .warehouseId(WAREHOUSE_ID_ARTW01)
          .dateFrom(ZonedDateTime.parse("2021-01-21T15:00Z[UTC]"))
          .dateTo(ZonedDateTime.parse("2021-01-21T17:00Z[UTC]"))
          .type(DeviationType.UNITS)
          .value(5.9)
          .userId(USER_ID)
          .build();

      // WHEN
      mvc.perform(MockMvcRequestBuilders
          .post(format(URL, FBM_WMS_INBOUND.getName()) + "/save/units")
          .content(getResourceAsString("post_save_deviation_request.json"))
          .contentType(APPLICATION_JSON));

      // THEN
      verify(saveDeviation).execute(saveDeviationInput);
    }

    @Test
    void testSaveDeviationsOk() throws Exception {
      // WHEN
      final var response = mvc.perform(MockMvcRequestBuilders
          .post(format(URL, FBM_WMS_INBOUND.getName()) + "/save/units/all")
          .content(getResourceAsString("post_save_all_deviation_request.json"))
          .contentType(APPLICATION_JSON));

      // THEN
      response.andExpect(status().isNoContent());

      final var dateFrom = ZonedDateTime.parse("2021-01-21T15:00Z[UTC]");
      final var dateTo = ZonedDateTime.parse("2021-01-21T17:00Z[UTC]");

      final List<SaveDeviationInput> input = List.of(
          SaveDeviationInput.builder()
              .warehouseId(WAREHOUSE_ID_ARTW01)
              .workflow(INBOUND)
              .paths(List.of(ShipmentType.SPD, ShipmentType.PRIVATE))
              .dateFrom(dateFrom)
              .dateTo(dateTo)
              .type(DeviationType.UNITS)
              .value(0.1)
              .userId(USER_ID)
              .build(),
          SaveDeviationInput.builder()
              .warehouseId(WAREHOUSE_ID_ARTW01)
              .workflow(INBOUND_TRANSFER)
              .dateFrom(dateFrom)
              .dateTo(dateTo)
              .type(DeviationType.UNITS)
              .value(0.1)
              .userId(USER_ID)
              .build()
      );

      verify(saveDeviation).save(input);
    }

  @Test
  void testSaveDeviationsError() throws Exception {
    // GIVEN
    doThrow(RuntimeException.class).when(saveDeviation).save(anyList());

    // WHEN
    final var response = mvc.perform(MockMvcRequestBuilders
        .post(format(URL, FBM_WMS_INBOUND.getName()) + "/save/units/all")
        .content(getResourceAsString("post_save_all_deviation_request.json"))
        .contentType(APPLICATION_JSON));

    // THEN
    response.andExpect(status().isInternalServerError());
  }

    @Test
    void testSaveDeviationsWithInvalidBody() throws Exception {
      // GIVEN
      doThrow(RuntimeException.class).when(saveDeviation).save(anyList());

      // WHEN
      final var response = mvc.perform(MockMvcRequestBuilders
          .post(format(URL, FBM_WMS_INBOUND.getName()) + "/save/units/all")
          .content("[]")
          .contentType(APPLICATION_JSON));

      // THEN
      response.andExpect(status().isBadRequest());
    }
}

  @Nested
  @DisplayName("Test disable deviation")
  class DisableDeviationController {
    @Test
    void disableDeviationOk() throws Exception {
      // WHEN
      final var result = whenDisableDeviation();

      // THEN
      verify(authorizeUser).execute(new AuthorizeUserDto(USER_ID, singletonList(OUTBOUND_SIMULATION)));
      verify(disableDeviation).execute(argThat(input -> input.getWorkflow().equals(FBM_WMS_OUTBOUND)));
      result.andExpect(status().isOk());
    }

    @Test
    void disableDeviationErrorApi() throws Exception {
      // GIVEN
      doThrow(RuntimeException.class).when(disableDeviation).execute(any(DisableDeviationInput.class));

      // WHEN
      final var result = whenDisableDeviation();

      // THEN
      result.andExpect(status().isInternalServerError());
    }

    @Test
    void disableDeviationUserNoAuthorized() throws Exception {
      // GIVEN
      when(authorizeUser.execute(any(AuthorizeUserDto.class)))
          .thenThrow(UserNotAuthorizedException.class);

      // WHEN
      final var result = whenDisableDeviation();

      // THEN
      result.andExpect(status().isForbidden());
      verifyNoInteractions(disableDeviation);
    }

    private ResultActions whenDisableDeviation() throws Exception {
      return mvc.perform(MockMvcRequestBuilders
          .post(format(URL, FBM_WMS_OUTBOUND.getName()) + "/disable")
          .param("caller.id", String.valueOf(USER_ID))
          .param("warehouse_id", WAREHOUSE_ID_ARTW01)
          .contentType(APPLICATION_JSON));
    }
  }

  @Nested
  @DisplayName("Test disable deviation Shipment")
  class DisableDeviationShipmentController {

    @Test
    void testDisableDeviationShipmentOk() throws Exception {
      // WHEN
      final var result = whenDisableDeviationInboundUnits();

      // THEN
      result.andExpect(status().isOk());

      verify(disableDeviation).execute(argThat(input -> input.getWorkflow().equals(FBM_WMS_INBOUND)));
      verify(authorizeUser).execute(
          argThat(i -> i.getRequiredPermissions()
              .equals(List.of(OUTBOUND_SIMULATION))
          )
      );
    }

    @Test
    void testDisableDeviationShipmentOnErrorShouldReturnADeviationResponse() throws Exception {
      // GIVEN
      doThrow(RuntimeException.class).when(disableDeviation).execute(new DisableDeviationInput(WAREHOUSE_ID_ARTW01, FBM_WMS_INBOUND));

      // WHEN
      final var result = whenDisableDeviationInboundUnits();

      // THEN
      result.andExpect(status().isInternalServerError());
    }

    private ResultActions whenDisableDeviationInboundUnits() throws Exception {
      return mvc.perform(MockMvcRequestBuilders
          .post(format(URL, FBM_WMS_INBOUND.getName()) + "/disable/units")
          .param("caller.id", String.valueOf(USER_ID))
          .param("logistic_center_id", WAREHOUSE_ID_ARTW01)
          .contentType(APPLICATION_JSON));
    }
  }

}
