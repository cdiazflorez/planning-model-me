package com.mercadolibre.planning.model.me.usecases.throughput;

import com.mercadolibre.planning.model.me.gateways.planningmodel.ProcessVisitor;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Entity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;

import javax.inject.Named;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.GLOBAL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.WALL_IN;

@Named
class ThroughputResultMapper
        implements ProcessVisitor<Map<ProcessName, List<Entity>>, Map<ZonedDateTime, Integer>> {
    @Override
    public Map<ZonedDateTime, Integer> visitWaving(Map<ProcessName, List<Entity>> entities) {
        final Map<ZonedDateTime, Integer> packing = visitPacking(entities);

        return entities.get(PICKING)
                .stream()
                .collect(Collectors.toMap(
                        Entity::getDate,
                        e -> Math.min(
                                e.getValue(), packing.getOrDefault(e.getDate(), 0)
                        )
                ));
    }

    @Override
    public Map<ZonedDateTime, Integer> visitPicking(Map<ProcessName, List<Entity>> entities) {
        return mapDefault(entities, PICKING);
    }

    @Override
    public Map<ZonedDateTime, Integer> visitBatchSorter(
            Map<ProcessName, List<Entity>> entities) {

        return mapDefault(entities, BATCH_SORTER);
    }

    @Override
    public Map<ZonedDateTime, Integer> visitWallIn(Map<ProcessName, List<Entity>> entities) {
        return mapDefault(entities, WALL_IN);
    }

    @Override
    public Map<ZonedDateTime, Integer> visitPacking(Map<ProcessName, List<Entity>> entities) {
        final Map<ZonedDateTime, Integer> packing = mapDefault(entities, PACKING);

        return entities.get(PACKING_WALL)
                .stream()
                .collect(Collectors.toMap(Entity::getDate,
                        e -> e.getValue() + packing.getOrDefault(e.getDate(), 0))
                );
    }

    @Override
    public Map<ZonedDateTime, Integer> visitPackingWall(
            Map<ProcessName, List<Entity>> entities) {

        return mapDefault(entities, PACKING_WALL);
    }

    @Override
    public Map<ZonedDateTime, Integer> visitGlobal(Map<ProcessName, List<Entity>> entities) {
        return mapDefault(entities, GLOBAL);
    }

    public Map<ZonedDateTime, Integer> mapDefault(
            Map<ProcessName, List<Entity>> entities,
            ProcessName process) {

        return entities.get(process)
                .stream()
                .collect(Collectors.toMap(Entity::getDate, Entity::getValue));
    }
}
