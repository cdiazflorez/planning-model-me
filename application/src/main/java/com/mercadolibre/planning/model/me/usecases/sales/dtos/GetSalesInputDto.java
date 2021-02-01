package com.mercadolibre.planning.model.me.usecases.sales.dtos;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.ZonedDateTime;

@Getter
@AllArgsConstructor
public class GetSalesInputDto {
    private Workflow workflow;
    private String warehouseId;
    private ZonedDateTime dateCreatedFrom;
    private ZonedDateTime dateCreatedTo;
    private ZonedDateTime dateOutFrom;
    private ZonedDateTime dateOutTo;

    public GetSalesInputDto(Workflow workflow, String warehouseId, ZonedDateTime dateCreatedFrom) {
        this.workflow = workflow;
        this.warehouseId = warehouseId;
        this.dateCreatedFrom = dateCreatedFrom;
    }
}
