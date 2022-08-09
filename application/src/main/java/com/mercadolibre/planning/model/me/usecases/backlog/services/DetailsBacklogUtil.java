package com.mercadolibre.planning.model.me.usecases.backlog.services;

import static java.time.ZoneOffset.UTC;

import com.mercadolibre.planning.model.me.usecases.backlog.entities.NumberOfUnitsInAnArea;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

final class DetailsBacklogUtil {

  public static final String NO_AREA = "N/A";

  public static final long MAX_CURRENT_PHOTO_AGE_IN_MINUTES = 10;

  private static final List<NumberOfUnitsInAnArea> DEFAULT_PHOTO = List.of(new NumberOfUnitsInAnArea(NO_AREA, 0));

  private DetailsBacklogUtil() {
  }

  static Map<Instant, List<NumberOfUnitsInAnArea>> mergeMaps(final Map<Instant, List<NumberOfUnitsInAnArea>> one,
                                                             final Map<Instant, List<NumberOfUnitsInAnArea>> other) {
    return Stream.of(
            one.entrySet(),
            other.entrySet()
        )
        .flatMap(Set::stream)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  static Map<Instant, List<NumberOfUnitsInAnArea>> selectPhotos(final Map<Instant, List<NumberOfUnitsInAnArea>> numberOfUnitsByDate,
                                                                final Instant dateFrom,
                                                                final Instant viewDate) {
    final var fullHours = ChronoUnit.HOURS.between(dateFrom, viewDate) + 1;

    final var photosOClock = calculateOClockPhotos(numberOfUnitsByDate, dateFrom, fullHours);
    final var currentPhoto = calculateLastPhoto(numberOfUnitsByDate, viewDate);
    return Stream.of(
            photosOClock.entrySet().stream(),
            currentPhoto.entrySet().stream()
        )
        .flatMap(Function.identity())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private static Map<Instant, List<NumberOfUnitsInAnArea>> calculateOClockPhotos(final Map<Instant, List<NumberOfUnitsInAnArea>> photos,
                                                                                 final Instant dateFrom,
                                                                                 final long hours) {
    return LongStream.range(0, hours)
        .mapToObj(hourShift -> dateFrom.plus(hourShift, ChronoUnit.HOURS))
        .collect(Collectors.toMap(
            Function.identity(),
            date -> photos.getOrDefault(date, DEFAULT_PHOTO)
        ));
  }

  private static Map<Instant, List<NumberOfUnitsInAnArea>> calculateLastPhoto(final Map<Instant, List<NumberOfUnitsInAnArea>> photos,
                                                                              final Instant viewDate) {

    if (!photos.isEmpty()) {
      final var lastPhotoDate = photos.keySet()
          .stream()
          .max(Comparator.naturalOrder())
          .orElseThrow();

      final var isRecentPhoto = ChronoUnit.MINUTES.between(lastPhotoDate, viewDate) <= MAX_CURRENT_PHOTO_AGE_IN_MINUTES;
      final var isOClock = lastPhotoDate.atZone(UTC).getMinute() == 0;

      if (isRecentPhoto || !isOClock) {
        return Map.of(lastPhotoDate, photos.get(lastPhotoDate));
      } else {
        return Map.of(viewDate, DEFAULT_PHOTO);
      }
    } else {
      return Map.of(viewDate, DEFAULT_PHOTO);
    }
  }
}
