package org.tvbrowser.tvbrowser;

import org.tvbrowser.content.TvBrowserContentProvider;

import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class ProgramsListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
  SimpleCursorAdapter adapter;
  
  private Handler handler = new Handler();
  
  private boolean mKeepRunning;
  private Thread mUpdateThread;
  
  private long mChannelID;
  
  private ProgramListViewBinderAndClickHandler mViewAndClickHandler;
  
  @Override
  public void onResume() {
    super.onResume();
    
    mKeepRunning = true;
    
    createUpdateThread();
    
    mUpdateThread.start();
  }
  
  @Override
  public void onPause() {
    super.onPause();
    
    mKeepRunning = false;
  }
  
  @Override
  public void onDetach() {
    super.onDetach();
    
    mKeepRunning = false;
  }
  
  public void setChannelID(long id) {
    mChannelID = id;
    
    handler.post(new Runnable() {
      @Override
      public void run() {
        if(!isDetached()) {
          getLoaderManager().restartLoader(0, null, ProgramsListFragment.this);
        }
      }
    });
  }
  
  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    mChannelID = -1;
    registerForContextMenu(getListView());
    
    String[] projection = {
        TvBrowserContentProvider.DATA_KEY_UNIX_DATE,
        TvBrowserContentProvider.DATA_KEY_STARTTIME,
        TvBrowserContentProvider.DATA_KEY_ENDTIME,
        TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,
        TvBrowserContentProvider.DATA_KEY_TITLE,
        TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE,
        TvBrowserContentProvider.DATA_KEY_GENRE
    };
    
    mViewAndClickHandler = new ProgramListViewBinderAndClickHandler(getActivity());
    
    // Create a new Adapter an bind it to the List View
    adapter = new SimpleCursorAdapter(getActivity(),/*android.R.layout.simple_list_item_1*/R.layout.program_list_entries,null,
        projection,new int[] {R.id.startDateLabelPL,R.id.startTimeLabelPL,R.id.endTimeLabelPL,R.id.channelLabelPL,R.id.titleLabelPL,R.id.episodeLabelPL,R.id.genre_label_pl},0);
    adapter.setViewBinder(mViewAndClickHandler);
    
    setListAdapter(adapter);
    
    getLoaderManager().initLoader(0, null, this);
  }
  
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {
    mViewAndClickHandler.onCreateContextMenu(menu, v, menuInfo);
  }
  
  @Override
  public boolean onContextItemSelected(MenuItem item) {
    return mViewAndClickHandler.onContextItemSelected(item);
  }
  
  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    super.onListItemClick(l, v, position, id);
    
    mViewAndClickHandler.onListItemClick(l, v, position, id);
  }
  
  private void createUpdateThread() {
    mUpdateThread = new Thread() {
      public void run() {
        while(mKeepRunning) {
          try {
            if(mKeepRunning) {
              handler.post(new Runnable() {
                @Override
                public void run() {
                  if(!isDetached()) {
                    getLoaderManager().restartLoader(0, null, ProgramsListFragment.this);
                  }
                }
              });
            }
            sleep(60000);
          } catch (InterruptedException e) {
          }
        }
      }
    };
  }

  @Override
  public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
    String[] projection = {
        TvBrowserContentProvider.KEY_ID,
        TvBrowserContentProvider.DATA_KEY_UNIX_DATE,
        TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,
        TvBrowserContentProvider.DATA_KEY_STARTTIME,
        TvBrowserContentProvider.DATA_KEY_ENDTIME,
        TvBrowserContentProvider.DATA_KEY_TITLE,
        TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION,
        TvBrowserContentProvider.DATA_KEY_MARKING_VALUES,
        TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER,
        TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE,
        TvBrowserContentProvider.CHANNEL_KEY_NAME,
        TvBrowserContentProvider.DATA_KEY_GENRE
    };
    
    String where = " ( " + TvBrowserContentProvider.DATA_KEY_STARTTIME + " <= " + System.currentTimeMillis() + " AND " + TvBrowserContentProvider.DATA_KEY_ENDTIME + " >= " + System.currentTimeMillis();
    where += " OR " + TvBrowserContentProvider.DATA_KEY_STARTTIME + " > " + System.currentTimeMillis() + " ) ";
    
    if(mChannelID != -1) {
      where += "AND " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " = " + mChannelID;
    }
    
    CursorLoader loader = new CursorLoader(getActivity(), TvBrowserContentProvider.CONTENT_URI_DATA_WITH_CHANNEL, projection, where, null, TvBrowserContentProvider.DATA_KEY_STARTTIME + " , " + TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER + " , " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID);
    
    return loader;
  }

  @Override
  public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader,
      Cursor c) {
    adapter.swapCursor(c);
  }

  @Override
  public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {
    adapter.swapCursor(null);
  }
}
