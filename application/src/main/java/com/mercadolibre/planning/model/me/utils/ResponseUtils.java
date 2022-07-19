package com.mercadolibre.planning.model.me.utils;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source.FORECAST;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source.SIMULATION;
import static com.mercadolibre.planning.model.me.utils.DateUtils.getHourAndDay;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.capitalize;

import com.mercadolibre.planning.model.me.entities.projection.ColumnHeader;
import com.mercadolibre.planning.model.me.entities.projection.Content;
import com.mercadolibre.planning.model.me.entities.projection.Tab;
import com.mercadolibre.planning.model.me.entities.projection.complextable.ComplexTableAction;
import com.mercadolibre.planning.model.me.entities.projection.complextable.Data;
import com.mercadolibre.planning.model.me.entities.projection.simulationmode.ErrorMessage;
import com.mercadolibre.planning.model.me.entities.projection.simulationmode.SimulationMode;
import com.mercadolibre.planning.model.me.entities.projection.simulationmode.Snackbar;
import com.mercadolibre.planning.model.me.gateways.logisticcenter.dtos.LogisticCenterConfiguration;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityRow;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.RowName;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Source;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class ResponseUtils {

    private static final String FIRST_TITLE = "title_1";

    private static final String FIRST_SUBTITLE = "subtitle_1";

    public static final String PROJECTION_TITLE = "Proyección";

    private static final String NO_VALUE = "-";

    private static final DateTimeFormatter COLUMN_HOUR_FORMAT = ofPattern("HH:00");

    private ResponseUtils() {
    }

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

    public static List<Tab> createOutboundTabs() {
        return List.of(
                new Tab("cpt", "Cumplimiento de CPTs"));
    }

    public static List<Tab> createInboundTabs() {
        return List.of(new Tab("iss", "Vencimiento de ISs"));
    }

    public static final ComplexTableAction action = new ComplexTableAction(
            "Aplicar",
            "Cancelar",
            "Editar"
    );

    public static final SimulationMode simulationMode = new SimulationMode(
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

    //TODO: analizar si vale la pena refactorizar este metodo para que se llame
    // desde las 2 proyecciones
    public static Data createData(final LogisticCenterConfiguration config,
                                  final MagnitudeType magnitudeType,
                                  final List<EntityRow> entities,
                                  final List<ColumnHeader> headers
    ) {

        final Map<RowName, List<EntityRow>> entitiesByProcess = entities.stream()
                .collect(groupingBy(EntityRow::getRowName));

        return new Data(
                magnitudeType.getName(),
                capitalize(magnitudeType.getTitle()),
                true,
                entitiesByProcess.entrySet().stream()
                        .sorted(Comparator.comparing(entry -> entry.getKey().getIndex()))
                        .map(entry -> createContent(
                                config,
                                entry.getKey(),
                                headers,
                                entry.getValue()
                        ))
                        .collect(toList())
        );
    }

    private static Map<String, Content> createContent(final LogisticCenterConfiguration config,
                                                      final RowName processName,
                                                      final List<ColumnHeader> headers,
                                                      final List<EntityRow> entities) {
        final Map<String, Map<Source, EntityRow>> entitiesByHour = entities.stream()
                .map(entity -> entity.convertTimeZone(config.getZoneId()))
                .collect(groupingBy(
                        entity -> getHourAndDay(entity.getDate()),
                        toMap(EntityRow::getSource, identity(), (e1, e2) -> e2)));

        final Map<String, Content> content = new LinkedHashMap<>();

        headers.forEach(header -> {
            if ("column_1".equals(header.getId())) {
                content.put(header.getId(),
                        new Content(capitalize(processName.getTitle()), null, null,
                                processName.getName(), true));
            } else {
                final Map<Source, EntityRow> entityBySource = entitiesByHour.get(header.getValue());

                if (entityBySource != null && entityBySource.containsKey(SIMULATION)) {
                    content.put(header.getId(), new Content(
                            valueOf(entityBySource.get(SIMULATION).getValue()),
                            entityBySource.get(SIMULATION).getDate(),
                            createTooltip(entityBySource.get(FORECAST)),
                            null,
                            entityBySource.get(SIMULATION).isValid()));
                } else if (entityBySource != null && entityBySource.containsKey(FORECAST)) {
                    content.put(header.getId(), new Content(
                            valueOf(entityBySource.get(FORECAST).getValue()),
                            entityBySource.get(FORECAST).getDate(),
                            null,
                            null,
                            entityBySource.get(FORECAST).isValid()));
                } else {
                    content.put(header.getId(),
                            new Content(NO_VALUE, null, null, null, true));
                }
            }
        });
        return content;
    }

    private static Map<String, String> createTooltip(EntityRow entity) {
        return Map.of(
                FIRST_TITLE, "Hora de operación",
                FIRST_SUBTITLE, format("%s - %s",
                        entity.getTime().format(COLUMN_HOUR_FORMAT),
                        entity.getTime().plusHours(1).format(COLUMN_HOUR_FORMAT)),
                "title_2", "Items por hora",
                "subtitle_2", valueOf(entity.getValue())
        );

    }
}
