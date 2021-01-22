package com.mercadolibre.planning.model.me.utils;

import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProductivityRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.QuantityByDate;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Simulation;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.SimulationEntity;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.spreadsheet.MeliDocument;
import com.mercadolibre.spreadsheet.MeliDocumentFactory;
import com.mercadolibre.spreadsheet.MeliSheet;
import com.mercadolibre.spreadsheet.implementations.poi.PoiDocument;
import com.mercadolibre.spreadsheet.implementations.poi.PoiMeliDocumentFactory;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.ProcessName.PICKING;
import static com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow.FBM_WMS_OUTBOUND;

public class TestUtils {

    public static final String WAREHOUSE_ID = "ARTW01";
    public static final Workflow WORKFLOW = FBM_WMS_OUTBOUND;
    public static final ZonedDateTime A_DATE = ZonedDateTime.of(2020, 8, 19, 17, 40, 0, 0,
            ZoneId.of("UTC"));
    public static final String PROCESSING_TIME = "processing_time";
    public static final Long USER_ID = 1234L;
    private static final String FORECAST_EXAMPLE_FILE = "forecast_example.xlsx";
    public static final ZoneId ZONE_ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");

    private static MeliDocumentFactory meliDocumentFactory = new PoiMeliDocumentFactory();

    public static MeliDocument createMeliDocument(final List<String> sheetNames) {
        try {
            final MeliDocument meliDocument = meliDocumentFactory.newDocument();

            sheetNames.forEach((sheetName) -> meliDocument.addSheet(sheetName));

            return meliDocument;
        } catch (final MeliDocument.MeliDocumentException e) {
            return null;
        }
    }

    public static byte[] createMeliDocumentAsByteArray(final List<String> sheetNames) {
        try {
            return createMeliDocument(sheetNames).toBytes();
        } catch (final MeliDocument.MeliDocumentException e) {
            return null;
        }
    }

    public static MeliSheet getMeliSheetFrom(final String name, final String filePath) {
        final byte[] forecastExampleFile = getResource(filePath);

        try {
            return new PoiDocument(forecastExampleFile).getSheetByName(name);
        } catch (MeliDocument.MeliDocumentException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] getResource(final String resourceName) {
        try {
            return TestUtils.class.getClassLoader()
                    .getResourceAsStream(resourceName).readAllBytes();
        } catch (IOException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public static ProductivityRequest createProductivityRequest(final ZonedDateTime currentTime) {
        return ProductivityRequest.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId(WAREHOUSE_ID)
                .entityType(PRODUCTIVITY)
                .processName(List.of(PICKING, PACKING, PACKING_WALL))
                .dateFrom(currentTime)
                .dateTo(currentTime.plusDays(1))
                .abilityLevel(List.of(1,2))
                .simulations(List.of(new Simulation(PICKING,
                        List.of(new SimulationEntity(
                                HEADCOUNT, List.of(new QuantityByDate(currentTime, 20)))))))
                .build();
    }
}
