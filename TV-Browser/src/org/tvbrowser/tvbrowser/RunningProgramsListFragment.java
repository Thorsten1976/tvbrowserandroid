package org.tvbrowser.tvbrowser;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.tvbrowser.content.TvBrowserContentProvider;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class RunningProgramsListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
  private static final String WHERE_CLAUSE_KEY = "WHERE_CLAUSE_KEY";
  SimpleCursorAdapter adapter;
  
  private Handler handler = new Handler();
  
  private boolean mKeepRunning;
  private Thread mUpdateThread;
  private int mWhereClauseID;
  
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
  
  public void setWhereClauseID(int id) {
    mWhereClauseID = id;
    
    if(mKeepRunning) {
      handler.post(new Runnable() {
        @Override
        public void run() {
          if(!isDetached()) {
            getLoaderManager().restartLoader(0, null, RunningProgramsListFragment.this);
          }
        }
      });
    }
  }
  
  @Override
  public void onSaveInstanceState(Bundle outState) {
    Log.d("test", "SAVE");
    
    outState.putInt(WHERE_CLAUSE_KEY, mWhereClauseID);
    super.onSaveInstanceState(outState);
  }
    
  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    
    Log.d("test", "bundle " + savedInstanceState);
    
    if(savedInstanceState != null) {
      mWhereClauseID = savedInstanceState.getInt(WHERE_CLAUSE_KEY,R.id.now_button);
    }
    else {
      mWhereClauseID = R.id.now_button;
    }
    
    registerForContextMenu(getListView());
    
    String[] projection = {
        TvBrowserContentProvider.DATA_KEY_STARTTIME,
        TvBrowserContentProvider.DATA_KEY_ENDTIME,
        TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,
        TvBrowserContentProvider.DATA_KEY_TITLE,
        TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE,
        TvBrowserContentProvider.DATA_KEY_GENRE
    };
    
    // Create a new Adapter an bind it to the List View
    adapter = new SimpleCursorAdapter(getActivity(),/*android.R.layout.simple_list_item_1*/R.layout.running_list_entries,null,
        projection,new int[] {R.id.startTimeLabel,R.id.endTimeLabel,R.id.channelLabel,R.id.titleLabel,R.id.episodeLabel,R.id.genre_label},0);
    adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
      @Override
      public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        Log.d("TVB", " COLUMN " + columnIndex + " " + cursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID));
        if(columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID)) {
          int channelID = cursor.getInt(cursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID));
          
          Cursor channel = getActivity().getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_CHANNELS, channelID), null, null, null, null);
          //Log.d("TVB", " CHANNELCURSOR " + channel.getCount());
          if(channel.getCount() > 0) {
            channel.moveToFirst();
            TextView text = (TextView)view;
            text.setText(channel.getString(channel.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME)));
          }
          channel.close();
          
          return true;
        }
        else if(columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME)) {
          long date = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME));
          
          TextView text = (TextView)view;
          text.setTag(cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.KEY_ID)));
          text.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(date)));
          
          return true;
        }
        else if(columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME)) {
          long date = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME));
          
          TextView text = (TextView)view;
          text.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(date)));
          
          String value = cursor.getString(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES));
          
          if(value != null && value.trim().length() > 0) {
            if(value.contains("calendar")) {
              ((RelativeLayout)view.getParent()).setBackgroundResource(R.color.mark_color_calendar);
            }
            else {
              ((RelativeLayout)view.getParent()).setBackgroundResource(R.color.mark_color);
            }
          }
          else {
            ((RelativeLayout)view.getParent()).setBackgroundResource(android.R.drawable.list_selector_background);
            //((LinearLayout)view.getParent()).setBackgroundColor(view.getBackground().get);
          }
          
          return true;
        }
        else if(columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE)) {
          TextView text = (TextView)view;
          
          long end = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME));
          long start = cursor.getLong(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME));
          
          if(end <= System.currentTimeMillis()) {
            text.setTextColor(Color.rgb(200, 200, 200));
            ((TextView)((RelativeLayout)text.getParent()).findViewById(R.id.episodeLabel)).setTextColor(Color.rgb(200, 200, 200));
            ((TextView)((RelativeLayout)text.getParent()).findViewById(R.id.genre_label)).setTextColor(Color.rgb(200, 200, 200));
          }
          else if(System.currentTimeMillis() >= start && System.currentTimeMillis() <= end) {
            text.setTextColor(getActivity().getResources().getColor(R.color.running_color));
            ((TextView)((RelativeLayout)text.getParent()).findViewById(R.id.episodeLabel)).setTextColor(getActivity().getResources().getColor(R.color.running_color));
            ((TextView)((RelativeLayout)text.getParent()).findViewById(R.id.genre_label)).setTextColor(getActivity().getResources().getColor(R.color.running_color));
          }
          else {
            int[] attrs = new int[] { android.R.attr.textColorSecondary };
            TypedArray a = getActivity().getTheme().obtainStyledAttributes(R.style.AppTheme, attrs);
            int DEFAULT_TEXT_COLOR = a.getColor(0, Color.BLACK);
            a.recycle();
            
            text.setTextColor(DEFAULT_TEXT_COLOR);
            ((TextView)((RelativeLayout)text.getParent()).findViewById(R.id.episodeLabel)).setTextColor(DEFAULT_TEXT_COLOR);
            ((TextView)((RelativeLayout)text.getParent()).findViewById(R.id.genre_label)).setTextColor(DEFAULT_TEXT_COLOR);
          }
          //
        }
        else if(columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE)) {
          if(cursor.isNull(cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE))) {
            view.setVisibility(View.GONE);
          }
          else {
            view.setVisibility(View.VISIBLE);
          }
        }
        else if(columnIndex == cursor.getColumnIndex(TvBrowserContentProvider.DATA_KEY_GENRE)) {
          TextView text = (TextView)view;
          
          if(cursor.isNull(columnIndex)) {
            text.setVisibility(View.GONE);
          }
          else {
            text.setVisibility(View.VISIBLE);
          }
        }
        
        return false;
      }
  });
    
    setListAdapter(adapter);
    
    getLoaderManager().initLoader(0, null, this);
  }
  
  @Override
  public void onDetach() {
    super.onDetach();
    
    mKeepRunning = false;
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
                    getLoaderManager().restartLoader(0, null, RunningProgramsListFragment.this);
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
        TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,
        TvBrowserContentProvider.DATA_KEY_STARTTIME,
        TvBrowserContentProvider.DATA_KEY_ENDTIME,
        TvBrowserContentProvider.DATA_KEY_TITLE,
        TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION,
        TvBrowserContentProvider.DATA_KEY_MARKING_VALUES,
        TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER,
        TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE,
        TvBrowserContentProvider.DATA_KEY_GENRE
    };
    
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 30);
    
    switch(mWhereClauseID) {
      case R.id.button_6: 
        cal.set(Calendar.HOUR_OF_DAY, 6);break;
      case R.id.button_12:
        cal.set(Calendar.HOUR_OF_DAY, 12);break;
      case R.id.button_16:
        cal.set(Calendar.HOUR_OF_DAY, 16);break;
      case R.id.button_2015:
        cal.set(Calendar.HOUR_OF_DAY, 20);
        cal.set(Calendar.MINUTE, 15);break;
      case R.id.button_23:
        cal.set(Calendar.HOUR_OF_DAY, 23);break;
      default: cal.setTimeInMillis(System.currentTimeMillis());break;
    }

    long time = ((long)cal.getTimeInMillis() / 60000) * 60000;

    String where = TvBrowserContentProvider.DATA_KEY_STARTTIME + " <= " + time + " AND " + TvBrowserContentProvider.DATA_KEY_ENDTIME + " > " + time;
    
    CursorLoader loader = new CursorLoader(getActivity(), TvBrowserContentProvider.CONTENT_URI_DATA_WITH_CHANNEL, projection, where, null, TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER + " , " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " , " + TvBrowserContentProvider.DATA_KEY_STARTTIME);
    
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
  /*
  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    super.onListItemClick(l, v, position, id);
    
    Object o = v.findViewById(R.id.startTimeLabel).getTag();
    
    if(o instanceof Long) {
      showPopupMenu(v,(Long)o);
      Log.d("TVB", String.valueOf(o));
    }
  }
  
  private void showPopupMenu(final View v, final long programID) {
    PopupMenu popup = new PopupMenu(getActivity(), v);
    popup.getMenuInflater().inflate(R.menu.popupmenu, popup.getMenu());
    
    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
      @Override
      public boolean onMenuItemClick(MenuItem item) {
        Cursor info = getActivity().getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programID), new String[] {TvBrowserContentProvider.DATA_KEY_MARKING_VALUES}, null, null,null);
        
        String current = null;
        
        if(info.getCount() > 0) {
          info.moveToFirst();
          
          if(!info.isNull(0)) {
            current = info.getString(0);
          }
        }
        
        ContentValues values = new ContentValues();
        
        if(item.getItemId() == R.id.mark_item) {
          if(current != null && current.contains("marked")) {
            return true;
          }
          else if(current == null) {
            values.put(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES, "marked");
          }
          else {
            values.put(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES, current + ";marked");
          }
          
          
          Log.d("TVB","MARK " + programID);
          
        }
        else {
          if(current == null || current.trim().length() == 0) {
            return true;
          }
          
          if(current.contains(";marked")) {
            current = current.replace(";marked", "");
          }
          else if(current.contains("marked;")) {
            current = current.replace("marked;", "");
          }
          else if(current.contains("marked")) {
            current = current.replace("marked", "");
          }
          
          Log.d("TVB", String.valueOf(current));
          
          values.put(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES, current);
          
          Log.d("TVB","UNMARK " + programID);
        }
        
        v.invalidate();
        getActivity().getContentResolver().update(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programID), values, null, null);
        
        return true;
      }
    });
    
    popup.show();
  }*/
  
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v,
      ContextMenuInfo menuInfo) {
    getActivity().getMenuInflater().inflate(R.menu.popupmenu, menu);
  }
  
  @Override
  public boolean onContextItemSelected(MenuItem item) {
    // TODO Auto-generated method stub
    Log.d("test", "clickde");
    long programID = ((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).id;
    
    Cursor info = getActivity().getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programID), new String[] {TvBrowserContentProvider.DATA_KEY_MARKING_VALUES}, null, null,null);
    
    String current = null;
    
    if(info.getCount() > 0) {
      info.moveToFirst();
      
      if(!info.isNull(0)) {
        current = info.getString(0);
      }
    }
    
    info.close();
    
    ContentValues values = new ContentValues();
    
    if(item.getItemId() == R.id.mark_item) {
      if(current != null && current.contains("marked")) {
        return true;
      }
      else if(current == null) {
        values.put(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES, "marked");
      }
      else {
        values.put(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES, current + ";marked");
      }
      
      Log.d("TVB","MARK " + programID);
      
    }
    else if(item.getItemId() == R.id.unmark_item){
      if(current == null || current.trim().length() == 0) {
        return true;
      }
      /*
      if(current.contains(";marked")) {
        current = current.replace(";marked", "");
      }
      else if(current.contains("marked;")) {
        current = current.replace("marked;", "");
      }
      else if(current.contains("marked")) {
        current = current.replace("marked", "");
      }
      */
      
      current = "";
      
      Log.d("TVB", String.valueOf(current));
      
      values.put(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES, current);
      
      Log.d("TVB","UNMARK " + programID);
    }
    else if(item.getItemId() == R.id.create_calendar_entry) {
      info = getActivity().getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programID), new String[] {TvBrowserContentProvider.KEY_ID,TvBrowserContentProvider.DATA_KEY_STARTTIME,TvBrowserContentProvider.DATA_KEY_ENDTIME,TvBrowserContentProvider.DATA_KEY_TITLE,TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID,TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION,TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE}, null, null,null);
      
      if(info.getCount() > 0) {
        info.moveToFirst();
        
        Cursor channel = getActivity().getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_CHANNELS, info.getLong(info.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID))), new String[] {TvBrowserContentProvider.CHANNEL_KEY_NAME}, null, null, null);
        Log.d("TVB", "channel cursor " + channel.getCount());
        if(channel.getCount() > 0) {
          channel.moveToFirst();
       // Create a new insertion Intent.
          Intent addCalendarEntry = new Intent(Intent.ACTION_EDIT/*, CalendarContract.Events.CONTENT_URI*/);
          Log.d("TVB", getActivity().getContentResolver().getType(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI,1)));
          
          //Intent intent = new Intent(Intent.ACTION_INSERT);
          addCalendarEntry.setType(getActivity().getContentResolver().getType(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI,1)));
               
          //addCalendarEntry.putExtra(Events.STATUS, 1);
          //addCalendarEntry.putExtra(Events.VISIBLE, 0);
          //addCalendarEntry.putExtra(Events.HAS_ALARM, 1);
          
          String desc = null;
          
          if(!info.isNull(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION))) {
            desc = info.getString(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION));
            
            if(desc != null && desc.trim().toLowerCase().equals("null")) {
              desc = null;
            }
          }
          
          String episode = null;
          
          if(!info.isNull(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE))) {
            episode = info.getString(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE));
            
            if(episode != null && episode.trim().toLowerCase().equals("null")) {
              episode = null;
            }
          }
          addCalendarEntry.putExtra(Events.EVENT_LOCATION, channel.getString(0));
          // Add the calendar event details
          addCalendarEntry.putExtra(Events.TITLE, info.getString(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE)));
          
          String description = null;
          
          if(episode != null) {
            description = episode;
          }
          
          if(desc != null) {
            if(description != null) {
              description += "\n\n" + desc;
            }
            else {
              description = desc;
            }
          }
          
          if(description != null) {
            addCalendarEntry.putExtra(Events.DESCRIPTION, description);
          }
          
          addCalendarEntry.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,info.getLong(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME)));
          addCalendarEntry.putExtra(CalendarContract.EXTRA_EVENT_END_TIME,info.getLong(info.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME)));
          
          // Use the Calendar app to add the new event.
          startActivity(addCalendarEntry);
          
          if(current != null && current.contains("calendar")) {
            return true;
          }
          else if(current == null) {
            values.put(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES, "calendar");
          }
          else {
            values.put(TvBrowserContentProvider.DATA_KEY_MARKING_VALUES, current + ";calendar");
          }
        }
        
        channel.close();
      }
      
      info.close();
    }
    
    if(values.size() > 0) {
      /*if(marked) {
        Log.d("TVB", String.valueOf(((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).targetView));
        ((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).targetView.setBackgroundResource(R.color.mark_color);//.invalidate();
      }
      else {
        ((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).targetView.setBackgroundResource(android.R.drawable.list_selector_background);//.invalidate();
      }*/
      ((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).targetView.invalidate();
     // item.get v.invalidate();
      getActivity().getContentResolver().update(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, programID), values, null, null);
    }
    
    return true;
  }
  
  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    super.onListItemClick(l, v, position, id);
    
    Cursor c = getActivity().getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, id), null, null, null, null);
    
    c.moveToFirst();
    
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    
    TableLayout table = new TableLayout(builder.getContext());
    table.setShrinkAllColumns(true);
    
    TableRow row = new TableRow(table.getContext());
    TableRow row0 = new TableRow(table.getContext());
    
    TextView date = new TextView(row.getContext());
    date.setTextColor(Color.rgb(200, 0, 0));
    date.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
    
    Date start = new Date(c.getLong(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME)));
    SimpleDateFormat day = new SimpleDateFormat("EEE",Locale.getDefault());
    
    long channelID = c.getLong(c.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID));
    
    Cursor channel = getActivity().getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_CHANNELS, channelID), new String[] {TvBrowserContentProvider.CHANNEL_KEY_NAME}, null, null, null);
    
    channel.moveToFirst();
    
    date.setText(day.format(start) + " " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(start) + " - " + DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(c.getLong(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME)))) + " " + channel.getString(0));
    
    channel.close();
    
    row0.addView(date);
    
    TextView title = new TextView(row.getContext());
    title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
    title.setTypeface(null, Typeface.BOLD);
    title.setText(c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE)));
    
    row.addView(title);
    
    table.addView(row0);
    table.addView(row);
    
    if(!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_GENRE))) {
      TextView genre = new TextView(table.getContext());
      genre.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
      genre.setTypeface(null, Typeface.ITALIC);
      genre.setText(c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_GENRE)));
      
      TableRow rowGenre = new TableRow(table.getContext());
      
      rowGenre.addView(genre);
      table.addView(rowGenre);
    }
    
    if(!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE))) {
      TextView episode = new TextView(table.getContext());
      episode.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
      episode.setText(c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE)));
      episode.setTextColor(Color.GRAY);
      
      TableRow rowEpisode = new TableRow(table.getContext());
      
      rowEpisode.addView(episode);
      table.addView(rowEpisode);
    }
    
    if(!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION))) {
      TextView desc = new TextView(table.getContext());
      desc.setText(c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION)));
     /* desc.setSingleLine(false);*/
      desc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
      /*desc.setInputType(InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE);*/
      
      TableRow rowDescription = new TableRow(table.getContext());
      
      rowDescription.addView(desc);
      table.addView(rowDescription);
    }
    

    
    
    

    
    c.close();
        
    builder.setView(table);
    builder.show();
    
  }
}
