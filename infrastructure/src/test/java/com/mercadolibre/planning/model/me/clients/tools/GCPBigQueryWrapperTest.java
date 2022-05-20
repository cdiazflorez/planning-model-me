package com.mercadolibre.planning.model.me.clients.tools;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class GCPBigQueryWrapperTest {

  @Mock
  private BigQuery bigQuery;

  @InjectMocks
  private GCPBigQueryWrapper gcpBigQueryWrapper;

  @Test
  public void queryTest() {

    try {
      //GIVEN
      TableResult table = mock(TableResult.class);

      QueryJobConfiguration configuration =
          QueryJobConfiguration.newBuilder("SELECT * FROM meli-bi-data.WHOWNER.LK_SHP_FACILITIES")
              .build();

      when(bigQuery.query(any(QueryJobConfiguration.class))).thenReturn(table);

      //WHEN
      TableResult response = gcpBigQueryWrapper.query(configuration);

      //THEN
      Assertions.assertNotNull(response);
      verify(bigQuery).query(configuration);

    } catch (InterruptedException e) {
      log.error(e.getMessage(), e);
      Assertions.fail("Unexpected exception was thrown");
    }
  }
}
