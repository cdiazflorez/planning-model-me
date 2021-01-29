package com.mercadolibre.planning.model.me.controller.deviation;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.DeviationResponse;
import com.mercadolibre.planning.model.me.usecases.authorization.AuthorizeUser;
import com.mercadolibre.planning.model.me.usecases.authorization.dtos.AuthorizeUserDto;
import com.mercadolibre.planning.model.me.usecases.authorization.exceptions.UserNotAuthorizedException;
import com.mercadolibre.planning.model.me.usecases.deviation.SaveDeviation;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission.OUTBOUND_SIMULATION;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.me.utils.TestUtils.USER_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.getResourceAsString;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DeviationController.class)
public class DeviationControllerTest {

    private static final String URL = "/planning/model/middleend/workflows/%s/deviations";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private SaveDeviation saveDeviation;

    @MockBean
    private AuthorizeUser authorizeUser;

    private ResultActions result;

    @Test
    void saveDeviationOk() throws Exception {
        // GIVEN
        given(saveDeviation.execute(any()))
                .willReturn(DeviationResponse.builder()
                        .status(HttpStatus.OK.value())
                        .message("Forecast deviation saved")
                        .build());

        // WHEN
        whenSaveDeviation();

        // THEN
        verify(authorizeUser).execute(new AuthorizeUserDto(USER_ID,
                singletonList(OUTBOUND_SIMULATION)));

        thenStatusAndMessageAreCorrect(HttpStatus.OK,
                "Forecast deviation saved");
    }

    @Test
    void saveDeviationErrorApi() throws Exception {
        // GIVEN
        given(saveDeviation.execute(any()))
                .willReturn(DeviationResponse.builder()
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .message("Error forecast deviation")
                        .build());

        // WHEN
        whenSaveDeviation();

        // THEN
        thenStatusAndMessageAreCorrect(HttpStatus.INTERNAL_SERVER_ERROR,
                "Error forecast deviation");
    }

    @Test
    void saveDeviationUserNoAuthorized() throws Exception {
        // GIVEN
        when(authorizeUser.execute(any(AuthorizeUserDto.class)))
                .thenThrow(UserNotAuthorizedException.class);

        given(saveDeviation.execute(any()))
                .willReturn(DeviationResponse.builder()
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .message("Error forecast deviation")
                        .build());

        // WHEN
        whenSaveDeviation();

        // THEN
        result.andExpect(status().isForbidden());
        verifyNoInteractions(saveDeviation);
    }

    private void whenSaveDeviation() throws Exception {
        result = mvc.perform(MockMvcRequestBuilders
                .post(format(URL, FBM_WMS_OUTBOUND.getName()) + "/save")
                .content(getResourceAsString("post_save_deviation_request.json"))
                .contentType(APPLICATION_JSON));
    }

    private void thenStatusAndMessageAreCorrect(final HttpStatus status,
                                                final String message) throws Exception {
        result.andExpect(status().isOk())
                .andExpect(content().json(new JSONObject()
                        .put("status", status.value())
                        .put("message", message)
                        .toString()));
    }
}
