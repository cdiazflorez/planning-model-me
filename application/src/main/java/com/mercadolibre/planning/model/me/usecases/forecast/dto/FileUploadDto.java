package com.mercadolibre.planning.model.me.usecases.forecast.dto;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import lombok.Value;

@Value
public class FileUploadDto {
    private String warehouseId;

    private Workflow workflow;

    private byte[] bytes;

    private long userId;
}
