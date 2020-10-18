/*
 * TV-Browser for Android
 * Copyright (C) 2013-2014 René Mach (rene@tvbrowser.org)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to use, copy, modify or merge the Software,
 * furthermore to publish and distribute the Software free of charge without modifications and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.tvbrowser.utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import androidx.annotation.BoolRes;
import androidx.annotation.IntDef;
import androidx.annotation.IntegerRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.collection.SparseArrayCompat;
import androidx.preference.PreferenceManager;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.filter.FilterValues;
import org.tvbrowser.filter.FilterValuesChannels;
import org.tvbrowser.tvbrowser.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.database.Cursor;
import android.util.Log;

import static android.content.Context.MODE_PRIVATE;

public final class PrefUtils {

  private static final String PREFERENCES_FAVORITE = "preferencesFavorite";
  private static final String PREFERENCES_FILTER = "filterPreferences";
  private static final String PREFERENCES_TRANSPORTATION = "transportation";
  private static final String PREFERENCES_MARKINGS = "markings";
  private static final String PREFERENCES_MARKING_REMINDERS = "markingsReminders";
  private static final String PREFERENCES_MARKING_SYNC = "markingsSynchronization";

  public static final int TYPE_PREFERENCES_SHARED_GLOBAL = 0;
  public static final int TYPE_PREFERENCES_FAVORITES = 1;
  public static final int TYPE_PREFERENCES_FILTERS = 2;
  public static final int TYPE_PREFERENCES_TRANSPORTATION = 3;
  public static final int TYPE_PREFERENCES_MARKINGS = 4;
  public static final int TYPE_PREFERENCES_MARKING_REMINDERS = 5;
  public static final int TYPE_PREFERENCES_MARKING_SYNC = 6;

  @Retention(RetentionPolicy.SOURCE)
  @IntDef({TYPE_PREFERENCES_SHARED_GLOBAL, TYPE_PREFERENCES_FAVORITES, TYPE_PREFERENCES_FILTERS,
          TYPE_PREFERENCES_TRANSPORTATION, TYPE_PREFERENCES_MARKINGS,
          TYPE_PREFERENCES_MARKING_REMINDERS, TYPE_PREFERENCES_MARKING_SYNC})
  @interface PreferencesType {}

  private final Resources mResources;
  private final SparseArrayCompat<SharedPreferences> mSharedPreferences;

  public PrefUtils(@NonNull final Context context) {
    mResources = context.getResources();
    mSharedPreferences = new SparseArrayCompat<>(7);
    init(context);
  }

  public void init(@NonNull final Context context) {
    synchronized (mSharedPreferences) {
      mSharedPreferences.put(TYPE_PREFERENCES_SHARED_GLOBAL,
              PreferenceManager.getDefaultSharedPreferences(context));
      mSharedPreferences.put(TYPE_PREFERENCES_FAVORITES,
              context.getSharedPreferences(PREFERENCES_FAVORITE, MODE_PRIVATE));
      mSharedPreferences.put(TYPE_PREFERENCES_FILTERS,
              context.getSharedPreferences(PREFERENCES_FILTER, MODE_PRIVATE));
      mSharedPreferences.put(TYPE_PREFERENCES_TRANSPORTATION,
              context.getSharedPreferences(PREFERENCES_TRANSPORTATION, MODE_PRIVATE));
      mSharedPreferences.put(TYPE_PREFERENCES_MARKINGS,
              context.getSharedPreferences(PREFERENCES_MARKINGS, MODE_PRIVATE));
      mSharedPreferences.put(TYPE_PREFERENCES_MARKING_REMINDERS,
              context.getSharedPreferences(PREFERENCES_MARKING_REMINDERS, MODE_PRIVATE));
      mSharedPreferences.put(TYPE_PREFERENCES_MARKING_SYNC,
              context.getSharedPreferences(PREFERENCES_MARKING_SYNC, MODE_PRIVATE));
    }
  }

  public SharedPreferences getDefault() {
    return getShared(TYPE_PREFERENCES_SHARED_GLOBAL);
  }

  public SharedPreferences getShared(@PreferencesType final int type) {
    return mSharedPreferences.get(type);
  }

  public Editor edit() {
    return edit(TYPE_PREFERENCES_SHARED_GLOBAL);
  }

  public Editor edit(@PreferencesType final int type) {
    return getShared(type).edit();
  }

  /**
   * Returns a preference value from the shared preferences identified by a given string resource id
   * (preferences key).
   * <p/>
   * If the underlying shared preferences object is <code>null</code>, was not initialized properly,
   * or if the preferences key could not be found, the given default value is returned.
   * </p>
   * The default value defines the return type of this generic operation and cannot be
   * <code>null</code>.
   *
   * @param <T> the generic return type.
   * @param defaultValue the fallback value which maps to the generic return type.
   * @param prefKey the string resource identifier which maps to the preferences key.
   * @return the preferences value from the shared preferences identified by a given string
   *  resource, or the default value as fallback.
   */
  @Nullable
  @SuppressWarnings("unchecked")
  public <T> T getValue(@StringRes final int prefKey, @Nullable final T defaultValue) {
    return getValue(TYPE_PREFERENCES_SHARED_GLOBAL, prefKey, defaultValue);
  }

  /**
   * Returns a preference value from the shared preferences identified by a given string resource id
   * (preferences key) and a given {@link PreferencesType}.
   * <p/>
   * If the underlying shared preferences object is <code>null</code>, was not initialized properly,
   * or if the preferences key could not be found, the given default value is returned.
   * </p>
   * The default value defines the return type of this generic operation and cannot be
   * <code>null</code>.
   *
   * @param <T> the generic return type.
   * @param defaultValue the fallback value which maps to the generic return type.
   * @param prefKey the string resource identifier which maps to the preferences key.
   * @param type the shared preferences type (one of {@link PreferencesType}).
   * @return the preferences value from the shared preferences identified by a given string
   *  resource, or the default value as fallback.
   */
  @Nullable
  @SuppressWarnings("unchecked")
  public <T> T getValue(@PreferencesType final int type, @StringRes final int prefKey, @Nullable final T... defaultValue) {
    T result = null;
    final SharedPreferences sharedPreferences = getShared(type);
    final String key = mResources.getString(prefKey);
    if (sharedPreferences != null && sharedPreferences.contains(key)) {
      result = (T) sharedPreferences.getAll().get(key);
    }
    return result==null ? (defaultValue!=null && defaultValue.length==1 ? defaultValue[0] : null) : result;
  }

  public <T> void setValue(@StringRes final int prefKey, @NonNull final T value, final boolean... apply) {
    setValue(TYPE_PREFERENCES_SHARED_GLOBAL, prefKey, value, apply);
  }

  @SuppressWarnings("unchecked")
  public <T> void setValue(@PreferencesType final int type, @StringRes final int prefKey, @NonNull final T value, final boolean... apply) {
      final String key = mResources.getString(prefKey);
      final Editor edit = edit(type);
      if (value instanceof Integer) {
        edit.putInt(key, (Integer) value);
      } else if (value instanceof String) {
        edit.putString(key, value.toString());
      } else if (value instanceof Boolean) {
        edit.putBoolean(key, (Boolean) value);
      } else if (value instanceof Float) {
        edit.putFloat(key, (Float) value);
      } else if (value instanceof Long) {
        edit.putLong(key, (Long) value);
      } else if (value instanceof Set<?>) {
        if (((Set<?>) value).stream().allMatch(String.class::isInstance)) {
          edit.putStringSet(key, (Set<String>) value);
        }
      }
      if (apply!=null && apply.length==1 && apply[0]) {
        edit.apply();
      } else {
        edit.commit();
      }
  }

  public Integer getIntValueWithDefaultKey(int prefKey, @IntegerRes int defaultKey) {
      return getValue(prefKey, mResources.getInteger(defaultKey));
  }

  public Long getLongValueWithDefaultKey(int prefKey, @IntegerRes int defaultKey) {
      return getValue(prefKey, (long) mResources.getInteger(defaultKey));
  }
  
  public Boolean getBooleanValueWithDefaultKey(int prefKey, @BoolRes int defaultKey) {
	  return getValue(prefKey, mResources.getBoolean(defaultKey));
  }
  
  public String getStringValueWithDefaultKey(int prefKey, @StringRes int defaultKey) {
      return getValue(prefKey, mResources.getString(defaultKey));
  }

  public int getStringValueAsInt(int prefKey, @StringRes int defaultKey) throws NumberFormatException {
      final String value = getValue(prefKey, mResources.getString(defaultKey));
      return value==null ? Integer.MIN_VALUE : Integer.parseInt(value);
  }

  public void resetDataMetaData() {
    edit()
      .putLong(mResources.getString(R.string.META_DATA_DATE_FIRST_KNOWN),
              mResources.getInteger(R.integer.meta_data_date_known_default))
      .putLong(mResources.getString(R.string.META_DATA_DATE_LAST_KNOWN),
              mResources.getInteger(R.integer.meta_data_date_known_default))
      .putLong(mResources.getString(R.string.META_DATA_ID_FIRST_KNOWN),
              mResources.getInteger(R.integer.meta_data_id_default))
      .putLong(mResources.getString(R.string.META_DATA_ID_LAST_KNOWN),
              mResources.getInteger(R.integer.meta_data_id_default))
      .putLong(mResources.getString(R.string.LAST_DATA_UPDATE), 0)
      .commit();
  }
  
  public void updateDataMetaData(@NonNull final Context context) {
    setMetaDataLongValue(context, R.string.META_DATA_DATE_FIRST_KNOWN);
    setMetaDataLongValue(context, R.string.META_DATA_DATE_LAST_KNOWN);
    setMetaDataLongValue(context, R.string.META_DATA_ID_FIRST_KNOWN);
    setMetaDataLongValue(context, R.string.META_DATA_ID_LAST_KNOWN);
  }
  
  private void setMetaDataLongValue(@Nullable final Context context, final int value) {
    if(context!=null && IOUtils.isDatabaseAccessible(context)) {
      String sort = null;
      String column = null;

      if (value == R.string.META_DATA_DATE_FIRST_KNOWN) {
        column = TvBrowserContentProvider.DATA_KEY_STARTTIME;
        sort = column + " ASC LIMIT 1";
      } else if (value == R.string.META_DATA_DATE_LAST_KNOWN) {
        column = TvBrowserContentProvider.DATA_KEY_STARTTIME;
        sort = column + " DESC LIMIT 1";
      } else if (value == R.string.META_DATA_ID_FIRST_KNOWN) {
        column = TvBrowserContentProvider.KEY_ID;
        sort = column + " ASC LIMIT 1";
      } else if (value == R.string.META_DATA_ID_LAST_KNOWN) {
        column = TvBrowserContentProvider.KEY_ID;
        sort = column + " DESC LIMIT 1";
      }
      
      Cursor valueCursor = null;
      try {
        valueCursor = context.getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA,
                new String[] {column}, null, null, sort);
        if(valueCursor!=null && IOUtils.prepareAccessFirst(valueCursor)) {
          long last = valueCursor.getLong(valueCursor.getColumnIndex(column));
          edit().putLong(mResources.getString(value), last).commit();
        }
      }finally {
        IOUtils.close(valueCursor);
      }
    }
  }
  
  public void updateChannelSelectionState(@Nullable final Context context) {
    if(context!=null && IOUtils.isDatabaseAccessible(context)) {
      boolean value;
      Cursor channels = null;
      try {
        channels = context.getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_CHANNELS,
                new String[] {TvBrowserContentProvider.KEY_ID},
                TvBrowserContentProvider.CHANNEL_KEY_SELECTION + "=1", null,
                TvBrowserContentProvider.KEY_ID + " ASC LIMIT 1");
        value = channels != null && channels.getCount() > 0;
      }finally {
        IOUtils.close(channels);
      }
      
      edit().putBoolean(mResources.getString(R.string.CHANNELS_SELECTED), value).commit();
    }
  }
  
  public boolean getChannelsSelected() {
    return getDefault().getBoolean(mResources.getString(R.string.CHANNELS_SELECTED),
            mResources.getBoolean(R.bool.channels_selected_default));
  }
  
  private static String getFilterSelection(final Context context, @NonNull final Set<String> filterIds) {
    final Set<FilterValues> filterValues = new HashSet<>();
      for (String filterId : filterIds) {
        final FilterValues filter = FilterValues.load(filterId, context);
        if (filter != null) {
          filterValues.add(filter);
        }
      }
    return getFilterSelection(context, false, filterValues);
  }
  
  public static String getFilterSelection(final Context context, final boolean onlyChannelFilter, @NonNull final Set<FilterValues> filterValues) {
    final StringBuilder channels =  new StringBuilder();
    final StringBuilder result = new StringBuilder();
    for(FilterValues values : filterValues) {
      if(values instanceof FilterValuesChannels) {
        final int[] ids = ((FilterValuesChannels) values).getFilteredChannelIds();
        for(final int id : ids) {
          if(channels.length() > 0) {
            channels.append(", ");
          }
          channels.append(id);
        }
      }
      else if(!onlyChannelFilter) {
        result.append(values.getWhereClause(context).getWhere());
      }
    }
    
    if(channels.length() > 0) {
      result.append(" AND ").append(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID).append(" IN ( ");
      result.append(channels);
      result.append(" ) ");
    }
    
    return result.toString();
  }
  
  public String getFilterSelection(@NonNull final Context context) {
    final SharedPreferences pref = getDefault();
    final int oldVersion = pref.getInt(mResources.getString(R.string.OLD_VERSION), 379);
    
    Set<String> currentFilterIds = new HashSet<>();
    if(oldVersion < 379) {
      final String currentFilterId = pref.getString(mResources.getString(R.string.CURRENT_FILTER_ID), null);
      if(currentFilterId != null) {
        currentFilterIds.add(currentFilterId);
      }
    }
    else {
      currentFilterIds = pref.getStringSet(mResources.getString(R.string.CURRENT_FILTER_ID), currentFilterIds);
    }
    
    return getFilterSelection(context, currentFilterIds);
  }
  
  public boolean isNewDate() {
    Log.d("info6", "LAST KNOWN START DATE " + getDefault().getInt(
            mResources.getString(R.string.PREF_MISC_LAST_KNOWN_OPEN_DATE), -1) +
            " - CURRENT DATE " + Calendar.getInstance().get(Calendar.DAY_OF_YEAR));
    return Calendar.getInstance().get(Calendar.DAY_OF_YEAR) != getDefault().getInt(
            mResources.getString(R.string.PREF_MISC_LAST_KNOWN_OPEN_DATE), -1);
  }
  
  public void updateKnownOpenDate() {
    edit().putInt(mResources.getString(R.string.PREF_MISC_LAST_KNOWN_OPEN_DATE),
            Calendar.getInstance().get(Calendar.DAY_OF_YEAR)).commit();
  }

  public boolean isDarkTheme() {
    return getBooleanValueWithDefaultKey(R.string.DARK_STYLE, R.bool.dark_style_default);
  }
}