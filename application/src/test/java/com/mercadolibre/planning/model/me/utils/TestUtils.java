package com.mercadolibre.planning.model.me.utils;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.HEADCOUNT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.MagnitudeType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;
import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyList;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProductivityRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.QuantityByDate;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Simulation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SimulationEntity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.Area;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.ProcessTotals;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.StaffingProcess;
import com.mercadolibre.planning.model.me.gateways.staffing.dtos.response.Totals;
import com.mercadolibre.spreadsheet.MeliDocument;
import com.mercadolibre.spreadsheet.MeliDocumentFactory;
import com.mercadolibre.spreadsheet.MeliSheet;
import com.mercadolibre.spreadsheet.implementations.poi.PoiDocument;
import com.mercadolibre.spreadsheet.implementations.poi.PoiMeliDocumentFactory;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;

/**
 *
 */
public class TestUtils {

  public static final String WAREHOUSE_ID = "ARTW01";

  public static final Workflow WORKFLOW = FBM_WMS_OUTBOUND;

  public static final ZonedDateTime A_DATE = ZonedDateTime.of(2020, 8, 19, 17, 40, 0, 0, UTC);

  public static final Long USER_ID = 1234L;

  public static final String ORDER_GROUP_TYPE = "order";

  public static final String OUTBOUND_WORKFLOW = "fbm-wms-outbound";

  public static final String INBOUND_WORKFLOW = "fbm-wms-inbound";

  public static final String WITHDRAWALS_WORKFLOW = "fbm-wms-withdrawals";

  public static final String RECEIVING_PROCESS = "receiving";

  public static final String CHECK_IN_PROCESS = "check_in";

  public static final String PUT_AWAY_PROCESS = "put_away";

  public static final String PICKING_PROCESS = "picking";

  public static final String BATCH_SORTER_PROCESS = "batch_sorter";

  public static final String WALL_IN_PROCESS = "wall_in";

  public static final String PACKING_PROCESS = "packing";

  public static final String PACKING_WALL_PROCESS = "packing_wall";

  public static final String AREA_MZ1 = "MZ-1";

  public static final String AREA_RKL = "RK-L";

  public static final int INBOUND_IDLE_WORKERS = 2;

  public static final int INBOUND_SYS_WORKERS = 18;

  public static final int OUTBOUND_IDLE_WORKERS = 12;

  public static final int OUTBOUND_SYS_WORKERS = 70;

  public static final int OUTBOUND_NS_WORKERS = 20;

  public static final int WITHDRAWALS_IDLE_WORKERS = 12;

  public static final int WITHDRAWALS_SYS_WORKERS = 18;

  public static final int WITHDRAWALS_NS_WORKERS = 24;

  public static final Double RECEIVING_NET_PRODUCTIVITY = 25.40;

  public static final int RECEIVING_SYS_WORKERS = 3;

  public static final Double RECEIVING_THROUGHPUT = 1.10;

  public static final int CHECK_IN_SYS_WORKERS = 6;

  public static final Double CHECK_IN_NET_PRODUCTIVITY = 35.50;

  public static final Double CHECK_IN_THROUGHPUT = 11.10;

  public static final int PUT_AWAY_SYS_WORKERS = 9;

  public static final Double PUT_AWAY_NET_PRODUCTIVITY = 15.32;

  public static final Double PUT_AWAY_THROUGHPUT = 21.10;

  public static final int PUT_AWAY_MZ1_SYS_WORKERS = 5;

  public static final Double PUT_AWAY_MZ1_NET_PRODUCTIVITY = 32.4;

  public static final Double PUT_AWAY_MZ1_THROUGHPUT = 231.3;

  public static final int PUT_AWAY_RKL_SYS_WORKERS = 4;

  public static final Double PUT_AWAY_RKL_NET_PRODUCTIVITY = 22.14;

  public static final Double PUT_AWAY_RKL_THROUGHPUT = 57.3;

  public static final int OUTBOUND_PICKING_IDLE_WORKERS = 6;

  public static final int OUTBOUND_PICKING_SYS_WORKERS = 20;

  public static final Double OUTBOUND_PICKING_NET_PRODUCTIVITY = 45.71;

  public static final Double OUTBOUND_PICKING_THROUGHPUT = 2700.0;

  public static final int OUTBOUND_PICKING_MZ1_IDLE_WORKERS = 4;

  public static final int OUTBOUND_PICKING_MZ1_SYS_WORKERS = 10;

  public static final Double OUTBOUND_PICKING_MZ1_NET_PRODUCTIVITY = 60.33;

  public static final Double OUTBOUND_PICKING_MZ1_THROUGHPUT = 345.3;

  public static final int OUTBOUND_PICKING_RKL_IDLE_WORKERS = 2;

  public static final int OUTBOUND_PICKING_RKL_SYS_WORKERS = 10;

  public static final Double OUTBOUND_PICKING_RKL_NET_PRODUCTIVITY = 75.42;

  public static final Double OUTBOUND_PICKING_RKL_THROUGHPUT = 119.3;

  public static final int OUTBOUND_PACKING_SYS_WORKERS = 15;

