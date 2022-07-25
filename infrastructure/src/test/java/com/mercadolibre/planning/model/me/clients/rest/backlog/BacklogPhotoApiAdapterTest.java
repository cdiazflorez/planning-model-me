package com.mercadolibre.planning.model.me.clients.rest.backlog;

import static com.mercadolibre.planning.model.me.gateways.backlog.dto.Process.PACKING;
import static com.mercadolibre.planning.model.me.gateways.backlog.dto.Process.PACKING_WALL;
import static com.mercadolibre.planning.model.me.gateways.backlog.dto.Process.PICKING;
import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.entities.workflows.Area;
import com.mercadolibre.planning.model.me.entities.workflows.BacklogWorkflow;
import com.mercadolibre.planning.model.me.entities.workflows.Step;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogPhotosRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Photo;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Process;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.services.backlog.BacklogGrouper;
import com.mercadolibre.planning.model.me.services.backlog.BacklogRequest;
import com.mercadolibre.planning.model.me.usecases.BacklogPhoto;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    final BacklogPhotosRequest photoRequestMock = createBacklogPhotoRequest(Set.of(Step.TO_PICK, Step.TO_PACK));
    when(backlogApiGateway.getPhotos(photoRequestMock))
        .thenReturn(mockPhotos());

    final BacklogRequest backlogPhotoRequest = createRequest(Set.of(Process.PICKING, PACKING_WALL));

    // WHEN
    final Map<Process, List<BacklogPhoto>> backlogByProcess =
        backlogPhotoApiAdapter.getTotalBacklogPerProcessAndInstantDate(backlogPhotoRequest);

    // THEN
    Assertions.assertEquals(expectedSummaryBacklog(), backlogByProcess);
  }

  @Test
  void testExecuteBacklogDetails() {
    // GIVEN
    final BacklogPhotosRequest photoRequestMock = createBacklogPhotoRequest(Set.of(Step.TO_PICK, Step.TO_PACK));
    when(backlogApiGateway.getPhotos(photoRequestMock))
        .thenReturn(mockPhotos());

    final BacklogRequest backlogPhotoRequest = createRequest(Set.of(Process.PICKING, Process.PACKING, PACKING_WALL));

    // WHEN
    final Map<Process, List<Photo>> backlogByProcess = backlogPhotoApiAdapter.getBacklogDetails(backlogPhotoRequest);

    // THEN
    Assertions.assertEquals(expectedBacklogDetails(), backlogByProcess);
  }

  private BacklogRequest createRequest(Set<Process> processes) {
    return new BacklogRequest(
        WAREHOUSE_ID,
        Set.of(Workflow.FBM_WMS_OUTBOUND),
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
        WAREHOUSE_ID,
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

  private Map<Process, List<BacklogPhoto>> expectedSummaryBacklog() {

    return Map.of(
        Process.PICKING,
        List.of(
            new BacklogPhoto(Instant.parse("2022-06-08T00:00:00Z"), 110),
            new BacklogPhoto(Instant.parse("2022-06-08T01:00:00Z"), 55)
        ),
        Process.PACKING,
        List.of(
            new BacklogPhoto(Instant.parse("2022-06-08T00:00:00Z"), 200),
            new BacklogPhoto(Instant.parse("2022-06-08T01:00:00Z"), 100)
        ),
        PACKING_WALL,
        List.of(
            new BacklogPhoto(Instant.parse("2022-06-08T00:00:00Z"), 50),
            new BacklogPhoto(Instant.parse("2022-06-08T01:00:00Z"), 10)
        )
    );
  }

  private Map<Process, List<Photo>> expectedBacklogDetails() {
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
                        100
                    ),

                    new Photo.Group(
                        Map.of(BacklogGrouper.STEP, "TO_PICK",
                            BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z",
                            BacklogGrouper.AREA, "HV"),
                        10
                    )
                )
            ),
            new Photo(
                Instant.parse("2022-06-08T01:00:00Z"),
                List.of(
                    new Photo.Group(
                        Map.of(BacklogGrouper.STEP, "TO_PICK",
                            BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z"),
                        50
                    ),

                    new Photo.Group(
                        Map.of(BacklogGrouper.STEP, "TO_PICK",
                            BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z",
                            BacklogGrouper.AREA, "HV"),
                        5
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
                        200
                    )
                )
            ),
            new Photo(
                Instant.parse("2022-06-08T01:00:00Z"),
                List.of(
                    new Photo.Group(
                        Map.of(BacklogGrouper.STEP, "TO_PACK",
                            BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z"),
                        100
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
                        50
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
                        10
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
                    100
                ),

                new Photo.Group(
                    Map.of(BacklogGrouper.STEP, "TO_PICK",
                        BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z",
                        BacklogGrouper.AREA, "HV"),
                    10
                ),

                new Photo.Group(
                    Map.of(BacklogGrouper.STEP, "TO_PACK",
                        BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z",
                        BacklogGrouper.AREA, "AREA"),
                    200
                ),

                new Photo.Group(
                    Map.of(BacklogGrouper.STEP, "TO_PACK",
                        BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z",
                        BacklogGrouper.AREA, Area.PW.getName()),
                    50
                )
            )
        ),

        new Photo(
            Instant.parse("2022-06-08T01:00:00Z"),
            List.of(
                new Photo.Group(
                    Map.of(BacklogGrouper.STEP, "TO_PICK",
                        BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z"),
                    50
                ),

                new Photo.Group(
                    Map.of(BacklogGrouper.STEP, "TO_PICK",
                        BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z",
                        BacklogGrouper.AREA, "HV"),
                    5
                ),

                new Photo.Group(
                    Map.of(BacklogGrouper.STEP, "TO_PACK",
                        BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z"),
                    100
                ),

                new Photo.Group(
                    Map.of(BacklogGrouper.STEP, "TO_PACK",
                        BacklogGrouper.DATE_OUT, "2022-06-09T00:00:00Z",
                        BacklogGrouper.AREA, Area.PW.getName()),
                    10
                )
            )
        )
    );
  }

}
