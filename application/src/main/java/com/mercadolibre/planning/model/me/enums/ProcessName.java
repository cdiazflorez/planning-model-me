package com.mercadolibre.planning.model.me.enums;

import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mercadolibre.planning.model.me.entities.workflows.Step;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;

public enum ProcessName {

  PICKING(true, false, new Single("PICKING"), List.of(Step.TO_PICK, Step.TO_ROUTE)),
  PACKING_WALL(false, false, new Single("PACKING_WALL"), List.of(Step.TO_PACK)),
  PACKING(
      false,
      false,
      new Multiple(Multiple.Layout.parallel, new Single("PACKING"), PACKING_WALL.graph),
      List.of(Step.TO_PACK)
  ),
  WAVING(
      false,
      true,
      new Multiple(Multiple.Layout.serial, PICKING.graph, PACKING.graph),
      List.of(Step.PENDING)
  ),
  BATCH_SORTER(false, false, new Single("BATCH_SORTER"), List.of(Step.TO_SORT)),
  WALL_IN(false, false, new Single("WALL_IN"), List.of(Step.TO_GROUP, Step.SORTED, Step.GROUPING)),
  GLOBAL(false, false, new Single("GLOBAL")),
  CHECK_IN(false, false, new Single("CHECK_IN"), List.of(Step.CHECK_IN)),
  SCHEDULED(false, false, new Single("SCHEDULED"), List.of(Step.SCHEDULED)),
  PUT_AWAY(false, false, new Single("PUT_AWAY"), List.of(Step.PUT_AWAY)),
  RECEIVING(false, false, new Single("RECEIVING"));

  private static final Map<ProcessName, List<Step>> PROCESS_BY_STEP = Arrays.stream(values())
      .filter(process -> process.getSteps() != null && !process.getSteps().contains(Step.TO_PACK))
      .collect(
          toMap(Function.identity(), ProcessName::getSteps)
      );

  public final boolean hasAreas;
  public final boolean hasTargetBacklog;
  /**
   * The graph of the processes that are considered by the flow-backlog-api when a request for
   * the backlog of this process is sent.
   **/
  public final Graph graph;
  public List<Step> steps;

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
      final List<Step> step
  ) {
    this.hasAreas = hasAreas;
    this.hasTargetBacklog = hasTargetBacklog;
    this.graph = graph;
    this.steps = step;
  }

  @JsonCreator
  public static ProcessName from(final String value) {
    return valueOf(value.toUpperCase(Locale.getDefault()));
  }

  public static ProcessName getProcessByStep(final Step step) {
    return PROCESS_BY_STEP
        .entrySet().stream()
        .filter(r -> r.getValue().contains(step))
        .findFirst()
        .orElseThrow().getKey();
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

  public List<Step> getSteps() {
    return steps;
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
