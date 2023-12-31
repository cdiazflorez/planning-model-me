package com.mercadolibre.planning.model.me.clients.tools;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import com.mercadolibre.planning.model.me.gateways.sharedistribution.dto.DistributionElement;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests of ShareDistributionAPI.
 */
@ExtendWith(MockitoExtension.class)
public class ShareDistributionAPITest {

  private static final String WH = "ARTW01";

  @Mock
  private BigqueryWrapper bigQuery;

  @InjectMocks
  private ShareDistributionRepository shareDistributionRepository;

  @Test
  public void metricsAPITest() throws InterruptedException {

    //GIVEN
    ReflectionTestUtils.setField(shareDistributionRepository, "blacklistAreas", List.of("MU"));

    TableResult table = mock(TableResult.class);

    FieldValueList list = mock(FieldValueList.class);
    when(list.get("AREA")).thenReturn(FieldValue.of(FieldValue.Attribute.PRIMITIVE, "MZ-1"));
    when(list.get("WAREHOUSE_ID")).thenReturn(FieldValue.of(FieldValue.Attribute.PRIMITIVE, WH));
    when(list.get("SIs")).thenReturn(FieldValue.of(FieldValue.Attribute.PRIMITIVE, "500"));
    when(list.get("CPT_TIMEZONE")).thenReturn(FieldValue.of(FieldValue.Attribute.PRIMITIVE, "2022-03-28T00:00:00"));

    when(table.iterateAll()).thenReturn(Collections.singleton(list));

    when(bigQuery.query(any(QueryJobConfiguration.class))).thenReturn(table);


    // WHEN
    List<DistributionElement> response =
        shareDistributionRepository.getMetrics(WH, Instant.parse("2022-03-27T00:00:00Z"), Instant.parse("2022-03-29T00:00:00Z"));

    // THEN
    Assertions.assertNotNull(response);

  }

  @Test
  public void metricsAPIFilterTest() throws InterruptedException {

    //GIVEN
    ReflectionTestUtils.setField(shareDistributionRepository, "blacklistAreas", List.of("MU"));

    TableResult table = mock(TableResult.class);

    FieldValueList list = mock(FieldValueList.class);
    when(list.get("AREA")).thenReturn(FieldValue.of(FieldValue.Attribute.PRIMITIVE, "MU-1"));
    when(table.iterateAll()).thenReturn(Collections.singleton(list));
    when(bigQuery.query(any(QueryJobConfiguration.class))).thenReturn(table);

    // WHEN
    List<DistributionElement> response =
        shareDistributionRepository.getMetrics(WH, Instant.parse("2022-03-27T00:00:00Z"), Instant.parse("2022-03-29T00:00:00Z"));

    // THEN
    Assertions.assertTrue(response.isEmpty());

  }

}
