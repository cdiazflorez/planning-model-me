package com.mercadolibre.planning.model.me.clients.rest.backlog;

import static com.mercadolibre.planning.model.me.clients.rest.backlog.ClientUrl.BACKLOG_URL;
import static com.mercadolibre.planning.model.me.clients.rest.backlog.ClientUrl.CURRENT_BACKLOG_URL;
import static com.mercadolibre.planning.model.me.clients.rest.backlog.ClientUrl.DATE_FROM;
import static com.mercadolibre.planning.model.me.clients.rest.backlog.ClientUrl.DATE_IN_FROM;
import static com.mercadolibre.planning.model.me.clients.rest.backlog.ClientUrl.DATE_IN_TO;
import static com.mercadolibre.planning.model.me.clients.rest.backlog.ClientUrl.DATE_TO;
import static com.mercadolibre.planning.model.me.clients.rest.backlog.ClientUrl.GROUP_BY;
import static com.mercadolibre.planning.model.me.clients.rest.backlog.ClientUrl.LAST_PHOTO_URL;
import static com.mercadolibre.planning.model.me.clients.rest.backlog.ClientUrl.PHOTO_URL;
import static com.mercadolibre.planning.model.me.clients.rest.backlog.ClientUrl.PROCESSES;
import static com.mercadolibre.planning.model.me.clients.rest.backlog.ClientUrl.REQUEST_DATE;
import static com.mercadolibre.planning.model.me.clients.rest.backlog.ClientUrl.SLA_FROM;
import static com.mercadolibre.planning.model.me.clients.rest.backlog.ClientUrl.SLA_TO;
import static com.mercadolibre.planning.model.me.clients.rest.backlog.ClientUrl.STEPS;
import static com.mercadolibre.planning.model.me.clients.rest.backlog.ClientUrl.WORKFLOWS;
import static java.lang.String.format;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static org.springframework.http.HttpStatus.OK;

