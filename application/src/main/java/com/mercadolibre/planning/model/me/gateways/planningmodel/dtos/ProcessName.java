package com.mercadolibre.planning.model.me.gateways.planningmodel.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.stream.Stream;

public enum ProcessName {

    PICKING(true, false, new Single("PICKING")),
    PACKING_WALL(false, false, new Single("PACKING_WALL")),
    PACKING(
            false,
            false,
            new Multiple(Multiple.Layout.parallel, new Single("PACKING"), PACKING_WALL.graph)
    ),
    WAVING(
            false,
            true,
            new Multiple(Multiple.Layout.serial, PICKING.graph, PACKING.graph)
    ),
    BATCH_SORTER(false, false, new Single("BATCH_SORTER")),
    WALL_IN(false, false, new Single("WALL_IN")),
    GLOBAL(false, false, new Single("GLOBAL"));

    public final boolean hasAreas;

    public final boolean hasTargetBacklog;

    /** The graph of the processes that are considered by the flow-backlog-api when a request for
     * the backlog of this process is sent. */
    public final Graph graph;

    ProcessName(
            final boolean hasAreas,
            final boolean hasTargetBacklog,
            final Graph graph
    ) {
        this.hasAreas = hasAreas;
        this.hasTargetBacklog = hasTargetBacklog;
        this.graph = graph;
    }

    @JsonCreator
    public static ProcessName from(final String value) {
        return valueOf(value.toUpperCase());
    }

    @JsonValue
    public String getName() {
        return name().toLowerCase();
    }

    public boolean hasAreas() {
        return hasAreas;
    }

    public boolean hasTargetBacklog() {
        return hasTargetBacklog;
    }

    /**
     * A directed acyclic graph of the stages of a process.
     */
    public interface Graph {
        /**
         * Gives the stages that constitute this {@link Graph}, flattened and mapped to
         * {@link ProcessName}.
         */
        Stream<ProcessName> flatten();
    }

    /**
     * A single stage {@linkg Graph}.
     */
    @RequiredArgsConstructor
    public static class Single implements Graph {
        private final String name;

        @Override
        public Stream<ProcessName> flatten() {
            return Stream.of(from(name));
        }

        public ProcessName getProcessName() {
            return from(name);
        }
    }

    /**
     * A {@link Graph} that consists of many stages that are all in serial or all in parallel.
     */
    public static class Multiple implements Graph {
        public final Layout layout;
        public final Graph[] graphs;

        public enum Layout { serial, parallel }

        public Multiple(final Layout layout, final Graph... graphs) {
            this.layout = layout;
            this.graphs = graphs;
        }

        @Override
        public Stream<ProcessName> flatten() {
            return Arrays.stream(graphs).flatMap(Graph::flatten);
        }
    }
}
