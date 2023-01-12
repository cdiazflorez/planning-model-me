package com.mercadolibre.planning.model.me.clients.rest.backlog;

import static com.mercadolibre.planning.model.me.enums.ProcessName.CHECK_IN;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.enums.ProcessName.PUT_AWAY;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID_ARTW01;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.entities.workflows.Area;
import com.mercadolibre.planning.model.me.entities.workflows.BacklogWorkflow;
import com.mercadolibre.planning.model.me.entities.workflows.Step;
import com.mercadolibre.planning.model.me.enums.ProcessName;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogPhotosRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Photo;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper;
import com.mercadolibre.planning.model.me.services.backlog.BacklogRequest;
import com.mercadolibre.planning.model.me.usecases.BacklogPhoto;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BacklogPhotoApiAdapterTest {
  private static final Instant DATE_FROM = Instant.parse("2022-06-22T00:00:00Z");

  private static final Instant DATE_TO = Instant.parse("2022-06-24T00:00:00Z");

  @InjectMocks
  private BacklogPhotoApiAdapter backlogPhotoApiAdapter;

  @Mock
  private BacklogApiGateway backlogApiGateway;

  @Test
  void testExecuteBacklogSummary() {
    // GIVEN
    final BacklogPhotosRequest photoRequestMock = createBacklogPhotoRequest(Set.of(Step.TO_PICK, Step.TO_ROUTE, Step.TO_PACK));
    when(backlogApiGateway.getPhotos(photoRequestMock))
        .thenReturn(mockPhotos());

    final BacklogRequest backlogPhotoRequest = createRequest(Set.of(PICKING, PACKING, PACKING_WALL), Workflow.FBM_WMS_OUTBOUND);

    // WHEN
    final Map<ProcessName, List<BacklogPhoto>> backlogByProcess =
        backlogPhotoApiAdapter.getTotalBacklogPerProcessAndInstantDate(backlogPhotoRequest, false);

    // THEN
    final var expected = expectedSummaryBacklog();

    assertionsSummary(expected, backlogByProcess);
  }

  @Test
  void testExecuteBacklogInboundSummary() {
    // GIVEN
    final BacklogPhotosRequest photoRequestMock = createBacklogPhotoInboundRequest(Set.of(Step.CHECK_IN, Step.PUT_AWAY));
    when(backlogApiGateway.getPhotos(photoRequestMock))
        .thenReturn(mockInboundPhotos());

    final BacklogRequest backlogPhotoRequest = createRequest(Set.of(CHECK_IN, PUT_AWAY), Workflow.FBM_WMS_INBOUND);

    // WHEN
    final Map<ProcessName, List<BacklogPhoto>> backlogByProcess =
        backlogPhotoApiAdapter.getTotalBacklogPerProcessAndInstantDate(backlogPhotoRequest, false);

    // THEN
    Assertions.assertEquals(2, backlogByProcess.size());

    final var putAway = backlogByProcess.get(PUT_AWAY);
    Assertions.assertEquals(250, putAway.get(0).getQuantity());
    Assertions.assertEquals(150, putAway.get(1).getQuantity());

    final var checkIn = backlogByProcess.get(CHECK_IN);
    Assertions.assertEquals(110, checkIn.get(0).getQuantity());
    Assertions.assertEquals(200, checkIn.get(1).getQuantity());
  }
  @Test
  void testExecuteBacklogSummaryCached() {
    // GIVEN
    final BacklogPhotosRequest photoRequestMock = createBacklogPhotoRequest(Set.of(Step.TO_PICK, Step.TO_ROUTE, Step.TO_PACK));
    when(backlogApiGateway.getPhotosCached(photoRequestMock))
        .thenReturn(mockPhotos());

    final BacklogRequest backlogPhotoRequest = createRequest(Set.of(PICKING, PACKING, PACKING_WALL), Workflow.FBM_WMS_OUTBOUND);

    // WHEN
    final Map<ProcessName, List<BacklogPhoto>> backlogByProcess =
        backlogPhotoApiAdapter.getTotalBacklogPerProcessAndInstantDate(backlogPhotoRequest, true);

    // THEN
    final var expected = expectedSummaryBacklog();

    assertionsSummary(expected, backlogByProcess);
  }

  @Test
  void testExecuteBacklogDetails() {
    // GIVEN
    final BacklogPhotosRequest photoRequestMock = createBacklogPhotoRequest(Set.of(Step.TO_PICK, Step.TO_ROUTE, Step.TO_PACK));
    when(backlogApiGateway.getPhotos(photoRequestMock))
        .thenReturn(mockPhotos());

    final BacklogRequest backlogPhotoRequest = createRequest(Set.of(PICKING, PACKING, PACKING_WALL), Workflow.FBM_WMS_OUTBOUND);

    // WHEN
    final Map<ProcessName, List<Photo>> backlogByProcess = backlogPhotoApiAdapter.getBacklogDetails(backlogPhotoRequest);

    // THEN
    Assertions.assertEquals(3, backlogByProcess.size());

    final var expected = expectedBacklogDetails();

    Assertions.assertEquals(expected.get(PICKING).size(), backlogByProcess.get(PICKING).size());
    Assertions.assertEquals(expected.get(PACKING).size(), backlogByProcess.get(PACKING).size());
    Assertions.assertEquals(expected.get(PACKING_WALL).size(), backlogByProcess.get(PACKING_WALL).size());
    Assertions.assertEquals(sortedPhotoByTakenOn(expected.get(PICKING)), sortedPhotoByTakenOn(backlogByProcess.get(PICKING)));
    Assertions.assertEquals(sortedPhotoByTakenOn(expected.get(PACKING)), sortedPhotoByTakenOn(backlogByProcess.get(PACKING)));
    Assertions.assertEquals(sortedPhotoByTakenOn(expected.get(PACKING_WALL)), sortedPhotoByTakenOn(backlogByProcess.get(PACKING_WALL)));
  }

  private void assertionsSummary(
      final Map<ProcessName, List<BacklogPhoto>> expected,
      final Map<ProcessName, List<BacklogPhoto>> backlogByProcess) {

    Assertions.assertEquals(expected.get(PICKING).size(), backlogByProcess.get(PICKING).size());
    Assertions.assertEquals(expected.get(PACKING).size(), backlogByProcess.get(PACKING).size());
    Assertions.assertEquals(expected.get(PACKING_WALL).size(), backlogByProcess.get(PACKING_WALL).size());
    Assertions.assertEquals(sortedBacklogPhotoByTakenOn(expected.get(PICKING)), sortedBacklogPhotoByTakenOn(backlogByProcess.get(PICKING)));
    Assertions.assertEquals(sortedBacklogPhotoByTakenOn(expected.get(PACKING)), sortedBacklogPhotoByTakenOn(backlogByProcess.get(PACKING)));
    Assertions.assertEquals(sortedBacklogPhotoByTakenOn(expected.get(PACKING_WALL)),
        sortedBacklogPhotoByTakenOn(backlogByProcess.get(PACKING_WALL)));
  }

  private BacklogRequest createRequest(Set<ProcessName> processes, Workflow workflow) {
    return new BacklogRequest(
        WAREHOUSE_ID_ARTW01,
        Set.of(workflow),
        processes,
        DATE_FROM,
        DATE_TO,
        DATE_FROM,
        DATE_TO,
        DATE_FROM,
        DATE_TO,
        Set.of(BacklogGrouper.STEP, BacklogGrouper.AREA, BacklogGrouper.DATE_OUT)
    );
  }

  private BacklogPhotosRequest createBacklogPhotoRequest(Set<Step> steps) {
    return new BacklogPhotosRequest(
        WAREHOUSE_ID_ARTW01,
        Set.of(BacklogWorkflow.OUTBOUND_ORDERS),
        steps,
        DATE_FROM,
        DATE_TO,
        DATE_FROM,
        DATE_TO,
        Set.of(BacklogGrouper.STEP, BacklogGrouper.AREA, BacklogGrouper.DATE_OUT),
        DATE_FROM,
        DATE_TO
    );
  }

  private BacklogPhotosRequest createBacklogPhotoInboundRequest(Set<Step> steps) {
    return new BacklogPhotosRequest(
        WAREHOUSE_ID_ARTW01,
        Set.of(BacklogWorkflow.INBOUND, BacklogWorkflow.INBOUND_TRANSFER),
        steps,
        DATE_FROM,
        DATE_TO,
        DATE_FROM,
        DATE_TO,
        Set.of(BacklogGrouper.STEP, BacklogGrouper.AREA, BacklogGrouper.DATE_OUT),
        DATE_FROM,
        DATE_TO
    );
  }

  private Map<ProcessName, List<BacklogPhoto>> expectedSummaryBacklog() {

    return Map.of(
        PICKING,
        List.of(
            new BacklogPhoto(Instant.parse("2022-06-08T00:00:00Z"), 110),
            new BacklogPhoto(Instant.parse("2022-06-08T01:00:00Z"), 55),
            new BacklogPhoto(Instant.parse("2022-06-08T01:30:00Z"), 33)
        ),
        PACKING,
        List.of(
            new BacklogPhoto(Instant.parse("2022-06-08T00:00:00Z"), 200),
            new BacklogPhoto(Instant.parse("2022-06-08T01:00:00Z"), 100),
            new BacklogPhoto(Instant.parse("2022-06-08T01:30:00Z"), 20)
        ),
        PACKING_WALL,
        List.of(
            new BacklogPhoto(Instant.parse("2022-06-08T00:00:00Z"), 50),
            new BacklogPhoto(Instant.parse("2022-06-08T01:00:00Z"), 10),
            new BacklogPhoto(Instant.parse("2022-06-08T01:30:00Z"), 2)
        )
    );
  }

  private Map<ProcessName, List<Photo>> expectedBacklogDetails() {
    return Map.of(
        PICKING,
        List.of(
            new Photo(
                Instant.parse("2022-06-08T00:00:00Z"),
                List.of(
                    new Photo.Group(
                        Map.of(BacklogGrouper.STEP, "TO_PICK",
                            BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z",
                            BacklogGrouper.AREA, "MZ"),
                        100,
                        1000
                    ),

                    new Photo.Group(
                        Map.of(BacklogGrouper.STEP, "TO_PICK",
                            BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z",
                            BacklogGrouper.AREA, "HV"),
                        10,
                        1000
                    )
                )
            ),
            new Photo(
                Instant.parse("2022-06-08T01:00:00Z"),
                List.of(
                    new Photo.Group(
                        Map.of(BacklogGrouper.STEP, "TO_PICK",
                            BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z"),
                        50,
                        500
                    ),

                    new Photo.Group(
                        Map.of(BacklogGrouper.STEP, "TO_PICK",
                            BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z",
                            BacklogGrouper.AREA, "HV"),
                        5,
                        50
                    )
                )
            )
        ),
        PACKING,
        List.of(
            new Photo(
                Instant.parse("2022-06-08T00:00:00Z"),
                List.of(
                    new Photo.Group(
                        Map.of(BacklogGrouper.STEP, "TO_PACK",
                            BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z",
                            BacklogGrouper.AREA, "AREA"),
                        200,
                        2000
                    )
                )
            ),
            new Photo(
                Instant.parse("2022-06-08T01:00:00Z"),
                List.of(
                    new Photo.Group(
                        Map.of(BacklogGrouper.STEP, "TO_PACK",
                            BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z"),
                        100,
                        1000
                    )
                )
            )
        ),
        PACKING_WALL,
        List.of(
            new Photo(
                Instant.parse("2022-06-08T00:00:00Z"),
                List.of(
                    new Photo.Group(
                        Map.of(BacklogGrouper.STEP, "TO_PACK",
                            BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z",
                            BacklogGrouper.AREA, Area.PW.getName()),
                        50,
                        500
                    )
                )
            ),
            new Photo(
                Instant.parse("2022-06-08T01:00:00Z"),
                List.of(
                    new Photo.Group(
                        Map.of(BacklogGrouper.STEP, "TO_PACK",
                            BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z",
                            BacklogGrouper.AREA, Area.PW.getName()),
                        10,
                        100
                    )
                )
            )
        )
    );

  }

  private List<Photo> mockPhotos() {
    return List.of(
        new Photo(
            Instant.parse("2022-06-08T00:00:00Z"),
            List.of(
                new Photo.Group(
                    Map.of(BacklogGrouper.STEP, "TO_PICK",
                        BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z",
                        BacklogGrouper.AREA, "MZ"),
                    100,
                    1000
                ),

                new Photo.Group(
                    Map.of(BacklogGrouper.STEP, "TO_PICK",
                        BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z",
                        BacklogGrouper.AREA, "HV"),
                    10,
                    1000
                ),

                new Photo.Group(
                    Map.of(BacklogGrouper.STEP, "TO_PACK",
                        BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z",
                        BacklogGrouper.AREA, "AREA"),
                    200,
                    2000
                ),

                new Photo.Group(
                    Map.of(BacklogGrouper.STEP, "TO_PACK",
                        BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z",
                        BacklogGrouper.AREA, Area.PW.getName()),
                    50,
                    500
                )
            )
        ),

        new Photo(
            Instant.parse("2022-06-08T00:30:00Z"),
            List.of(
                new Photo.Group(
                    Map.of(BacklogGrouper.STEP, "TO_PICK",
                        BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z",
                        BacklogGrouper.AREA, "MZ"),
                    100,
                    1000
                ),

                new Photo.Group(
                    Map.of(BacklogGrouper.STEP, "TO_PICK",
                        BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z",
                        BacklogGrouper.AREA, "HV"),
                    10,
                    1000
                ),

                new Photo.Group(
                    Map.of(BacklogGrouper.STEP, "TO_PACK",
                        BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z",
                        BacklogGrouper.AREA, "AREA"),
                    200,
                    2000
                ),

                new Photo.Group(
                    Map.of(BacklogGrouper.STEP, "TO_PACK",
                        BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z",
                        BacklogGrouper.AREA, Area.PW.getName()),
                    50,
                    500
                )
            )
        ),

        new Photo(
            Instant.parse("2022-06-08T01:30:00Z"),
            List.of(
                new Photo.Group(
                    Map.of(BacklogGrouper.STEP, "TO_PICK",
                        BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z"),
                    30,
                    500
                ),

                new Photo.Group(
                    Map.of(BacklogGrouper.STEP, "TO_PICK",
                        BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z",
                        BacklogGrouper.AREA, "HV"),
                    3,
                    50
                ),

                new Photo.Group(
                    Map.of(BacklogGrouper.STEP, "TO_PACK",
                        BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z"),
                    20,
                    1000
                ),

                new Photo.Group(
                    Map.of(BacklogGrouper.STEP, "TO_PACK",
                        BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z",
                        BacklogGrouper.AREA, Area.PW.getName()),
                    2,
                    100
                )
            )
        ),

        new Photo(
            Instant.parse("2022-06-08T01:00:00Z"),
            List.of(
                new Photo.Group(
                    Map.of(BacklogGrouper.STEP, "TO_PICK",
                        BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z"),
                    50,
                    500
                ),

                new Photo.Group(
                    Map.of(BacklogGrouper.STEP, "TO_PICK",
                        BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z",
                        BacklogGrouper.AREA, "HV"),
                    5,
                    50
                ),

                new Photo.Group(
                    Map.of(BacklogGrouper.STEP, "TO_PACK",
                        BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z"),
                    100,
                    1000
                ),

                new Photo.Group(
                    Map.of(BacklogGrouper.STEP, "TO_PACK",
                        BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z",
                        BacklogGrouper.AREA, Area.PW.getName()),
                    10,
                    100
                )
            )
        )
    );

  }


  private List<Photo> mockInboundPhotos() {
    final String CHECK_IN = "CHECK_IN";
    final String PUT_AWAY = "PUT_AWAY";
    final String NA = "N/A";
    return List.of(
        new Photo(
            Instant.parse("2022-06-08T00:00:00Z"),
            List.of(
                new Photo.Group(
                    Map.of(BacklogGrouper.STEP, CHECK_IN,
                        BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z",
                        BacklogGrouper.AREA, NA),
                    100,
                    1000
                ),

                new Photo.Group(
                    Map.of(BacklogGrouper.STEP, CHECK_IN,
                        BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z",
                        BacklogGrouper.AREA, NA),
                    10,
                    1000
                ),

                new Photo.Group(
                    Map.of(BacklogGrouper.STEP, PUT_AWAY,
                        BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z",
                        BacklogGrouper.AREA, NA),
                    200,
                    2000
                ),

                new Photo.Group(
                    Map.of(BacklogGrouper.STEP, PUT_AWAY,
                        BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z",
                        BacklogGrouper.AREA, NA),
                    50,
                    500
                )
            )
        ),

        new Photo(
            Instant.parse("2022-06-08T00:30:00Z"),
            List.of(
                new Photo.Group(
                    Map.of(BacklogGrouper.STEP, CHECK_IN,
                        BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z",
                        BacklogGrouper.AREA, NA),
                    190,
                    1000
                ),

                new Photo.Group(
                    Map.of(BacklogGrouper.STEP, CHECK_IN,
                        BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z",
                        BacklogGrouper.AREA, NA),
                    10,
                    1000
                ),

                new Photo.Group(
                    Map.of(BacklogGrouper.STEP, PUT_AWAY,
                        BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z",
                        BacklogGrouper.AREA, NA),
                    100,
                    2000
                ),

                new Photo.Group(
                    Map.of(BacklogGrouper.STEP, PUT_AWAY,
                        BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z",
                        BacklogGrouper.AREA, NA),
                    50,
                    500
                )
            )
        )
    );

  }

  private List<BacklogPhoto> sortedBacklogPhotoByTakenOn(final List<BacklogPhoto> backlogPhotos) {
    return backlogPhotos.stream().sorted(Comparator.comparing(BacklogPhoto::getTakenOn)).collect(Collectors.toList());
  }

  private List<Photo> sortedPhotoByTakenOn(final List<Photo> photos) {
    return photos.stream().sorted(Comparator.comparing(Photo::getTakenOn)).collect(Collectors.toList());
  }


}
