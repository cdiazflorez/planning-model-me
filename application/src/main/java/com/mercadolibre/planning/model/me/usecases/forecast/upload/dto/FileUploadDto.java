package com.mercadolibre.planning.model.me.usecases.forecast.upload.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FileUploadDto {
    private String warehouseId;
    private byte[] bytes;
    private long userId;

    public FileUploadDto(final String warehouseId, final byte[] bytes,
                         final long userId) {
        this.warehouseId = warehouseId;
        this.bytes = bytes.clone();
        this.userId = userId;
    }
}
