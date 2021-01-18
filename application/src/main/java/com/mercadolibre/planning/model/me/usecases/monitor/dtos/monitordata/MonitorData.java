package com.mercadolibre.planning.model.me.usecases.monitor.dtos.monitordata;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
public abstract class MonitorData {

    protected String type;

}
