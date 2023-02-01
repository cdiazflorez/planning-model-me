package com.mercadolibre.planning.model.me.usecases.forecast.parsers.inbound.model;

import com.mercadolibre.planning.model.me.usecases.forecast.utils.SheetVersion;
import com.mercadolibre.spreadsheet.MeliSheet;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * interface used to get the rows.
 */
public interface RowGetter {

    /**
     * read the files from the sheet.
     * @param sheet         Sheet file.
     * @param zoneId        Zone Id.
     * @param row           number of row.
     * @param sheetVersion  Sheet version.
     * @return {@link RepsRow}
     */
    RepsRow readRows(MeliSheet sheet,
                     ZoneId zoneId,
                     int row,
                     SheetVersion sheetVersion);

    /**
     * gets the missing information from each row.
     * @param repsRows Reps by row.
     * @return Optional message.
     */
    Optional<String> getMissingRowValues(List<RepsRow> repsRows);

}
