package com.mercadolibre.planning.model.me.controller;

import com.mercadolibre.planning.model.me.enums.ProcessName;
import java.time.ZonedDateTime;
import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class GetBacklogProjectionRequest {

    @NotBlank
    private String warehouseId;

    private List<ProcessName> processName;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private ZonedDateTime dateFrom;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private ZonedDateTime dateTo;

  /*  public BacklogProjectionInput getBacklogProjectionInput(final Workflow workflow,
                                                            final long callerId) {
        return BacklogProjectionInput.builder()
                .warehouseId(warehouseId)
                .workflow(workflow)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .userId(callerId)
                .processName(processName)
                .groupType("order")
                .build();
    }*/
}
