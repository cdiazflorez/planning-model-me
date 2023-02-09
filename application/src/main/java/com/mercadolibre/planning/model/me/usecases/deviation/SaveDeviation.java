package com.mercadolibre.planning.model.me.usecases.deviation;

import com.mercadolibre.planning.model.me.enums.DeviationType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.DeviationGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.PlanningModelGateway;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SaveDeviationRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.usecases.deviation.dtos.SaveDeviationInput;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Named;
import lombok.AllArgsConstructor;

@Named
@AllArgsConstructor
public class SaveDeviation {
  private static final int EXPECT_VALUE = 100;

  private final PlanningModelGateway planningModelGateway;

  private final DeviationGateway deviationGateway;

  public void execute(final SaveDeviationInput input) {
    validateUnitRangeByWorkflow(input.getType(), input.getWorkflow(), input.getValue());

    planningModelGateway.newSaveDeviation(input);
  }

  public void save(final List<SaveDeviationInput> deviations) {
      deviationGateway.save(deviations);
  }

  private void validateUnitRangeByWorkflow(final DeviationType type, final Workflow workflow, final double value) {
    if (workflow.equals(Workflow.FBM_WMS_INBOUND) && type.equals(DeviationType.UNITS) && (value > EXPECT_VALUE || value < -EXPECT_VALUE)) {
      throw new IllegalArgumentException("The value must be between -100 to 100");
    }
  }
}
