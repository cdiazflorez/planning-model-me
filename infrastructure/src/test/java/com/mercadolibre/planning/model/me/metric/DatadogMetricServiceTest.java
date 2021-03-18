package com.mercadolibre.planning.model.me.metric;

import com.mercadolibre.metrics.MetricCollector;
import com.mercadolibre.planning.model.me.controller.deviation.request.DeviationRequest;
import com.mercadolibre.planning.model.me.controller.simulation.request.RunSimulationRequest;
import com.mercadolibre.planning.model.me.gateways.planningmodel.dtos.Workflow;
import com.mercadolibre.planning.model.me.utils.TestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.mercadolibre.planning.model.me.utils.TestUtils.WAREHOUSE_ID;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class DatadogMetricServiceTest {

    @Mock
    private DatadogWrapper wrapper;

    @InjectMocks
    private DatadogMetricService service;

    private static final Workflow WORKFLOW = Workflow.FBM_WMS_OUTBOUND;
    private static final String PROJECTION_TYPE = "CPT";

    @Test
    @DisplayName("Send run simulation info")
    public void testSendRunSimulationInfo() {
        // GIVEN
        final RunSimulationRequest runSimulationRequest =
                TestUtils.mockRunSimulationRequest();
        final MetricCollector.Tags tags = createTags();
        tags.add("picking", "headcount");

        // WHEN
        service.trackSimulation(runSimulationRequest.getWarehouseId(),
                runSimulationRequest.getSimulations());

        // THEN
        thenCreatedTagsCorrectly("application.planning.model.simulation", tags);
    }

    @Test
    @DisplayName("Log fails but we catch the exception")
      public void testSendRunSimulationInfoFailure() {
        // GIVEN
        final RunSimulationRequest runSimulationRequest =
                TestUtils.mockRunSimulationRequest();
        final MetricCollector.Tags tags = createTags();
        tags.add("picking", "headcount");
        willThrow(new RuntimeException()).given(wrapper)
                .incrementCounter(
                        eq("application.planning.model.simulation"),
                        eq(tags));

        // WHEN
        service.trackSimulation(runSimulationRequest.getWarehouseId(),
                runSimulationRequest.getSimulations());

        // THEN
        thenCreatedTagsCorrectly("application.planning.model.simulation", tags);
    }

    @Test
    @DisplayName("Send deviation to adjust forecast with 24-hour range")
    public void testSendDeviation24RangeInfo() {
        // GIVEN
        final DeviationRequest deviationRequest =
                TestUtils.mockDeviationRequest(24);
        final MetricCollector.Tags tags = createTags();
        tags.add("duration", "true");

        // WHEN
        service.trackDeviationAdjustment(deviationRequest);

        // THEN
        thenCreatedTagsCorrectly("application.planning.model.deviation", tags);
    }

    @Test
    @DisplayName("Send deviation to adjust forecast with non 24-hour range")
    public void testSendDeviationNon24RangeInfo() {
        // GIVEN
        final DeviationRequest deviationRequest =
                TestUtils.mockDeviationRequest(10);
        final MetricCollector.Tags tags = createTags();
        tags.add("duration", "false");

        // WHEN
        service.trackDeviationAdjustment(deviationRequest);

        // THEN
        thenCreatedTagsCorrectly("application.planning.model.deviation", tags);
    }

    @Test
    @DisplayName("Send projection")
    public void testSendProjectionInfo() {
        // GIVEN
        final MetricCollector.Tags tags = createTags();
        tags.add("workflow", "fbm-wms-outbound");
        tags.add("projection_type", "CPT");

        // WHEN
        service.trackProjection(WAREHOUSE_ID, WORKFLOW, PROJECTION_TYPE);

        // THEN
        thenCreatedTagsCorrectly("application.planning.model.projection", tags);
    }

    @Test
    @DisplayName("Send forecast upload info")
    public void testForecastUploadInfo() {
        // GIVEN
        final MetricCollector.Tags tags = createTags();
        tags.add("warehouse_id", WAREHOUSE_ID);

        // WHEN
        service.trackForecastUpload(WAREHOUSE_ID);

        // THEN
        thenCreatedTagsCorrectly("application.planning.model.forecast.upload", tags);
    }

    private MetricCollector.Tags createTags() {
        final MetricCollector.Tags tags = new MetricCollector.Tags();

        tags.add("warehouse_id", WAREHOUSE_ID);
        tags.add("scope", "development");

        return tags;
    }

    private void thenCreatedTagsCorrectly(final String counterName,
                                          final MetricCollector.Tags tags) {

        final ArgumentCaptor<MetricCollector.Tags> captor = ArgumentCaptor
                .forClass(MetricCollector.Tags.class);
        verify(wrapper).incrementCounter(
                eq(counterName),
                captor.capture());

        assertArrayEquals(tags.toArray(), captor.getValue().toArray());
    }
}
