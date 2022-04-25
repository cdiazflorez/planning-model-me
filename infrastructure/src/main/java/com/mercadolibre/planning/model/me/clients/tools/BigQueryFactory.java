package com.mercadolibre.planning.model.me.clients.tools;


import com.google.api.services.bigquery.BigqueryScopes;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.JobException;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

/** Returns the wrapper according to the profile. */
@Slf4j
@Configuration
public class BigQueryFactory {

  private static final String JSON_ENV_VARIABLE = "SECRET_GCP_SERVICE_TOKEN";

  @Bean
  @Profile({"!development"})
  public BigqueryWrapper getBigquery() {
    try {
      String jsonFileCode = System.getenv(JSON_ENV_VARIABLE);
      byte[] decodedBytes = Base64.getDecoder().decode(jsonFileCode);
      InputStream jsonStream = new ByteArrayInputStream(decodedBytes);

      BigQuery service = BigQueryOptions.newBuilder().setProjectId("meli-bi-data")
          .setCredentials(ServiceAccountCredentials.fromStream(jsonStream).createScoped(BigqueryScopes.all()))
          .build()
          .getService();

      return new GCPBigQueryWrapper(service);
    } catch (IOException e) {
      log.error(e.getMessage(), e);
    }

    return null;
  }

  @Bean
  @Profile({"development"})
  public BigqueryWrapper getBigqueryTest() {

    return new BigqueryWrapper() {
      @Override
      public TableResult query(QueryJobConfiguration configuration) throws InterruptedException, JobException {
        return null;
      }
    };
  }

}


