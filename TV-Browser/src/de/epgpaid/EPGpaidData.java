/*
 * EPGpaid data: A supplement data plugin for TV-Browser.
 * Copyright: (c) 2018 René Mach
 */
package de.epgpaid;

import org.tvbrowser.App;
import org.tvbrowser.tvbrowser.R;
import org.tvbrowser.utils.PrefUtils;

public final class EPGpaidData {
  public static final int TYPE_DATE_FROM = 1;
  public static final int TYPE_DATE_UNTIL = 2;

  public static void setDateValue(final int type, final long date) {
    final PrefUtils prefUtils = App.get().prefs();
    switch (type) {
      case TYPE_DATE_FROM: prefUtils.setValue(R.string.PREF_EPGPAID_ACCESS_FROM, date, true); break;
      case TYPE_DATE_UNTIL: prefUtils.setValue(R.string.PREF_EPGPAID_ACCESS_UNTIL, date, true); break;
    }
  }
}