  public static final Double OUTBOUND_PACKING_EFF_PRODUCTIVITY = 34.5;

  public static final Double OUTBOUND_PACKING_THROUGHPUT = 1350.13;

  public static final int OUTBOUND_PACKING_WALL_IDLE_WORKERS = 5;

  public static final int OUTBOUND_PACKING_WALL_SYS_WORKERS = 35;

  public static final Double OUTBOUND_PACKING_WALL_EFF_PRODUCTIVITY = 32.4;

  public static final Double OUTBOUND_PACKING_WALL_THROUGHPUT = 11.10;

  public static final int WITHDRAWALS_PICKING_IDLE_WORKERS = 10;

  public static final int WITHDRAWALS_PICKING_SYS_WORKERS = 14;

  public static final Double WITHDRAWALS_PICKING_NET_PRODUCTIVITY = 71.45;

  public static final Double WITHDRAWALS_PICKING_THROUGHPUT = 270.0;

  public static final int WITHDRAWALS_PICKING_RKL_IDLE_WORKERS = 10;

  public static final int WITHDRAWALS_PICKING_RKL_SYS_WORKERS = 14;

  public static final Double WITHDRAWALS_PICKING_RKL_NET_PRODUCTIVITY = 71.45;

  public static final Double WITHDRAWALS_PICKING_RKL_THROUGHPUT = 270.0;

  public static final int WITHDRAWALS_PACKING_IDLE_WORKERS = 2;

  public static final int WITHDRAWALS_PACKING_SYS_WORKERS = 4;

  public static final Double WITHDRAWALS_PACKING_EFF_PRODUCTIVITY = 54.3;

  public static final Double WITHDRAWALS_PACKING_THROUGHPUT = 350.13;

  private static final MeliDocumentFactory MELI_DOCUMENT_FACTORY = new PoiMeliDocumentFactory();

  /**
   * Creates a MeliDocument based on list of sheet names.
   *
   * @param sheetNames base list
   * @return a {@link MeliDocument}
   */
  public static MeliDocument createMeliDocument(final List<String> sheetNames) {
    try {
      final MeliDocument meliDocument = MELI_DOCUMENT_FACTORY.newDocument();

      sheetNames.forEach(meliDocument::addSheet);

      return meliDocument;
    } catch (final MeliDocument.MeliDocumentException e) {
      return null;
    }
  }

  /**
   * Creates a list of bytes based on a MeliDocument created from a ist of sheet names.
   *
   * @param sheetNames base list
   * @return a byte Array
   */
  public static byte[] createMeliDocumentAsByteArray(final List<String> sheetNames) {
    try {
      var meliDocument = createMeliDocument(sheetNames);
      return meliDocument == null ? null : meliDocument.toBytes();
    } catch (final MeliDocument.MeliDocumentException e) {
      return null;
    }
  }

