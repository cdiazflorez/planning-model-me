package com.mercadolibre.planning.model.me.clients.tools;

import com.google.cloud.bigquery.JobException;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;

interface BigqueryWrapper {
  TableResult query(QueryJobConfiguration configuration) throws InterruptedException, JobException;
}
