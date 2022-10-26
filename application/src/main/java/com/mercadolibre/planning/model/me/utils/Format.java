package com.mercadolibre.planning.model.me.utils;

import java.text.DecimalFormat;

public final class Format {

  private Format() {
  }

  /**
   * Decimal Truncate.
   * Truncate decimal number about quantity received by param.
   *
   * @param number          double number to truncate.
   * @param decimalQuantity maximum decimal quantity that the number can to have.
   * @return number truncated.
   **/
  public static Double decimalTruncate(final Double number, final int decimalQuantity) {
    final String pattern = "." + "#".repeat(Math.max(0, decimalQuantity));

    final DecimalFormat df = new DecimalFormat(pattern);

    return Double.parseDouble(df.format(number));
  }
}
