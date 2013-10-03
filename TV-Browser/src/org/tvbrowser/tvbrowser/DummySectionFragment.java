package org.tvbrowser.tvbrowser;

import org.tvbrowser.content.TvBrowserContentProvider;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

public class DummySectionFragment extends Fragment {
  /**
   * The fragment argument representing the section number for this fragment.
   */
  public static final String ARG_SECTION_NUMBER = "section_number";

  public DummySectionFragment() {
  }
  
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View rootView = null;
    
    if(getArguments().getInt(ARG_SECTION_NUMBER) == 1) {
      rootView = inflater.inflate(R.layout.running_program_fragment,
          container, false);
      
      final RunningProgramsListFragment running = (RunningProgramsListFragment)getActivity().getSupportFragmentManager().findFragmentById(R.id.runningListFragment);
      
      View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          if(running != null) {
            running.setWhereClauseID(v.getId());
          }
        }
      };
      
      rootView.findViewById(R.id.now_button).setOnClickListener(listener);
      rootView.findViewById(R.id.button_6).setOnClickListener(listener);
      rootView.findViewById(R.id.button_12).setOnClickListener(listener);
      rootView.findViewById(R.id.button_16).setOnClickListener(listener);
      rootView.findViewById(R.id.button_2015).setOnClickListener(listener);
      rootView.findViewById(R.id.button_23).setOnClickListener(listener);
    }
    else if(getArguments().getInt(ARG_SECTION_NUMBER) == 2) {
        rootView = inflater.inflate(R.layout.program_list_fragment,
            container, false);
        
        ContentResolver cr = getActivity().getContentResolver();
        
        StringBuilder where = new StringBuilder(TvBrowserContentProvider.CHANNEL_KEY_SELECTION);
        where.append(" = 1");
        
        Cursor channelCursor = cr.query(TvBrowserContentProvider.CONTENT_URI_CHANNELS, null, where.toString(), null, TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER + " , " + TvBrowserContentProvider.GROUP_KEY_GROUP_ID);
        
        if(channelCursor.getCount() > 0) {
          channelCursor.moveToFirst();
          
          LinearLayout parent = (LinearLayout)rootView.findViewById(R.id.button_bar);
          final ProgramsListFragment programList = (ProgramsListFragment)getActivity().getSupportFragmentManager().findFragmentById(R.id.programListFragment);
          View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              
              
              if(programList != null) {
                programList.setChannelID((Long)v.getTag());
              }
            }
          };
          
          Button all = (Button)parent.findViewById(R.id.all_channels);
          all.setTag(Long.valueOf(-1));
          all.setOnClickListener(listener);
          
          do {
            Button channelButton = new Button(getActivity(),null,android.R.attr.buttonBarButtonStyle);
            channelButton.setTag(channelCursor.getLong(channelCursor.getColumnIndex(TvBrowserContentProvider.KEY_ID)));
            
            channelButton.setOnClickListener(listener);
            
            channelButton.setText(channelCursor.getString(channelCursor.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME)));
            
            parent.addView(channelButton);
          }while(channelCursor.moveToNext());
        }
        
        channelCursor.close();
    }
    else {
      rootView = inflater.inflate(R.layout.fragment_tv_browser_dummy,
        container, false);
    }
    
    return rootView;
  }
}
