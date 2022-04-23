package com.mercadolibre.planning.model.me.clients.tools;

import com.google.api.gax.paging.Page;
import com.google.cloud.bigquery.*;
import com.mercadolibre.planning.model.me.gateways.sharedistribution.dto.DistributionResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ShareDistributionAPITest {

  @Mock
  BigqueryWrapper bigqueryWrapper;

  @Mock
  BigQuery bigQuery;

  @Test
  public void getMetricsTest() throws InterruptedException {

    //GIVEN
    Field[] fields = {Field.newBuilder("WAREHOUSE_ID", StandardSQLTypeName.STRING).build(),
        Field.newBuilder("CPT_TIMEZONE", StandardSQLTypeName.STRING).build(),
        Field.newBuilder("AREA", StandardSQLTypeName.STRING).build(),
        Field.newBuilder("SIs", StandardSQLTypeName.NUMERIC).build()};

    Schema schema = Schema.of(fields);

    TableResult tableResult = new TableResult(schema, 1, new Page<FieldValueList>() {
      @Override
      public boolean hasNextPage() {
        return false;
      }

      @Override
      public String getNextPageToken() {
        return null;
      }

      @Override
      public Page<FieldValueList> getNextPage() {
        return null;
      }

      @Override
      public Iterable<FieldValueList> iterateAll() {
        List<FieldValue> fieldValueList = new ArrayList<>();
        fieldValueList.add(FieldValue.of(FieldValue.Attribute.PRIMITIVE, "ARBA01"));
        fieldValueList.add(FieldValue.of(FieldValue.Attribute.PRIMITIVE, "2022-03-28T00:00:00"));
        fieldValueList.add(FieldValue.of(FieldValue.Attribute.PRIMITIVE, "MZ-1"));
        fieldValueList.add(FieldValue.of(FieldValue.Attribute.PRIMITIVE, "500"));

        FieldValueList list = FieldValueList.of(fieldValueList);

        return Collections.singleton(list);
      }

      @Override
      public Iterable<FieldValueList> getValues() {
        return null;
      }
    });

    when(bigqueryWrapper.query(any(QueryJobConfiguration.class))).thenReturn(tableResult);
    when(bigQuery.query(any(QueryJobConfiguration.class))).thenReturn(tableResult);

    ShareDistributionAPI api = new ShareDistributionAPI(bigqueryWrapper);
    GCPBigQueryWrapper wrapper = new GCPBigQueryWrapper(bigQuery);

    // WHEN
    List<DistributionResponse> response = api.getMetrics("ARTW01");
    TableResult result = wrapper.query(QueryJobConfiguration.newBuilder("SELECT * FROM meli-bi-data.WHOWNER.LK_SHP_FACILITIES").build());

    // THEN
    Assertions.assertNotNull(response);
    Assertions.assertNotNull(result);


  }
}
