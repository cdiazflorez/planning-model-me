package com.mercadolibre.planning.model.me.clients.tools;

import com.google.cloud.bigquery.*;
import com.mercadolibre.planning.model.me.gateways.sharedistribution.ShareDistributionGateway;
import com.mercadolibre.planning.model.me.gateways.sharedistribution.dto.DistributionResponse;
import com.mercadolibre.planning.model.me.utils.DateUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
@AllArgsConstructor
public class ShareDistributionAPI implements ShareDistributionGateway {


  private static final String PATTERN = "yyyy-MM-dd HH:mm:ss";

  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneOffset.ofHours(-5));

  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(PATTERN);

  private final BigqueryWrapper bigQuery;

  @Override
  public List<DistributionResponse> getMetrics(String wareHouseId) {

    ZonedDateTime dateTo = DateUtils.getCurrentUtcDate().withZoneSameInstant(ZoneOffset.ofHours(-5));
    ZonedDateTime dateFrom = dateTo.minusMonths(1);

    String dateToStr = dateTo.format(DATE_TIME_FORMATTER);
    String dateFromStr = dateFrom.format(DATE_TIME_FORMATTER);

    String query =
        "SELECT " +
            "  COUNT(A1.FBM_QUANTITY) AS SIs, " +
            "  SUBSTRING(A1.ADDRESS_ID_FROM,0,4) AS AREA, " +
            "  A1.WAREHOUSE_ID, " +
            "  CAST(A2.OUTBOUND.DEPARTURE_ESTIMATED_DATE AS DATETIME) AS CPT_TIMEZONE " +
            "FROM " +
            "  `meli-bi-data.WHOWNER.BT_FBM_STOCK_MOVEMENT` AS A1 " +
            "LEFT JOIN " +
            "  `meli-bi-data.SHIPPING_BI.BT_SHP_MT_OUTBOUND` AS A2 " +
            "ON " +
            "  CAST (A1.SHP_SHIPMENT_ID AS STRING) = A2.ID " +
            "WHERE " +
            "  A1.WAREHOUSE_ID = '" + wareHouseId + "'" +
            "  AND A1.FBM_PROCESS_NAME = 'PICKING' " +
            "  AND  CAST(A2.OUTBOUND.DEPARTURE_ESTIMATED_DATE AS DATETIME) BETWEEN DATETIME'" + dateFromStr + "'AND DATETIME '" +
            dateToStr + "'" +
            "  AND A1.SHP_SHIPMENT_ID IS NOT NULL " +
            "GROUP BY 2,3,4 " +
            "ORDER BY 3,4";
    return queryRun(query);
  }

  public List<DistributionResponse> queryRun(String query) {

    List<DistributionResponse> distributionList = new ArrayList<>();

    try {

      QueryJobConfiguration queryConfig =
          QueryJobConfiguration.newBuilder(query).build();

      TableResult result = bigQuery.query(queryConfig);

      result.iterateAll().forEach(rows -> {
            distributionList.add(
                DistributionResponse.builder().warehouseID(rows.get("WAREHOUSE_ID").getStringValue())
                    .cptTime(ZonedDateTime.parse(rows.get("CPT_TIMEZONE").getStringValue(), TIME_FORMATTER).withZoneSameInstant(ZoneOffset.UTC))
                    .area(rows.get("AREA").getStringValue())
                    .sis(rows.get("SIs").getLongValue())
                    .build()
            );
          }
      );
    } catch (BigQueryException | InterruptedException e) {
      log.error(e.getMessage(), e);
    }
    return distributionList;
  }
}
