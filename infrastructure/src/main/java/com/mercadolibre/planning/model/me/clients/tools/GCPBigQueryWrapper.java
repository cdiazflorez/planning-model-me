package com.mercadolibre.planning.model.me.clients.tools;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.JobException;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class GCPBigQueryWrapper implements BigqueryWrapper {

    private BigQuery bigQuery;

    @Override
    public TableResult query(QueryJobConfiguration configuration) throws InterruptedException, JobException {
        return bigQuery.query(configuration);
    }
}
