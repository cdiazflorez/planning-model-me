package com.mercadolibre.planning.model.me.clients.tools;

import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import com.mercadolibre.planning.model.me.gateways.sharedistribution.ShareDistributionGateway;
import com.mercadolibre.planning.model.me.gateways.sharedistribution.dto.DistributionElement;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * Implements consult to Bigquery.
 */
@Slf4j
@Component
@RefreshScope
@RequiredArgsConstructor
public class ShareDistributionRepository implements ShareDistributionGateway {


  private static final String PATTERN = "yyyy-MM-dd HH:mm:ss";

  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneOffset.ofHours(-4));

  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(PATTERN);

  private final BigqueryWrapper bigQuery;

  @Value("${blacklist.areas.share.distributions:MU,CA}")
  private List<String> blacklistAreas;


  @Override
  public List<DistributionElement> getMetrics(String wareHouseId, Instant startDate, Instant endDate) {

    ZonedDateTime dateFrom = ZonedDateTime.ofInstant(startDate, ZoneOffset.ofHours(-4));
    ZonedDateTime dateTo = ZonedDateTime.ofInstant(endDate, ZoneOffset.ofHours(-4));


    String dateToStr = dateTo.format(DATE_TIME_FORMATTER);
    String dateFromStr = dateFrom.format(DATE_TIME_FORMATTER);

    String query =
        "SELECT "
            + "  COUNT(A1.FBM_QUANTITY) AS SIs, "
            + "  SUBSTRING(A1.ADDRESS_ID_FROM,0,4) AS AREA, "
            + "  A1.WAREHOUSE_ID, "
            + "  CAST(A2.OUTBOUND.DEPARTURE_ESTIMATED_DATE AS DATETIME) AS CPT_TIMEZONE "
            + "FROM "
            + "  `meli-bi-data.WHOWNER.BT_FBM_STOCK_MOVEMENT` AS A1 "
            + "LEFT JOIN "
            + "  `meli-bi-data.SHIPPING_BI.BT_SHP_MT_OUTBOUND` AS A2 "
            + "ON "
            + "  CAST (A1.SHP_SHIPMENT_ID AS STRING) = A2.ID "
            + "WHERE "
            + "  A1.WAREHOUSE_ID = '"
            + wareHouseId
            + "'"
            + "  AND A1.FBM_PROCESS_NAME = 'PICKING' "
            + "  AND  CAST(A2.OUTBOUND.DEPARTURE_ESTIMATED_DATE AS DATETIME) BETWEEN DATETIME'"
            + dateFromStr
            + "'AND DATETIME '"
            + dateToStr
            + "'"
            + "  AND A1.SHP_SHIPMENT_ID IS NOT NULL "
            + "GROUP BY 2,3,4 "
            + "ORDER BY 3,4";
    return queryRun(query);
  }

  public List<DistributionElement> queryRun(String query) {

    List<DistributionElement> distributionList = new ArrayList<>();

    try {

      QueryJobConfiguration queryConfig =
          QueryJobConfiguration.newBuilder(query).build();

      TableResult result = bigQuery.query(queryConfig);

      result.iterateAll().forEach(rows -> {
            String area = rows.get("AREA").getStringValue().split("-")[0];
            if (!blacklistAreas.contains(area)) {
              distributionList.add(
                  DistributionElement.builder().warehouseID(rows.get("WAREHOUSE_ID").getStringValue())
                      .cptTime(ZonedDateTime.parse(rows.get("CPT_TIMEZONE").getStringValue(),
                          TIME_FORMATTER).withZoneSameInstant(ZoneOffset.UTC))
                      .area(rows.get("AREA").getStringValue())
                      .sis(rows.get("SIs").getLongValue())
                      .build()
              );
            }
          }
      );
    } catch (BigQueryException | InterruptedException e) {
      log.error(e.getMessage(), e);
    }
    return distributionList;
  }
}
