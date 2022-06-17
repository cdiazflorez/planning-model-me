package com.mercadolibre.planning.model.me.services.backlog;

import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogPhotoRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Photo;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName;
import com.mercadolibre.planning.model.me.usecases.backlog.TotaledBacklogPhoto;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BacklogPhotoApiAdapterTest {

  @InjectMocks
  private BacklogPhotoApiAdapter backlogPhotoApiAdapter;

  @Mock
  private BacklogApiGateway backlogApiGateway;

  @Test
  void testExecuteCurrentBacklog() {
    final BacklogPhotoRequest backlogPhotoRequest = new BacklogPhotoRequest();
    backlogPhotoRequest.setProcesses(List.of("waving", "picking"));

    when(backlogApiGateway.getPhotos(backlogPhotoRequest))
        .thenReturn(mockPhotos());

    final Map<ProcessName, List<TotaledBacklogPhoto>> backlogByProcess = backlogPhotoApiAdapter.getCurrentBacklog(backlogPhotoRequest);

    Assertions.assertEquals(expectedCurrentBacklog(), backlogByProcess);
  }

  @Test
  void testExecuteHistoryBacklog() {
    final BacklogPhotoRequest backlogPhotoRequest = new BacklogPhotoRequest();
    backlogPhotoRequest.setProcesses(List.of("waving", "picking"));

    when(backlogApiGateway.getPhotos(backlogPhotoRequest))
        .thenReturn(mockPhotos());

    final Map<ProcessName, Map<Instant, Integer>> backlogByProcess = backlogPhotoApiAdapter.getHistoryBacklog(backlogPhotoRequest);

    Assertions.assertEquals(expectedHistoryBacklog(), backlogByProcess);
  }

  private Map<ProcessName, List<TotaledBacklogPhoto>> expectedCurrentBacklog() {
    return Map.of(
        ProcessName.WAVING,
        List.of(
            new TotaledBacklogPhoto(Instant.parse("2022-06-08T00:00:00Z"), 100),
            new TotaledBacklogPhoto(Instant.parse("2022-06-08T01:00:00Z"), 50)
        ),
        ProcessName.PICKING,
        List.of(
            new TotaledBacklogPhoto(Instant.parse("2022-06-08T00:00:00Z"), 200),
            new TotaledBacklogPhoto(Instant.parse("2022-06-08T01:00:00Z"), 100)
        )
    );
  }

  private Map<ProcessName, Map<Instant, Integer>> expectedHistoryBacklog() {
    return Map.of(
        ProcessName.WAVING,
        Map.of(
            Instant.parse("2022-06-08T00:00:00Z"), 100,
            Instant.parse("2022-06-08T01:00:00Z"), 50
        ),
        ProcessName.PICKING,
        Map.of(
            Instant.parse("2022-06-08T00:00:00Z"), 200,
            Instant.parse("2022-06-08T01:00:00Z"), 100
        )
    );
  }

  private List<Photo> mockPhotos() {
    return List.of(
        new Photo(
            Instant.parse("2022-06-08T00:00:00Z"),
            List.of(
                new Photo.Group(
                    Map.of("status", "PENDING"),
                    100
                ),

                new Photo.Group(
                    Map.of("status", "TO_PICK"),
                    200
                )
            )
        ),

        new Photo(
            Instant.parse("2022-06-08T01:00:00Z"),
            List.of(
                new Photo.Group(
                    Map.of("status", "PENDING"),
                    50
                ),

                new Photo.Group(
                    Map.of("status", "TO_PICK"),
                    100
                )
            )
        )
    );
  }

}
