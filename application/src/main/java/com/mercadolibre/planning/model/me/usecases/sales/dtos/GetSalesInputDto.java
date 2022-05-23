package com.mercadolibre.planning.model.me.usecases.sales.dtos;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Getter
@AllArgsConstructor
@Builder
public class GetSalesInputDto {
    private Workflow workflow;
    private String warehouseId;
    private ZonedDateTime dateCreatedFrom;
    private ZonedDateTime dateCreatedTo;
    private ZonedDateTime dateOutFrom;
    private ZonedDateTime dateOutTo;
    private ZoneId timeZone;
    private Boolean fromDS;

    public GetSalesInputDto(final Workflow workflow,
                            final String warehouseId,
                            final ZonedDateTime dateCreatedFrom,
                            final ZonedDateTime dateCreatedTo,
                            final ZonedDateTime dateOutFrom,
                            final ZonedDateTime dateOutTo) {
        this.workflow = workflow;
        this.warehouseId = warehouseId;
        this.dateCreatedFrom = dateCreatedFrom;
        this.dateCreatedTo = dateCreatedTo;
        this.dateOutFrom = dateOutFrom;
        this.dateOutTo = dateOutTo;
    }
}
