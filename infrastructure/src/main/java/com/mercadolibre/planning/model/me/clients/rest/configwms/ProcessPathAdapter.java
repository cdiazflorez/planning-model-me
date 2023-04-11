package com.mercadolibre.planning.model.me.clients.rest.configwms;

import com.mercadolibre.planning.model.me.enums.ProcessPath;
import com.mercadolibre.planning.model.me.usecases.forecast.UploadForecast;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ProcessPathAdapter implements UploadForecast.ProcessPathGateway {

  private static final String STATUS_ACTIVE = "ACTIVE";

  private final ConfigApiClient configApiClient;

  @Override
  public Set<ProcessPath> getProcessPathGateway(final String logisticCenterId) {
    final var processPathValid = Arrays.stream(ProcessPath.values()).map(ProcessPath::getName)
        .collect(Collectors.toUnmodifiableSet());

    return configApiClient.getProcessPath(logisticCenterId)
        .getProcessPaths()
        .stream()
        .filter(processPathCode -> STATUS_ACTIVE.equalsIgnoreCase(processPathCode.getStatus()))
        .map(ConfigApiClient.ProcessPathConfigWrapper.ProcessPathConfig::getProcessPathCode)
        .map(String::toLowerCase)
        .filter(processPathValid::contains)
        .map(ProcessPath::from)
        .collect(Collectors.toSet());
  }
}