  /**
   * Creates a MeliSheet based on a filepath and a sheet name.
   *
   * @param name     sheet's name
   * @param filePath to search the sheet by name
   * @return a {@link MeliSheet} or null
   */
  public static MeliSheet getMeliSheetFrom(final String name, final String filePath) {
    final byte[] forecastExampleFile = getResource(filePath);
    try {
      return forecastExampleFile == null
          ? null
          : new PoiDocument(forecastExampleFile).getSheetByName(name);
    } catch (MeliDocument.MeliDocumentException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Get the bytes resource from its name.
   *
   * @param resourceName resource name to load
   * @return null or the resource loaded as Stream
   */
  public static byte[] getResource(final String resourceName) {
    try {
      var resource = TestUtils.class.getClassLoader()
          .getResourceAsStream(resourceName);
      return resource == null ? null : resource.readAllBytes();
    } catch (IOException exception) {
      exception.printStackTrace();
      return null;
    }
  }

  /**
   * Creates a Productivity Request from known values:
   * <p>workflow: FBM_WMS_OUTBOUND</p>
   * <p>warehouseId: {@value WAREHOUSE_ID}</p>
   * <p>entityType: PRODUCTIVITY</p>
   * <p>processName: [PICKING, PACKING, PACKING_WALL]</p>
   * <p>simulations(Simulation): [PICKING].</p>
   * <p>simulations(SimulationEntity): [HEADCOUNT].</p>
   *
   * @param currentTime to evaluate dates from-to
   * @return a {@link ProductivityRequest} instance
   */
  public static ProductivityRequest createProductivityRequest(final ZonedDateTime currentTime) {
    return ProductivityRequest.builder()
        .workflow(FBM_WMS_OUTBOUND)
        .warehouseId(WAREHOUSE_ID)
        .entityType(PRODUCTIVITY)
        .processName(List.of(PICKING, PACKING, PACKING_WALL))
        .dateFrom(currentTime)
        .dateTo(currentTime.plusDays(1))
        .abilityLevel(List.of(1, 2))
        .simulations(List.of(new Simulation(PICKING,
            List.of(new SimulationEntity(
                HEADCOUNT, List.of(new QuantityByDate(currentTime, 20)))))))
        .build();
  }

  /**
   * Creates the list of Processes belonging to inbound based on static values.
   *
   * @return a List of {@link StaffingProcess}
   */
  public static List<StaffingProcess> inboundProcesses() {
    return List.of(
        new StaffingProcess(
            RECEIVING_PROCESS,
            new ProcessTotals(0, RECEIVING_SYS_WORKERS, RECEIVING_NET_PRODUCTIVITY, null, RECEIVING_THROUGHPUT),
            emptyList()),
        new StaffingProcess(
            CHECK_IN_PROCESS,
            new ProcessTotals(1, CHECK_IN_SYS_WORKERS, CHECK_IN_NET_PRODUCTIVITY, null, CHECK_IN_THROUGHPUT),
            emptyList()),
        new StaffingProcess(
            PUT_AWAY_PROCESS,
            new ProcessTotals(1, PUT_AWAY_SYS_WORKERS, PUT_AWAY_NET_PRODUCTIVITY, null, PUT_AWAY_THROUGHPUT),
            List.of(
                new Area(AREA_MZ1,
                    new Totals(0, PUT_AWAY_MZ1_SYS_WORKERS, PUT_AWAY_MZ1_NET_PRODUCTIVITY, PUT_AWAY_MZ1_THROUGHPUT)),
                new Area(AREA_RKL,
                    new Totals(1, PUT_AWAY_RKL_SYS_WORKERS, PUT_AWAY_RKL_NET_PRODUCTIVITY, PUT_AWAY_RKL_THROUGHPUT))
            ))
    );
  }

  /**
   * Creates the list of Processes belonging to outbound based on static values.
   *
   * @return a List of {@link StaffingProcess}
   */
  public static List<StaffingProcess> outboundProcesses() {
    return List.of(
        new StaffingProcess(
            PICKING_PROCESS,
            new ProcessTotals(OUTBOUND_PICKING_IDLE_WORKERS, OUTBOUND_PICKING_SYS_WORKERS, OUTBOUND_PICKING_NET_PRODUCTIVITY, null,
                OUTBOUND_PICKING_THROUGHPUT),
            List.of(
                new Area(AREA_MZ1,
                    new Totals(OUTBOUND_PICKING_MZ1_IDLE_WORKERS, OUTBOUND_PICKING_MZ1_SYS_WORKERS, OUTBOUND_PICKING_MZ1_NET_PRODUCTIVITY,
                        OUTBOUND_PICKING_MZ1_THROUGHPUT)),
                new Area(AREA_RKL,
                    new Totals(OUTBOUND_PICKING_RKL_IDLE_WORKERS, OUTBOUND_PICKING_RKL_SYS_WORKERS, OUTBOUND_PICKING_RKL_NET_PRODUCTIVITY,
                        OUTBOUND_PICKING_RKL_THROUGHPUT))
            )),
        new StaffingProcess(
            BATCH_SORTER_PROCESS,
            new ProcessTotals(null, null, null, null, null),
            emptyList()),
        new StaffingProcess(
            WALL_IN_PROCESS,
            new ProcessTotals(0, 0, 0.0, null, 0.0),
            emptyList()),
        new StaffingProcess(
            PACKING_PROCESS,
            new ProcessTotals(1, OUTBOUND_PACKING_SYS_WORKERS, null, OUTBOUND_PACKING_EFF_PRODUCTIVITY,
                OUTBOUND_PACKING_THROUGHPUT),
            emptyList()),
        new StaffingProcess(
            PACKING_WALL_PROCESS,
            new ProcessTotals(OUTBOUND_PACKING_WALL_IDLE_WORKERS, OUTBOUND_PACKING_WALL_SYS_WORKERS, null,
                OUTBOUND_PACKING_WALL_EFF_PRODUCTIVITY, OUTBOUND_PACKING_WALL_THROUGHPUT),
            emptyList())
    );
  }

  /**
   * Creates the list of Processes belonging to withdrawals based on static values.
   *
   * @return a List of {@link StaffingProcess}
   */
  public static List<StaffingProcess> withdrawalsProcesses() {
    return List.of(
        new StaffingProcess(
            PICKING_PROCESS,
            new ProcessTotals(WITHDRAWALS_PICKING_IDLE_WORKERS, WITHDRAWALS_PICKING_SYS_WORKERS, WITHDRAWALS_PICKING_NET_PRODUCTIVITY, null,
                WITHDRAWALS_PICKING_THROUGHPUT),
            List.of(
                new Area(AREA_RKL, new Totals(WITHDRAWALS_PICKING_RKL_IDLE_WORKERS, WITHDRAWALS_PICKING_RKL_SYS_WORKERS,
                    WITHDRAWALS_PICKING_RKL_NET_PRODUCTIVITY, WITHDRAWALS_PICKING_RKL_THROUGHPUT))
            )),
        new StaffingProcess(
            PACKING_PROCESS,
            new ProcessTotals(WITHDRAWALS_PACKING_IDLE_WORKERS, WITHDRAWALS_PACKING_SYS_WORKERS, null,
                WITHDRAWALS_PACKING_EFF_PRODUCTIVITY, WITHDRAWALS_PACKING_THROUGHPUT),
            emptyList())
    );
  }
}