import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpClient;
import com.mercadolibre.fbm.wms.outbound.commons.rest.HttpRequest;
import com.mercadolibre.fbm.wms.outbound.commons.rest.exception.ClientException;
import com.mercadolibre.json.type.TypeReference;
import com.mercadolibre.planning.model.me.clients.rest.config.RestPool;
import com.mercadolibre.planning.model.me.controller.backlog.exception.BacklogNotRespondingException;
import com.mercadolibre.planning.model.me.gateways.backlog.BacklogApiGateway;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogCurrentRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogLastPhotoRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogPhotosRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.BacklogRequest;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Consolidation;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.Photo;
import com.mercadolibre.planning.model.me.gateways.backlog.dto.PhotoRequest;
import com.mercadolibre.restclient.MeliRestClient;
import com.newrelic.api.agent.Trace;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BacklogApiClientImplement extends HttpClient implements BacklogApiGateway {

  public BacklogApiClientImplement(final MeliRestClient client) {
    super(client, RestPool.BACKLOG.name());
  }

  /**
   * get backlog from backlog api.
   *
   * @param request request of client backlog api
   * @return list of consolidation
   * @throws BacklogNotRespondingException error call backlog api
   * @deprecated will be used getPhoto
   */
  @Trace
  @Deprecated
  public List<Consolidation> getBacklog(final BacklogRequest request) {
    try {
      final HttpRequest httpRequest = HttpRequest.builder()
          .url(format(BACKLOG_URL.getName(), request.getWarehouseId()))
          .GET()
          .queryParams(getQueryParams(request))
          .acceptedHttpStatuses(Set.of(OK))
          .build();

      return send(httpRequest, response ->
          response.getData(new TypeReference<>() {
          })
      );
    } catch (ClientException e) {
      throw new BacklogNotRespondingException("Unable to get backlog from Backlog API", e);
    }
  }

  /**
   * get current backlog.
   *
   * @param logisticCenterId logistic center id
   * @param workflows        list of workflow
   * @param steps            list step
   * @param slaFrom          sla from
   * @param slaTo            sla to
   * @param groupingFields   list of grouping fields
   * @return list of consolidation
   * @deprecated will be used getLastPhoto
   */
  @Trace
  @Deprecated
  public List<Consolidation> getCurrentBacklog(final String logisticCenterId,
                                               final List<String> workflows,
                                               final List<String> steps,
                                               final Instant slaFrom,
                                               final Instant slaTo,
                                               final List<String> groupingFields) {

    BacklogRequest request = BacklogRequest.builder()
        .workflows(workflows)
        .steps(steps)
        .slaFrom(slaFrom)
        .slaTo(slaTo)
        .groupingFields(groupingFields)
        .build();

    final HttpRequest httpRequest = HttpRequest.builder()
        .url(format(CURRENT_BACKLOG_URL.getName(), logisticCenterId))
        .GET()
        .queryParams(getQueryParams(request))
        .acceptedHttpStatuses(Set.of(OK))
        .build();

    return send(httpRequest, response ->
        response.getData(new TypeReference<>() {
        })
    );
  }

  /**
   * get current backlog in client backlog api.
   *
   * @param request Request of client backlog api current backlog.
   * @return list of consolidation
   * @deprecated will be used getLastPhoto
   */
  @Trace
  @Deprecated
  public List<Consolidation> getCurrentBacklog(final BacklogCurrentRequest request) {
    final HttpRequest httpRequest = HttpRequest.builder()
        .url(format(CURRENT_BACKLOG_URL.getName(), request.getWarehouseId()))
        .GET()
        .queryParams(getQueryParams(request))
        .acceptedHttpStatuses(Set.of(OK))
        .build();

    return send(httpRequest, response ->
        response.getData(new TypeReference<>() {
        })
    );
  }

  /**
   * Gets the backlog photos in the interval specified by the {@link PhotoRequest}.
   * The cells of the returned photos are filtered and grouped according to the {@link PhotoRequest} parameter
   *
   * @param request request of client photo.
   * @return list of photos obtain for client.
   * @throws BacklogNotRespondingException error call backlogs api.
   */
  @Trace
  public List<Photo> getPhotos(final BacklogPhotosRequest request) {
    try {
      final HttpRequest httpRequest = HttpRequest.builder()
          .url(format(PHOTO_URL.getName(), request.getLogisticCenterId()))
          .GET()
          .queryParams(request.getQueryParamsPhoto())
          .acceptedHttpStatuses(Set.of(OK))
          .build();

      return send(httpRequest, response ->
          response.getData(new TypeReference<>() {
          })
      );
    } catch (ClientException e) {
      log.error("could not retrieve backlog photos", e);
      throw new BacklogNotRespondingException("Unable to get photos from Backlog API", e);
    }
  }

  /**
   * Gets the backlog of last photo.
   * The cells of the returned last photos is filtered and grouped according to the {@link PhotoRequest} parameter
   *
   * @param request request of client photo.
   * @return photo obtain for client.
   * @throws BacklogNotRespondingException error call backlogs api.
   */
  @Trace
  public Photo getLastPhoto(final BacklogLastPhotoRequest request) {
    try {
      final HttpRequest httpRequest = HttpRequest.builder()
          .url(format(LAST_PHOTO_URL.getName(), request.getLogisticCenterId()))
          .GET()
          .queryParams(request.getQueryParamsPhoto())
          .acceptedHttpStatuses(Set.of(OK))
          .build();

      return send(httpRequest, response ->
          response.getData(new TypeReference<>() {
          })
      );
    } catch (ClientException e) {
      log.error("could not retrieve backlog of last photo", e);
      throw new BacklogNotRespondingException("Unable to get last photo from Backlog API", e);
    }
  }

  private Map<String, String> getQueryParams(BacklogRequest request) {
    Map<String, String> params = new HashMap<>();
    addAsQueryParam(params, REQUEST_DATE.getName(), request.getRequestDate());
    addAsQueryParam(params, WORKFLOWS.getName(), request.getWorkflows());
    addAsQueryParam(params, PROCESSES.getName(), request.getProcesses());
    addAsQueryParam(params, STEPS.getName(), request.getSteps());
    addAsQueryParam(params, GROUP_BY.getName(), request.getGroupingFields());
    addAsQueryParam(params, DATE_FROM.getName(), request.getDateFrom());
    addAsQueryParam(params, DATE_TO.getName(), request.getDateTo());
    addAsQueryParam(params, DATE_IN_FROM.getName(), request.getDateInFrom());
    addAsQueryParam(params, DATE_IN_TO.getName(), request.getDateInTo());
    addAsQueryParam(params, SLA_FROM.getName(), request.getSlaFrom());
    addAsQueryParam(params, SLA_TO.getName(), request.getSlaTo());

    return params;
  }

  private Map<String, String> getQueryParams(BacklogCurrentRequest request) {
    Map<String, String> params = new HashMap<>();
    addAsQueryParam(params, REQUEST_DATE.getName(), request.getRequestDate());
    addAsQueryParam(params, WORKFLOWS.getName(), request.getWorkflows());
    addAsQueryParam(params, PROCESSES.getName(), request.getProcesses());
    addAsQueryParam(params, STEPS.getName(), request.getSteps());
    addAsQueryParam(params, GROUP_BY.getName(), request.getGroupingFields());
    addAsQueryParam(params, DATE_IN_FROM.getName(), request.getDateInFrom());
    addAsQueryParam(params, DATE_IN_TO.getName(), request.getDateInTo());
    addAsQueryParam(params, SLA_FROM.getName(), request.getSlaFrom());
    addAsQueryParam(params, SLA_TO.getName(), request.getSlaTo());

    return params;
  }

  private void addAsQueryParam(Map<String, String> map, String key, List<String> value) {
    if (value != null) {
      map.put(key, String.join(",", value));
    }
  }

  private void addAsQueryParam(Map<String, String> map, String key, Instant value) {
    if (value != null) {
      map.put(key, ISO_INSTANT.format(value));
    }
  }
}
