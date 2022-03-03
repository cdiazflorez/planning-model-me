package com.mercadolibre.planning.model.me.controller.staffing;

import static com.mercadolibre.planning.model.me.gateways.authorization.dtos.UserPermission.OUTBOUND_PROJECTION;

import com.mercadolibre.planning.model.me.entities.staffing.PlannedHeadcount;
import com.mercadolibre.planning.model.me.entities.staffing.Staffing;
import com.mercadolibre.planning.model.me.usecases.authorization.AuthorizeUser;
import com.mercadolibre.planning.model.me.usecases.authorization.dtos.AuthorizeUserDto;
import com.mercadolibre.planning.model.me.usecases.staffing.GetPlannedHeadcount;
import com.mercadolibre.planning.model.me.usecases.staffing.GetStaffing;
import com.mercadolibre.planning.model.me.usecases.staffing.dtos.GetPlannedHeadcountInput;
import com.mercadolibre.planning.model.me.usecases.staffing.dtos.GetStaffingInput;
import com.newrelic.api.agent.Trace;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for Staffing Monitor endpoints "/current" and "/plan".
 */
@RestController
@AllArgsConstructor
@RequestMapping("/wms/flow/middleend/logistic_center_id/{warehouseId}/staffing")
public class StaffingController {

  private final AuthorizeUser authorizeUser;

  private final GetStaffing getStaffing;

  private final GetPlannedHeadcount getPlannedHeadcount;

  /**
   * Get the last hour metrics from workflows, processes, and areas for the given warehouse.
   * @param warehouseId to get metrics for
   * @param callerId for authorization
   * @return a {@link ResponseEntity} of {@link Staffing}
   */
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

  /**
   * Get the planned metrics Processes for the day.
   * @param warehouseId to get metrics for
   * @param callerId for authorization
   * @return a {@link ResponseEntity} of {@link PlannedHeadcount}
   */
  @Trace
  @GetMapping("/plan")
  public ResponseEntity<PlannedHeadcount> getPlannedHeadcount(
      @PathVariable final String warehouseId,
      @RequestParam("caller.id") final long callerId) {

    authorizeUser.execute(new AuthorizeUserDto(callerId, List.of(OUTBOUND_PROJECTION)));

    return ResponseEntity.of(Optional.of(
        getPlannedHeadcount.execute(new GetPlannedHeadcountInput(warehouseId))
    ));
  }
}
