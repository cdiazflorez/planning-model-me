package com.mercadolibre.planning.model.me.controller.staffing;

import com.mercadolibre.planning.model.me.entities.staffing.Staffing;
import com.mercadolibre.planning.model.me.usecases.authorization.AuthorizeUser;
import com.mercadolibre.planning.model.me.usecases.authorization.dtos.AuthorizeUserDto;
import com.mercadolibre.planning.model.me.usecases.staffing.GetStaffing;
import com.mercadolibre.planning.model.me.usecases.staffing.GetStaffingInput;
import com.newrelic.api.agent.Trace;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

import static com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission.OUTBOUND_PROJECTION;

@RestController
@AllArgsConstructor
@RequestMapping("/wms/flow/middleend/logistic_center_id/{warehouseId}/staffing")
public class StaffingController {

    private final AuthorizeUser authorizeUser;

    private final GetStaffing getStaffing;

    @Trace
    @GetMapping("/current")
    public ResponseEntity<Staffing> getEntities(
            @PathVariable final String warehouseId,
            @RequestParam("caller.id") final long callerId) {

        authorizeUser.execute(new AuthorizeUserDto(callerId, List.of(OUTBOUND_PROJECTION)));

        return ResponseEntity.of(Optional.of(
                getStaffing.execute(new GetStaffingInput(warehouseId))
        ));
    }
}
