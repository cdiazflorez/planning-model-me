package com.mercadolibre.planning.model.me.enums;

import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mercadolibre.planning.model.me.entities.workflows.Step;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;

public enum ProcessName {

  PICKING(true, false, new Single("PICKING"), Step.TO_PICK),
  PACKING_WALL(false, false, new Single("PACKING_WALL"), Step.TO_PACK),
  PACKING(
      false,
      false,
      new Multiple(Multiple.Layout.parallel, new Single("PACKING"), PACKING_WALL.graph),
      Step.TO_PACK
  ),
  WAVING(
      false,
      true,
      new Multiple(Multiple.Layout.serial, PICKING.graph, PACKING.graph),
      Step.PENDING
  ),
  BATCH_SORTER(false, false, new Single("BATCH_SORTER"), Step.TO_SORT),
  WALL_IN(false, false, new Single("WALL_IN"), Step.TO_GROUP),
  GLOBAL(false, false, new Single("GLOBAL")),
  CHECK_IN(false, false, new Single("CHECK_IN"), Step.CHECK_IN),
  SCHEDULED(false, false, new Single("SCHEDULED"), Step.SCHEDULED),
  PUT_AWAY(false, false, new Single("PUT_AWAY"), Step.PUT_AWAY),
  RECEIVING(false, false, new Single("RECEIVING"));

  private static final Map<Step, ProcessName> PROCESS_BY_STEP = Arrays.stream(values())
      .filter(process -> process.getStep() != null && process.getStep() != Step.TO_PACK)
      .collect(
          toMap(ProcessName::getStep, Function.identity())
      );

  public final boolean hasAreas;
  public final boolean hasTargetBacklog;
  /**
   * The graph of the processes that are considered by the flow-backlog-api when a request for
   * the backlog of this process is sent.
   **/
  public final Graph graph;
  public Step step;

  ProcessName(
      final boolean hasAreas,
      final boolean hasTargetBacklog,
      final Graph graph
  ) {
    this.hasAreas = hasAreas;
    this.hasTargetBacklog = hasTargetBacklog;
    this.graph = graph;
  }

  ProcessName(
      final boolean hasAreas,
      final boolean hasTargetBacklog,
      final Graph graph,
      final Step step
  ) {
    this.hasAreas = hasAreas;
    this.hasTargetBacklog = hasTargetBacklog;
    this.graph = graph;
    this.step = step;
  }

  @JsonCreator
  public static ProcessName from(final String value) {
    return valueOf(value.toUpperCase(Locale.getDefault()));
  }

  public static ProcessName getProcessByStep(final Step step) {
    return PROCESS_BY_STEP.get(step);
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

  public Step getStep() {
    return step;
  }


  /**
   * A directed acyclic graph of the stages of a process.
   */
  public interface Graph {
    /**
     * Gives the stages that constitute this {@link Graph}, flattened and mapped to.
     *
     * @return ProcessName
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

    public Multiple(final Layout layout, final Graph... graphs) {
      this.layout = layout;
      this.graphs = graphs;
    }

    @Override
    public Stream<ProcessName> flatten() {
      return Arrays.stream(graphs).flatMap(Graph::flatten);
    }

    public enum Layout {
      serial,
      parallel
    }
  }
}
