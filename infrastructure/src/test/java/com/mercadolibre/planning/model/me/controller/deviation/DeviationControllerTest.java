package com.mercadolibre.planning.model.me.controller.deviation;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.INBOUND;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.INBOUND_TRANSFER;
import static com.mercadolibre.planning.model.me.utils.TestUtils.USER_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID_ARTW01;
import static com.mercadolibre.planning.model.me.utils.TestUtils.getResourceAsString;
import static java.lang.String.format;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mercadolibre.planning.model.me.enums.DeviationType;
import com.mercadolibre.planning.model.me.enums.ShipmentType;
import com.mercadolibre.planning.model.me.metric.DatadogMetricService;
import com.mercadolibre.planning.model.me.usecases.authorization.AuthorizeUser;
import com.mercadolibre.planning.model.me.usecases.authorization.dtos.AuthorizeUserDto;
import com.mercadolibre.planning.model.me.usecases.authorization.exceptions.UserNotAuthorizedException;
import com.mercadolibre.planning.model.me.usecases.deviation.DisableDeviation;
import com.mercadolibre.planning.model.me.usecases.deviation.SaveDeviation;
import com.mercadolibre.planning.model.me.usecases.deviation.dtos.DisableDeviationInput;
import com.mercadolibre.planning.model.me.usecases.deviation.dtos.SaveDeviationInput;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(controllers = DeviationController.class)
public class DeviationControllerTest {

    private static final String URL = "/planning/model/middleend/workflows/%s/deviations";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private SaveDeviation saveDeviation;

    @MockBean
    private DisableDeviation disableDeviation;

    @MockBean
    private AuthorizeUser authorizeUser;

    @MockBean
    private DatadogMetricService datadogMetricService;

    @BeforeEach
    public void setUp() {
        Mockito.reset(authorizeUser);
        Mockito.reset(disableDeviation);
    }

    @Nested
    @DisplayName("Test save deviation")
    class SaveDeviationShipmentController {
        @Test
        void testSaveDeviationsOk() throws Exception {
            // WHEN
            final var response = whenSaveDeviation();

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

            verify(saveDeviation).execute(input);
        }

        @Test
        void testSaveDeviationsError() throws Exception {
            // GIVEN
            doThrow(RuntimeException.class).when(saveDeviation).execute(anyList());

            // WHEN
            final var response = whenSaveDeviation();

            // THEN
            response.andExpect(status().isInternalServerError());
        }

        @Test
        void testSaveDeviationsWithInvalidBody() throws Exception {
            // GIVEN
            doThrow(RuntimeException.class).when(saveDeviation).execute(anyList());

            // WHEN
            final var response = mvc.perform(MockMvcRequestBuilders
                .post(format(URL, "INBOUND") + "/save/units/all")
                .param("caller.id", String.valueOf(USER_ID))
                .param("logistic_center_id", WAREHOUSE_ID_ARTW01)
                .content("[]")
                .contentType(APPLICATION_JSON));

            // THEN
            response.andExpect(status().isBadRequest());
        }

        private ResultActions whenSaveDeviation() throws Exception {
            return mvc.perform(MockMvcRequestBuilders
                .post(format(URL, "INBOUND") + "/save/units/all")
                .param("caller.id", String.valueOf(USER_ID))
                .param("logistic_center_id", WAREHOUSE_ID_ARTW01)
                .content(getResourceAsString("post_save_all_deviation_request.json"))
                .contentType(APPLICATION_JSON));
        }
    }

    @Nested
    @DisplayName("Test disable deviation")
    class DisableDeviationController {
        @Test
        void testDisableDeviationsOk() throws Exception {
            // WHEN
            final var response = whenDisableDeviation();

            // THEN
            response.andExpect(status().isNoContent());

            final List<ShipmentType> affectedShipmentTypes = List.of(ShipmentType.SPD, ShipmentType.PRIVATE);

            verify(disableDeviation)
                .execute(eq(WAREHOUSE_ID_ARTW01), Mockito.argThat(res -> compareArguments(affectedShipmentTypes, res)));
        }

        @Test
        void disableDeviationErrorApi() throws Exception {
            // GIVEN
            doThrow(RuntimeException.class).when(disableDeviation).execute(eq(WAREHOUSE_ID_ARTW01), any(List.class));

            // WHEN
            final var result = mvc.perform(MockMvcRequestBuilders
                .post(format(URL, "INBOUND") + "/disable/minutes/all")
                .param("caller.id", String.valueOf(USER_ID))
                .param("logistic_center_id", WAREHOUSE_ID_ARTW01)
                .contentType(APPLICATION_JSON));

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
                .post(format(URL, "INBOUND") + "/disable/minutes/all")
                .param("caller.id", String.valueOf(USER_ID))
                .param("logistic_center_id", WAREHOUSE_ID_ARTW01)
                .content(getResourceAsString("post_disable_all_deviation_request.json"))
                .contentType(APPLICATION_JSON));
        }

        private boolean compareArguments(final List<ShipmentType> affectedShipmentTypes, final List<DisableDeviationInput> actual) {
            return INBOUND.equals(actual.get(0).getWorkflow())
                && INBOUND_TRANSFER.equals(actual.get(1).getWorkflow())
                && DeviationType.MINUTES.equals(actual.get(0).getType())
                && DeviationType.MINUTES.equals(actual.get(1).getType())
                && affectedShipmentTypes.equals(actual.get(0).getAffectedShipmentTypes());
        }
    }
}
