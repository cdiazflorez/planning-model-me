package com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.function.Function;

@Getter
@AllArgsConstructor
public enum Productivity {
    RECEIVING(Process.RECEIVING,
            reps -> reps.getActiveRepsReceiving().getValue().equals(0)
                    ? 0
                    : reps.getReceivingWorkload().getValue() / reps.getActiveRepsReceiving().getValue()),
    CHECK_IN(Process.CHECK_IN,
            reps -> reps.getActiveRepsCheckIn().getValue().equals(0)
                    ? 0
                    : reps.getCheckInWorkload().getValue() / reps.getActiveRepsCheckIn().getValue()
    ),
    PUT_AWAY(Process.PUT_AWAY,
            reps -> reps.getActiveRepsPutAway().getValue().equals(0)
                    ? 0
                    : reps.getPutAwayWorkload().getValue() / reps.getActiveRepsPutAway().getValue()

    );

    private Process process;
    private Function<RepsRow, Integer> mapper;

}
