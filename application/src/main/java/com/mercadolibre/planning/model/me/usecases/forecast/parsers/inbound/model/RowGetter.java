package com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model;

import com.mercadolibre.planning.model.me.usecases.forecast.utils.SheetVersion;
import com.mercadolibre.spreadsheet.MeliSheet;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

public interface RowGetter {

    RepsRow readRows(MeliSheet sheet,
                     ZoneId zoneId,
                     int row,
                     SheetVersion sheetVersion);

    Optional<String> getMissingRowValues(List<RepsRow> repsRows);

}
