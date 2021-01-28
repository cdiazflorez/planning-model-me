package com.mercadolibre.planning.model.me.utils;

import com.mercadolibre.planning.model.me.entities.projection.ColumnHeader;
import com.mercadolibre.planning.model.me.entities.projection.Tab;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTableAction;
import com.mercadolibre.planning.model.me.entities.projection.simulationmode.ErrorMessage;
import com.mercadolibre.planning.model.me.entities.projection.simulationmode.SimulationMode;
import com.mercadolibre.planning.model.me.entities.projection.simulationmode.Snackbar;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static com.mercadolibre.planning.model.me.utils.DateUtils.getHourAndDay;
import static java.lang.String.format;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.stream.Collectors.toList;

public class ResponseUtils {

    private static final DateTimeFormatter COLUMN_HOUR_FORMAT = ofPattern("HH:00");

    public static List<ColumnHeader> createColumnHeaders(final ZonedDateTime dateFrom,
                                                         final int hoursToShow) {

        final List<ColumnHeader> columns = new ArrayList<>(hoursToShow);

        columns.add(new ColumnHeader("column_1", "Horas de Operación", null));
        columns.addAll(IntStream.range(0, hoursToShow)
                .mapToObj(index -> {
                    final ZonedDateTime date = dateFrom.plusHours(index);
                    return new ColumnHeader(
                            format("column_%s", 2 + index),
                            date.format(COLUMN_HOUR_FORMAT),
                            getHourAndDay(date));
                }).collect(toList()));

        return columns;
    }

    public static List<Tab> createTabs() {
        return List.of(
                new Tab("cpt", "Cumplimiento de CPTs"),
                new Tab("backlog", "Backlogs"));
    }

    public static ComplexTableAction action = new ComplexTableAction(
            "Aplicar",
            "Cancelar",
            "Editar"
    );

    public static SimulationMode simulationMode = new SimulationMode(
            "Iniciar Simulación",
            new Snackbar(
                    "Simulación en curso",
                    "Guardar",
                    "Cancelar"
            ),
            new ErrorMessage(
                    "No pudimos aplicar la simulacion",
                    "No pudimos guardar la simulacion"
            )
    );
}
