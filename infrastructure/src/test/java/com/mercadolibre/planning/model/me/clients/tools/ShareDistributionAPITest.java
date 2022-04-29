package com.mercadolibre.planning.model.me.clients.tools;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.api.gax.paging.Page;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.google.cloud.bigquery.TableResult;
import com.mercadolibre.planning.model.me.gateways.sharedistribution.dto.DistributionElement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests of ShareDistributionAPI. */
@ExtendWith(MockitoExtension.class)
public class ShareDistributionAPITest {

  @Mock
  BigqueryWrapper bigqueryWrapper;

  @Mock
  BigQuery bigQuery;

  @Test
  public void metricsAPITest() throws InterruptedException {

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

    ShareDistributionRepository api = new ShareDistributionRepository(bigqueryWrapper);
    GCPBigQueryWrapper wrapper = new GCPBigQueryWrapper(bigQuery);

    // WHEN
    List<DistributionElement> response =
        api.getMetrics("ARTW01", Instant.parse("2022-03-27T00:00:00Z"), Instant.parse("2022-03-29T00:00:00Z"));
    TableResult result = wrapper.query(QueryJobConfiguration.newBuilder("SELECT * FROM meli-bi-data.WHOWNER.LK_SHP_FACILITIES").build());

    // THEN
    Assertions.assertNotNull(response);
    Assertions.assertNotNull(result);


  }
}
