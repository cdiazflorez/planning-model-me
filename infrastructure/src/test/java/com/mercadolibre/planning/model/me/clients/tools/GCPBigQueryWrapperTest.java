package com.mercadolibre.planning.model.me.clients.tools;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GCPBigQueryWrapperTest {

  @Mock
  private BigQuery bigQuery;

  @InjectMocks
  GCPBigQueryWrapper gcpBigQueryWrapper;

  @Test
  public void queryTest() {

    try {
      //GIVEN
      TableResult table = mock(TableResult.class);

      when(bigQuery.query(any(QueryJobConfiguration.class))).thenReturn(table);

      //WHEN
      TableResult response = gcpBigQueryWrapper.query(QueryJobConfiguration.newBuilder("SELECT * FROM meli-bi-data.WHOWNER.LK_SHP_FACILITIES").build());

      //THEN
      Assertions.assertNotNull(response);


    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
