/*
 * TV-Browser for Android
 * Copyright (C) 2013 René Mach (rene@tvbrowser.org)
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
package org.tvbrowser.tvbrowser;

import android.app.SearchManager;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.content.res.Resources.Theme;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ListView;
import androidx.preference.PreferenceManager;

import org.tvbrowser.App;
import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.devplugin.PluginHandler;
import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.utils.PrefUtils;
import org.tvbrowser.utils.ProgramUtils;
import org.tvbrowser.utils.UiUtils;
import org.tvbrowser.view.SeparatorDrawable;

public class ActivityTvBrowserSearchResults extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>, OnSharedPreferenceChangeListener, ShowDateInterface, MarkingsUpdateListener {
  private SimpleCursorAdapter mProgramsListAdapter;

  private static final String QUERY_EXTRA_KEY = "QUERY_EXTRA_KEY";
  private static final String QUERY_EXTRA_ID_KEY = "QUERY_EXTRA_ID_KEY";
  public static final String QUERY_EXTRA_EPISODE_KEY = "QUERY_EXTRA_EPISODE_KEY";

  private ListView mListView;
  private Handler mHandler;
  
  private String mSearchString;
  private String mEpisodeString;
  
  @Override
  protected void onApplyThemeResource(Theme theme, int resid, boolean first) {
    super.onApplyThemeResource(theme, UiUtils.getThemeResourceId(UiUtils.TYPE_THEME_TOOLBAR, App.get().prefs().isDarkTheme()), first);
  }
  
  private ListView getListView() {
    return mListView;
  }
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final PrefUtils prefs = App.get().prefs();
    prefs.getDefault().registerOnSharedPreferenceChangeListener(this);
    
    setContentView(R.layout.list_view);
    
    mListView = findViewById(R.id.list_view);
    
    String[] projection = {
        TvBrowserContentProvider.DATA_KEY_UNIX_DATE,
        TvBrowserContentProvider.DATA_KEY_STARTTIME,
        TvBrowserContentProvider.DATA_KEY_ENDTIME,
        TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,
        TvBrowserContentProvider.DATA_KEY_TITLE,
        TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE,
        TvBrowserContentProvider.DATA_KEY_GENRE,
        TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT,
        TvBrowserContentProvider.DATA_KEY_CATEGORIES
    };
    
    registerForContextMenu(getListView());
    
    // Create a new Adapter an bind it to the List View
    
    mHandler = new Handler();

    final ProgramListViewBinderAndClickHandler viewAndClickHandler = new ProgramListViewBinderAndClickHandler(this,this,mHandler);
    mProgramsListAdapter = new OrientationHandlingCursorAdapter(this,R.layout.program_lists_entries,null,
        projection,new int[] {R.id.startDateLabelPL,R.id.startTimeLabelPL,R.id.endTimeLabelPL,
            R.id.channelLabelPL,R.id.titleLabelPL,R.id.episodeLabelPL,R.id.genre_label_pl,
            R.id.picture_copyright_pl,R.id.info_label_pl},0,true,mHandler);
    mProgramsListAdapter.setViewBinder(viewAndClickHandler);
    
    getListView().setAdapter(mProgramsListAdapter);
    
    mListView.setItemsCanFocus(true);
    
    // Initiate the Cursor Loader
    LoaderManager.getInstance(this).initLoader(0, null, this);
    
    // Get the launch Intent
    parseIntent(getIntent());
    
    SeparatorDrawable drawable = new SeparatorDrawable(this);
    
    getListView().setDivider(drawable);
    
    setDividerSize(prefs.getStringValueWithDefaultKey(R.string.PREF_PROGRAM_LISTS_DIVIDER_SIZE, R.string.pref_program_lists_divider_size_default));
  }
  
  @Override
  protected void onResume() {
    PluginHandler.incrementBlogCount();
    ProgramUtils.registerMarkingsListener(getApplicationContext(), this);
    super.onResume();
  }
  
  @Override
  protected void onPause() {
    PluginHandler.decrementBlogCount();
    ProgramUtils.unregisterMarkingsListener(getApplicationContext(), this);
    super.onPause();
  }
  
  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    parseIntent(getIntent());
  }
  
  private void parseIntent(Intent intent) {
    // If the Activity was started to service a Search request, extract the search query.
    if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
      mSearchString = intent.getStringExtra(SearchManager.QUERY);
      
      // Perform the search, passing in the search query as an argument to the Cursor Loader
      Bundle args = new Bundle();
      args.putString(QUERY_EXTRA_KEY, mSearchString);
      
      if(intent.hasExtra(QUERY_EXTRA_EPISODE_KEY)) {
        mEpisodeString = intent.getStringExtra(QUERY_EXTRA_EPISODE_KEY);
        
        args.putString(QUERY_EXTRA_EPISODE_KEY, mEpisodeString);
      }
      
      // Restart the Cursor Loader to execute the new query
      LoaderManager.getInstance(this).restartLoader(0, args, this);
    }
    else if(Intent.ACTION_VIEW.equals(intent.getAction())) {
      try {
        final Uri uri = intent.getData();

        if(uri != null) {
          long key = Long.parseLong(uri.getPathSegments().get(1));

          Bundle args = new Bundle();
          args.putLong(QUERY_EXTRA_ID_KEY, key);

          LoaderManager.getInstance(this).restartLoader(0, args, this);
        }
      }catch(NumberFormatException e) {
        // Ignore
      }
    }
    else if(intent.hasExtra(SearchManager.QUERY)) {
      Bundle args = new Bundle();
      mSearchString = intent.getStringExtra(SearchManager.QUERY);
      args.putString(QUERY_EXTRA_KEY, mSearchString);
      
      if(intent.hasExtra(QUERY_EXTRA_EPISODE_KEY)) {
        mEpisodeString = intent.getStringExtra(QUERY_EXTRA_EPISODE_KEY);
        
        args.putString(QUERY_EXTRA_EPISODE_KEY, mEpisodeString);
      }
      
      // Restart the Cursor Loader to execute the new query
      LoaderManager.getInstance(this).restartLoader(0, args, this);
    }
  }
  
  @NonNull
  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    String query = "0";
    String episodeQuery = "0";
    String operation = " OR ";
    
    long ID = -1;
    
    if(args != null) {
      // Extract the search query from the arguments.
      if(args.containsKey(QUERY_EXTRA_KEY) && args.getString(QUERY_EXTRA_KEY) != null) {
        episodeQuery = query = args.getString(QUERY_EXTRA_KEY);
      }
      
      ID = args.getLong(QUERY_EXTRA_ID_KEY, -1);
            
      if(args.containsKey(QUERY_EXTRA_EPISODE_KEY)) {
        episodeQuery = args.getString(QUERY_EXTRA_EPISODE_KEY);
        operation = " AND ";
      }
    }
    
    
    // Construct the new query in form of a Cursor Loader
    String[] projection;
    
    if(App.get().prefs().getBooleanValueWithDefaultKey(R.string.SHOW_PICTURE_IN_LISTS, R.bool.show_pictures_in_lists_default)) {
      projection = new String[14 + TvBrowserContentProvider.MARKING_COLUMNS.length];
      
      projection[projection.length-1] = TvBrowserContentProvider.DATA_KEY_PICTURE;
    }
    else {
      projection = new String[13 + TvBrowserContentProvider.MARKING_COLUMNS.length];
    }
    
    projection[0] = TvBrowserContentProvider.KEY_ID;
    projection[1] = TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID;
    projection[2] = TvBrowserContentProvider.DATA_KEY_STARTTIME;
    projection[3] = TvBrowserContentProvider.DATA_KEY_ENDTIME;
    projection[4] = TvBrowserContentProvider.DATA_KEY_TITLE;
    projection[5] = TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION;
    projection[6] = TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER;
    projection[7] = TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE;
    projection[8] = TvBrowserContentProvider.DATA_KEY_GENRE;
    projection[9] = TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT;
    projection[10] = TvBrowserContentProvider.DATA_KEY_UNIX_DATE;
    projection[11] = TvBrowserContentProvider.CHANNEL_KEY_NAME;
    projection[12] = TvBrowserContentProvider.DATA_KEY_CATEGORIES;
    
    System.arraycopy(TvBrowserContentProvider.MARKING_COLUMNS, 0, projection, 13, TvBrowserContentProvider.MARKING_COLUMNS.length);
        
    String where = "(" + TvBrowserContentProvider.DATA_KEY_TITLE + " LIKE '%" + query.replace("'", "''") + "%' " + operation + TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE + " LIKE '%" + episodeQuery.replace("'", "''") + "%') AND " + TvBrowserContentProvider.DATA_KEY_ENDTIME + ">=" + System.currentTimeMillis();
    String sortOrder = TvBrowserContentProvider.DATA_KEY_STARTTIME;
    
    Uri uri = TvBrowserContentProvider.CONTENT_URI_DATA_WITH_CHANNEL;
    
    if(ID != -1) {
      where = null;
      uri = ContentUris.withAppendedId(uri, ID);
    }
    else {
      where += UiUtils.getDontWantToSeeFilterString(ActivityTvBrowserSearchResults.this);
    }
    
    // Create the new Cursor loader
    return new CursorLoader(this, uri, projection, where, null, sortOrder);
  }

  @Override
  public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
    mProgramsListAdapter.swapCursor(cursor);
    
    if(cursor == null || cursor.getCount() < 1) {
      AlertDialog.Builder info = new AlertDialog.Builder(ActivityTvBrowserSearchResults.this);
      
      info.setTitle(R.string.search_no_result_title);
      info.setMessage(R.string.search_no_result_text);
      
      info.setPositiveButton(R.string.dialog_search_create_favorite, (dialog, which) -> {
        mHandler.post(() -> UiUtils.editFavorite(null, getApplicationContext(), mSearchString + (mEpisodeString != null ? " AND " + mEpisodeString : "")));

        finish();
      });
      
      info.setNegativeButton(R.string.dialog_close, (dialog, which) -> finish());
      
      info.show();
    }
  }

  @Override
  public void onLoaderReset(@NonNull Loader<Cursor> loader) {
    mProgramsListAdapter.swapCursor(null);
  }
  
  private void setDividerSize(String size) {    
    getListView().setDividerHeight(UiUtils.convertDpToPixel(Integer.parseInt(size), getResources()));
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    if(getString(R.string.PREF_PROGRAM_LISTS_DIVIDER_SIZE).equals(key)) {
      setDividerSize(App.get().prefs().getStringValueWithDefaultKey(R.string.PREF_PROGRAM_LISTS_DIVIDER_SIZE, R.string.pref_program_lists_divider_size_default));
    }
  }
  
  @Override
  public void onConfigurationChanged(@NonNull Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    
    SettingConstants.ORIENTATION = newConfig.orientation;
  }

  @Override
  public boolean showDate() {
    return true;
  }

  @Override
  public void refreshMarkings() {
    mHandler.post(() -> getListView().invalidateViews());
  }
}
