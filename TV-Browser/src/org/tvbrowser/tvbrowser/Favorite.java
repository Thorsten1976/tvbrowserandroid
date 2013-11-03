/*
 * TV-Browser for Android
 * Copyright (C) 2013 René Mach (rene@tvbrowser.org)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to use, copy, modify or merge the Software,
 * furthermore to publish and distribute the Software without modifications and to permit persons to whom
 * the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.tvbrowser.tvbrowser;

import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.SettingConstants;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.content.LocalBroadcastManager;

public class Favorite {
  public static final String OLD_NAME_KEY = "OLD_NAME_KEY";
  public static final String NAME_KEY = "NAME_KEY";
  public static final String SEARCH_KEY = "SEARCH_KEY";
  public static final String ONLY_TITLE_KEY = "ONLY_TITLE_KEY";
  
  private String mName;
  private String mSearch;
  private boolean mOnlyTitle;
  
  public Favorite(String name, String search, boolean onlyTitle) {
    mName = name;
    mSearch = search;
    mOnlyTitle = onlyTitle;
  }
  
  public boolean searchOnlyTitle() {
    return mOnlyTitle;
  }
  
  public String getName() {
    return mName;
  }
  
  public String getSearchValue() {
    return mSearch;
  }
  
  public void setValues(String name, String search, boolean onlyTitle) {
    mName = name;
    mSearch = search;
    mOnlyTitle = onlyTitle;
  }
  
  public String toString() {
    return mName;
  }
  
  public String getWhereClause() {
    StringBuilder builder = new StringBuilder();
    
    builder.append(" AND ((");
    builder.append(TvBrowserContentProvider.DATA_KEY_TITLE);
    builder.append(" LIKE \"%");
    builder.append(mSearch);
    builder.append("%\")");
    
    if(!mOnlyTitle) {
      String[] values = {TvBrowserContentProvider.DATA_KEY_TITLE_ORIGINAL,TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE,TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION,TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE_ORIGINAL};
      
      for(String value : values) {
        builder.append(" OR (");
        builder.append(value);
        builder.append(" LIKE \"%");
        builder.append(mSearch);
        builder.append("%\")");
      }
    }
    
    builder.append(")");
    
    return builder.toString();
  }
  
  public String getSaveString() {
    return mName + ";;" + mSearch + ";;" + String.valueOf(mOnlyTitle);
  }
  
  public static void removeFavoriteMarking(Context context, ContentResolver resolver, Favorite favorite) {
    String[] projection = {
        TvBrowserContentProvider.KEY_ID,
        TvBrowserContentProvider.DATA_KEY_UNIX_DATE,
        TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,
        TvBrowserContentProvider.DATA_KEY_STARTTIME,
        TvBrowserContentProvider.DATA_KEY_ENDTIME,
        TvBrowserContentProvider.DATA_KEY_TITLE,
        TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION,
        TvBrowserContentProvider.DATA_KEY_MARKING_VALUES,
        TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE,
        TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION,
        TvBrowserContentProvider.DATA_KEY_TITLE_ORIGINAL,
        TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE_ORIGINAL,
        TvBrowserContentProvider.DATA_KEY_GENRE,
        TvBrowserContentProvider.DATA_KEY_MARKING_VALUES
    };
    
    String where = " ( " + TvBrowserContentProvider.DATA_KEY_STARTTIME + " <= " + System.currentTimeMillis() + " AND " + TvBrowserContentProvider.DATA_KEY_ENDTIME + " >= " + System.currentTimeMillis();
    where += " OR " + TvBrowserContentProvider.DATA_KEY_STARTTIME + " > " + System.currentTimeMillis() + " ) ";
    
    where += favorite.getWhereClause();
    
    Cursor cursor = resolver.query(TvBrowserContentProvider.CONTENT_URI_DATA, projection, where, null, TvBrowserContentProvider.DATA_KEY_STARTTIME);
    
    if(cursor.getCount() > 0) {
      cursor.moveToFirst();
      
      do {
        long id = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.KEY_ID));
        String marking = "";
        
        if(!cursor.isNull(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES))) {
          marking = cursor.getString(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES));
        }
        
        String[] parts = marking.split(";");
        
        StringBuilder newValue = new StringBuilder();
        
        for(String part : parts) {
          if(!part.equalsIgnoreCase(SettingConstants.MARK_VALUE_FAVORITE)) {
            newValue.append(part);
            newValue.append(";");
          }
        }
        
        if(newValue.length() > 0) {
          newValue.deleteCharAt(newValue.length()-1);
        }
          
        ContentValues values = new ContentValues();
        
        values.put(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES, newValue.toString());
        
        resolver.update(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, id), values, null, null);
        
        Intent intent = new Intent(SettingConstants.MARKINGS_CHANGED);
        intent.putExtra(SettingConstants.MARKINGS_ID, id);
        
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
      }while(cursor.moveToNext());
    }
    
    cursor.close();
  }
  
  public static void updateFavoriteMarking(Context context, ContentResolver resolver, Favorite favorite) {
    String[] projection = {
        TvBrowserContentProvider.KEY_ID,
        TvBrowserContentProvider.DATA_KEY_UNIX_DATE,
        TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,
        TvBrowserContentProvider.DATA_KEY_STARTTIME,
        TvBrowserContentProvider.DATA_KEY_ENDTIME,
        TvBrowserContentProvider.DATA_KEY_TITLE,
        TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION,
        TvBrowserContentProvider.DATA_KEY_MARKING_VALUES,
        TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE,
        TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION,
        TvBrowserContentProvider.DATA_KEY_TITLE_ORIGINAL,
        TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE_ORIGINAL,
        TvBrowserContentProvider.DATA_KEY_GENRE,
        TvBrowserContentProvider.DATA_KEY_MARKING_VALUES
    };
    
    String where = " ( " + TvBrowserContentProvider.DATA_KEY_STARTTIME + " <= " + System.currentTimeMillis() + " AND " + TvBrowserContentProvider.DATA_KEY_ENDTIME + " >= " + System.currentTimeMillis();
    where += " OR " + TvBrowserContentProvider.DATA_KEY_STARTTIME + " > " + System.currentTimeMillis() + " ) ";
    
    where += favorite.getWhereClause();
    
    Cursor cursor = resolver.query(TvBrowserContentProvider.CONTENT_URI_DATA, projection, where, null, TvBrowserContentProvider.DATA_KEY_STARTTIME);
    
    if(cursor.getCount() > 0) {
      cursor.moveToFirst();
      
      do {
        long id = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.KEY_ID));
        String marking = "";
        
        if(!cursor.isNull(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES))) {
          marking = cursor.getString(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES));
        }
        
        if(!marking.contains(SettingConstants.MARK_VALUE_FAVORITE)) {
          StringBuilder value = new StringBuilder();
          value.append(marking.trim());
          
          if(marking.trim().length() != 0) {
            value.append(";");
          }
          
          value.append(SettingConstants.MARK_VALUE_FAVORITE);
          
          ContentValues values = new ContentValues();
          
          values.put(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES, value.toString());
          
          resolver.update(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, id), values, null, null);
          
          Intent intent = new Intent(SettingConstants.MARKINGS_CHANGED);
          intent.putExtra(SettingConstants.MARKINGS_ID, id);
          
          LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }
      }while(cursor.moveToNext());
    }
    
    cursor.close();
  }
  
  @Override
  public boolean equals(Object o) {
    if(o instanceof Favorite) {
      return ((Favorite)o).mName.equals(mName);
    }
    
    return super.equals(o);
  }
}
