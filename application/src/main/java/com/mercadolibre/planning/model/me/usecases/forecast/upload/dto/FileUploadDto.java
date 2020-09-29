package com.mercadolibre.planning.model.me.usecases.forecast.upload.dto;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileUploadDto {
    private String warehouseId;
    private byte[] bytes;

    public FileUploadDto(final String warehouseId, final byte[] bytes) {
        this.warehouseId = warehouseId;
        this.bytes = bytes.clone();
    }
}