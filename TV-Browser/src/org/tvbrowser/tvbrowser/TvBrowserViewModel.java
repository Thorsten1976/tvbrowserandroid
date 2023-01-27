package org.tvbrowser.tvbrowser;

import static android.content.Context.CONNECTIVITY_SERVICE;

import static org.tvbrowser.content.TvBrowserContentProvider.CHANNEL_KEY_SELECTION;
import static org.tvbrowser.content.TvBrowserContentProvider.CONTENT_URI_CHANNELS;
import static org.tvbrowser.content.TvBrowserContentProvider.CONTENT_URI_GROUPS;
import static org.tvbrowser.content.TvBrowserContentProvider.GROUP_KEY_DATA_SERVICE_ID;
import static org.tvbrowser.content.TvBrowserContentProvider.GROUP_KEY_GROUP_ID;
import static org.tvbrowser.content.TvBrowserContentProvider.KEY_ID;
import static org.tvbrowser.settings.SettingConstants.CHANNEL_UPDATE_DONE;
import static org.tvbrowser.settings.SettingConstants.DATA_UPDATE_DONE;
import static org.tvbrowser.settings.SettingConstants.EPG_DONATE_KEY;
import static org.tvbrowser.utils.PrefUtils.TYPE_PREFERENCES_MARKINGS;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.SavedStateHandle;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.WorkManager;

import org.tvbrowser.App;
import org.tvbrowser.utils.IOUtils;
import org.tvbrowser.utils.UiUtils;

import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * A {@link androidx.lifecycle.ViewModel} that holds the (UI) state of the {@link TvBrowser}
 * main activity and dependant fragments and views. In a second refactoring step all logic should be
 * located in a repository that is utilized from this view model; i.e. storage, networking, etc.
 */
@HiltViewModel
final class TvBrowserViewModel extends AndroidViewModel {

    private final SavedStateHandle mSavedStateHandle;
    private final WorkManager mWorkManager;

    @Inject
    TvBrowserViewModel(@NonNull final Application application, @NonNull final SavedStateHandle savedStateHandle) {
        super(application);
        mSavedStateHandle = savedStateHandle;
        mWorkManager = WorkManager.getInstance(application);
    }

    // FIXME delegate to IOUtils or own DbUtils class to reduce class size (maintainability, memory)
    @SuppressLint("Range")
    int getEpgDonateChannelsCount() {
        int result = 0;
        Log.d("info6", "getEpgDonateChannelsCount");
        try {
            final String[] projection = new String[] {KEY_ID};
            int epgDonateKey = -1;

            if (IOUtils.isDatabaseAccessible(getApplication())) {
                Log.d("info6", "groups");
                Cursor groups = null;
                Log.d("info6", "groups loaded");
                try {
                    groups = getApplication().getContentResolver().query(CONTENT_URI_GROUPS, projection,
                            GROUP_KEY_DATA_SERVICE_ID + "=\"" + EPG_DONATE_KEY +"\"", null, null);
                    if (groups!=null && IOUtils.prepareAccessFirst(groups)) {
                        epgDonateKey = groups.getInt(groups.getColumnIndex(KEY_ID));
                    }
                } finally {
                    IOUtils.close(groups);
                }

                if (epgDonateKey != -1 && IOUtils.isDatabaseAccessible(getApplication())) {
                    Log.d("info6", "epgDonateSubscribedChannels");
                    Cursor epgDonateSubscribedChannels = null;
                    Log.d("info6", "epgDonateSubscribedChannels loaded");
                    try {
                        epgDonateSubscribedChannels = getApplication().getContentResolver().query(
                                CONTENT_URI_CHANNELS, projection, " ( " + GROUP_KEY_GROUP_ID + "=" + epgDonateKey + " ) AND " +
                                        CHANNEL_KEY_SELECTION, null, null);
                        result = epgDonateSubscribedChannels != null ? epgDonateSubscribedChannels.getCount() : 0;
                    } finally {
                        IOUtils.close(epgDonateSubscribedChannels);
                    }
                }
            }
        } catch (Throwable t) {
            Log.d("info6", "", t);
        }
        return result;
    }

    // FIXME delegate to repository or settings class (maintainability, reduced memory consumption)
    @NonNull
    String getMarkingData() {
        final Map<String,?> mark = App.get().prefs().getShared(TYPE_PREFERENCES_MARKINGS).getAll();
        final StringBuilder markValue = new StringBuilder();
        for (String key : mark.keySet()) {
            markValue.append(key).append("=").append(mark.get(key)).append("\n");
        }
        if (markValue.length() == 0) {
            markValue.append("NO PLUGIN MARKINGS");
        }
        return markValue.toString();
    }

    // FIXME delegate to IOUtils or own DbUtils class to reduce class size (maintainability, memory)
    boolean hasChannels() {
        boolean result = false;
        if (IOUtils.isDatabaseAccessible(getApplication())) {
            Cursor c = null;
            try {
                c = getApplication().getContentResolver().query(CONTENT_URI_CHANNELS,
                        new String[] {KEY_ID}, null, null, null);
                result = c != null && c.getCount() > 0;
            } finally {
                IOUtils.close(c);
            }
        }
        return result;
    }

    // FIXME delegate to IOUtils (duplicate from TvBrowser class)
    boolean isOnline() {
        final ConnectivityManager cm = (ConnectivityManager) getApplication().getSystemService(CONNECTIVITY_SERVICE);
        final NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    void sendChannelFilterUpdate() {
        getApplication().sendBroadcast(new Intent(DATA_UPDATE_DONE));
        UiUtils.updateRunningProgramsWidget(getApplication());
        updateProgramListChannelBar();
    }

    void updateProgramListChannelBar() {
        LocalBroadcastManager.getInstance(getApplication()).sendBroadcast(
                new Intent(CHANNEL_UPDATE_DONE));
    }
}