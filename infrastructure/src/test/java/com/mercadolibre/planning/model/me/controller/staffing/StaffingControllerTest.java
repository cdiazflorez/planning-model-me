package com.mercadolibre.planning.model.me.controller.staffing;

import com.mercadolibre.planning.model.me.entities.staffing.Process;
import com.mercadolibre.planning.model.me.entities.staffing.Staffing;
import com.mercadolibre.planning.model.me.entities.staffing.StaffingWorkflow;
import com.mercadolibre.planning.model.me.entities.staffing.Worker;
import com.mercadolibre.planning.model.me.usecases.authorization.AuthorizeUser;
import com.mercadolibre.planning.model.me.usecases.authorization.dtos.AuthorizeUserDto;
import com.mercadolibre.planning.model.me.usecases.staffing.GetStaffing;
import com.mercadolibre.planning.model.me.usecases.staffing.GetStaffingInput;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;
import java.util.Map;

import static com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission.OUTBOUND_PROJECTION;
import static com.mercadolibre.planning.model.me.utils.TestUtils.USER_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.me.utils.TestUtils.getResourceAsString;
import static java.lang.String.format;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StaffingController.class)
public class StaffingControllerTest {

    private static final String URL = "/wms/flow/middleend/logistic_center_id/%s/staffing";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthorizeUser authorizeUser;

    @MockBean
    private GetStaffing getStaffing;

    @Test
    public void testGetStaffing() throws Exception {
        // GIVEN
        when(getStaffing.execute(new GetStaffingInput(WAREHOUSE_ID))).thenReturn(mockStaffing());

        // WHEN
        final ResultActions result = mockMvc.perform(MockMvcRequestBuilders
                .get(format(URL, WAREHOUSE_ID) + "/current")
                .param("caller.id", String.valueOf(USER_ID))
                .contentType(APPLICATION_JSON)
        );

        // THEN
        result.andExpect(status().isOk());
        result.andExpect(content().json(getResourceAsString("get_staffing_response.json")));

        verify(authorizeUser).execute(new AuthorizeUserDto(USER_ID, List.of(OUTBOUND_PROJECTION)));
    }

    private Staffing mockStaffing() {
        return Staffing.builder()
                .globalNetProductivity(34)
                .totalWorkers(310)
                .plannedWorkers(320)
                .workflows(List.of(
                        StaffingWorkflow.builder()
                                .workflow("fbm-wms-outbound-order")
                                .totalWorkers(100)
                                .globalNetProductivity(60)
                                .totalNonSystemicWorkers(10)
                                .processes(List.of(
                                        Process.builder()
                                                .process("picking")
                                                .netProductivity(40)
                                                .workers(new Worker(10, 30))
                                                .targetProductivity(50)
                                                .throughput(1200)
                                                .build(),
                                        Process.builder()
                                                .process("packing")
                                                .netProductivity(40)
                                                .workers(new Worker(10, 30))
                                                .targetProductivity(50)
                                                .throughput(1200)
                                                .build()
                                        )
                                )
                                .build(),
                        StaffingWorkflow.builder()
                                .workflow("fbm-wms-outbound-withdrawal")
                                .totalWorkers(100)
                                .globalNetProductivity(60)
                                .totalNonSystemicWorkers(10)
                                .processes(List.of(
                                        Process.builder()
                                                .process("picking")
                                                .netProductivity(40)
                                                .workers(new Worker(10, 30))
                                                .targetProductivity(50)
                                                .throughput(1200)
                                                .build(),
                                        Process.builder()
                                                .process("expedition")
                                                .netProductivity(40)
                                                .workers(new Worker(10, 30))
                                                .throughput(1200)
                                                .targetProductivity(50)
                                                .build()
                                        )
                                )
                                .build(),
                        StaffingWorkflow.builder()
                                .workflow("fbm-wms-inbound")
                                .totalWorkers(100)
                                .globalNetProductivity(60)
                                .totalNonSystemicWorkers(10)
                                .processes(List.of(
                                        Process.builder()
                                                .process("receiving")
                                                .netProductivity(40)
                                                .workers(new Worker(10, 30))
                                                .targetProductivity(50)
                                                .throughput(1200)
                                                .build(),
                                        Process.builder()
                                                .process("stage_in")
                                                .netProductivity(40)
                                                .workers(new Worker(10, 30))
                                                .targetProductivity(50)
                                                .throughput(1200)
                                                .build()
                                        )
                                )
                                .build()
                ))
                .build();
    }
}
