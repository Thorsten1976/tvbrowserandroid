package org.tvbrowser.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import org.tvbrowser.App;
import org.tvbrowser.content.TvBrowserContentProvider;
import org.tvbrowser.settings.SettingConstants;
import org.tvbrowser.tvbrowser.Favorite;
import org.tvbrowser.tvbrowser.InfoActivity;
import org.tvbrowser.tvbrowser.R;
import org.tvbrowser.tvbrowser.ServiceUpdateRemindersAndAutoUpdate;
import org.tvbrowser.view.SwipeScrollView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AsyncInfo extends AsyncTask<Void, Void, Boolean> {
    static final Map<String, Integer> VALUE_MAP;
    static {
        VALUE_MAP = new HashMap<>();
        VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_ACTORS, R.id.detail_actors);
        VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_REGIE, R.id.detail_regie);
        VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_CUSTOM_INFO, R.id.detail_custom);
        VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_AGE_LIMIT, R.id.detail_age_limit);
        VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_NETTO_PLAY_TIME, R.id.detail_netto_playtime);
        VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_VPS, R.id.detail_vps);
        VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_SCRIPT, R.id.detail_script);
        VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_REPETITION_FROM, R.id.detail_repetition_from);
        VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_MUSIC, R.id.detail_music);
        VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_MODERATION, R.id.detail_moderation);
        VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_REPETITION_ON, R.id.detail_repetition_on);
        VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_SEASON_NUMBER, R.id.detail_season);
        VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_PRODUCER, R.id.detail_producer);
        VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_CAMERA, R.id.detail_camera);
        VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_CUT, R.id.detail_cut);
        VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_OTHER_PERSONS, R.id.detail_other_persons);
        VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_PRODUCTION_FIRM, R.id.detail_production_firm);
        VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_AGE_LIMIT_STRING, R.id.detail_age_limit_string);
        VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_YEAR_PRODUCTION_FIRST, R.id.detail_year_production_first);
        VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_LAST_PRODUCTION_YEAR, R.id.detail_last_production_year);
        VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_ADDITIONAL_INFO, R.id.detail_additional_info);
        VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_SERIES, R.id.detail_series);
        VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_RATING, R.id.detail_rating);
        VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_GENRE_SUB_LIST, R.id.detail_subgenre_list);
        VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_VOD_INFO, R.id.detail_vod_info);
        VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_VOD_LINK, R.id.detail_vod_link);
        VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_KEYWORD_LIST, R.id.detail_keyword_list);
        VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_PART_NUMBER, R.id.detail_part_numer);
        VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_PART_NUMBER_TOTAL, R.id.detail_parts_total);
        VALUE_MAP.put(TvBrowserContentProvider.DATA_KEY_DURATION_ORIGINAL, R.id.detail_duration_original);
    }

    private final Context context;
    private View mLayout;
    private final View parent;

    private ProgressDialog mProgress;
    private Handler mHandler;
    private Runnable mShowWaiting;
    private boolean mShowWaitingDialog;
    private boolean mHasSpannableActors;

    private long mPreviousId;
    private long mNextId;

    private final long id;
    private final WeakReference<Activity> finish;
    private final Handler handler;

    public static boolean mShowingProgramInfo = false;

    AsyncInfo(final Context context, final long id, final Activity finish, final View parent, final Handler handler) {
        this.context = context;
        this.id = id;
        this.finish = new WeakReference<>(finish);
        this.parent = parent;
        this.handler = handler;
    }

    public static synchronized void showProgramInfo(final Context context, final long id, final Activity finish, final View parent, final Handler handler) {
        if (!mShowingProgramInfo && IOUtils.isDatabaseAccessible(context)) {
            mShowingProgramInfo = true;

            handler.post(() -> new AsyncInfo(context, id, finish, parent, handler).execute());
        }
    }

    public static void showProgramInfo(final Context context, long id, View parent, Handler handler) {
        showProgramInfo(context, id, null, parent, handler);
    }

    private static void checkAndAddHiglightingForFavorites(TextView view, CharSequence text, ArrayList<FavoriteTypePattern> patternList, boolean allPatterns, BackgroundColorSpan backgroundColorSpan) {
        SpannableStringBuilder string = new SpannableStringBuilder(text);
        for (FavoriteTypePattern typePattern : patternList) {
            if (allPatterns || typePattern.isNotOnlyTitleType()) {
                string = typePattern.addHighlighting(string, backgroundColorSpan);
            }
        }
        view.setText(string);
    }

    @Override
    protected void onPreExecute() {
        mPreviousId = -1;
        mNextId = -1;

        mShowWaitingDialog = true;
        mHandler = new Handler();
        mShowWaiting = () -> {
            mProgress = new ProgressDialog(context);
            mProgress.setMessage(context.getString(R.string.waiting_detail_show));

            if (mShowWaitingDialog) {
                try {
                    mProgress.show();
                } catch (WindowManager.BadTokenException ignored) {
                }
            }
        };

        int layout = App.get().prefs().getBooleanValueWithDefaultKey(R.string.PREF_DETAIL_ALLOW_SWIPE, R.bool.pref_detail_allow_swipe) ? R.layout.dialog_detail_swipe : R.layout.dialog_detail;

        mLayout = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(layout, parent instanceof ViewGroup ? (ViewGroup) parent : null, false);
        mHandler.postDelayed(mShowWaiting, 700);
    }

    @Override
    @SuppressLint("Range")
    protected Boolean doInBackground(Void... params) {
        Boolean result = Boolean.FALSE;

        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);

        final Cursor c = context.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, id), null, null, null, TvBrowserContentProvider.KEY_ID + " ASC LIMIT 1");

        try {
            if (IOUtils.prepareAccessFirst(c)) {
                mHasSpannableActors = false;
                result = Boolean.TRUE;
                final PrefUtils prefs = App.get().prefs();
                float textScale = Float.parseFloat(prefs.getStringValueWithDefaultKey(R.string.DETAIL_TEXT_SCALE, R.string.detail_text_scale_default));

                final long startTime = c.getLong(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_STARTTIME));
                final long endTime = c.getLong(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ENDTIME));
                final ImageButton handleReminder = mLayout.findViewById(R.id.detail_handle_reminder);

                if (startTime <= System.currentTimeMillis()) {
                    handleReminder.setVisibility(View.GONE);
                } else {
                    final AtomicBoolean userRemind = new AtomicBoolean(c.getInt(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER)) == 1);
                    final AtomicBoolean favoriteRemind = new AtomicBoolean(c.getInt(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER)) == 1);

                    if (userRemind.get() || favoriteRemind.get()) {
                        handleReminder.setImageResource(R.drawable.ic_action_remove_alarm);
                        CompatUtils.setBackground(handleReminder, ContextCompat.getDrawable(context, R.drawable.button_selector_remove));
                    } else {
                        handleReminder.setImageResource(R.drawable.ic_action_add_alarm);
                        CompatUtils.setBackground(handleReminder, ContextCompat.getDrawable(context, R.drawable.button_selector_add));
                    }

                    handleReminder.setOnClickListener(v -> {
                        ContentValues values = new ContentValues();
                        boolean add = true;

                        if (userRemind.get() || favoriteRemind.get()) {
                            add = false;
                            values.put(TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER, false);
                            values.put(TvBrowserContentProvider.DATA_KEY_MARKING_FAVORITE_REMINDER, false);

                        } else {
                            values.put(TvBrowserContentProvider.DATA_KEY_MARKING_REMINDER, true);
                        }

                        if (context.getContentResolver().update(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_DATA, id), values, null, null) == 1) {
                            if (add) {
                                userRemind.set(true);

                                ServiceUpdateRemindersAndAutoUpdate.startReminderUpdate(context);
                                ProgramUtils.addReminderId(id);

                                mHandler.post(() -> {
                                    handleReminder.setImageResource(R.drawable.ic_action_remove_alarm);
                                    CompatUtils.setBackground(handleReminder, ContextCompat.getDrawable(context, R.drawable.button_selector_remove));
                                    handleReminder.invalidate();
                                });
                            } else {
                                userRemind.set(false);
                                favoriteRemind.set(false);

                                IOUtils.removeReminder(context, id);
                                ProgramUtils.removeReminderId(id);
                                ServiceUpdateRemindersAndAutoUpdate.startReminderUpdate(context);

                                mHandler.post(() -> {
                                    handleReminder.setImageResource(R.drawable.ic_action_add_alarm);
                                    CompatUtils.setBackground(handleReminder, ContextCompat.getDrawable(context, R.drawable.button_selector_add));
                                    handleReminder.invalidate();
                                });
                            }

                            Intent intent = new Intent(SettingConstants.MARKINGS_CHANGED);
                            intent.putExtra(SettingConstants.EXTRA_MARKINGS_ID, id);

                            LocalBroadcastManager.getInstance(context).sendBroadcastSync(intent);

                            UiUtils.updateImportantProgramsWidget(context);
                            UiUtils.updateRunningProgramsWidget(context);
                        }
                    });
                }

                final int favoriteMatchHighlightColor = prefs.getValue(R.string.PREF_DETAIL_HIGHLIGHT_COLOR,
                        ContextCompat.getColor(context, R.color.pref_detail_highlight_color_default));

                final Favorite[] markedFromFavorites = Favorite.getFavoritesForUniqueId(context, id);
                final ArrayList<FavoriteTypePattern> patternList = new ArrayList<>();
                final BackgroundColorSpan backgroundColorSpan = new BackgroundColorSpan(favoriteMatchHighlightColor);

                if (markedFromFavorites.length > 0) {
                    for (Favorite favorite : markedFromFavorites) {
                        if (favorite.getType() != Favorite.RESTRICTION_RULES_TYPE) {
                            patternList.add(new FavoriteTypePattern(favorite.getType(), Pattern.compile(".*?(" + favorite.getSearchValue().replace("%", ".*?") + ").*?", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)));
                        }
                    }
                }

                TextView date = mLayout.findViewById(R.id.detail_date_channel);
                TextView title = mLayout.findViewById(R.id.detail_title);
                TextView genre = mLayout.findViewById(R.id.detail_genre);
                TextView info = mLayout.findViewById(R.id.detail_info);
                TextView episode = mLayout.findViewById(R.id.detail_episode_title);
                TextView shortDescription = mLayout.findViewById(R.id.detail_short_description);
                TextView description = mLayout.findViewById(R.id.detail_description);
                TextView link = mLayout.findViewById(R.id.detail_link);
                TextView pictureDescription = mLayout.findViewById(R.id.detail_picture_description);
                TextView pictureCopyright = mLayout.findViewById(R.id.detail_picture_copyright);

                date.setTextSize(TypedValue.COMPLEX_UNIT_PX, date.getTextSize() * textScale);
                title.setTextSize(TypedValue.COMPLEX_UNIT_PX, date.getTextSize() * textScale);
                genre.setTextSize(TypedValue.COMPLEX_UNIT_PX, genre.getTextSize() * textScale);
                info.setTextSize(TypedValue.COMPLEX_UNIT_PX, info.getTextSize() * textScale);
                episode.setTextSize(TypedValue.COMPLEX_UNIT_PX, episode.getTextSize() * textScale);
                shortDescription.setTextSize(TypedValue.COMPLEX_UNIT_PX, shortDescription.getTextSize() * textScale);
                description.setTextSize(TypedValue.COMPLEX_UNIT_PX, description.getTextSize() * textScale);
                link.setTextSize(TypedValue.COMPLEX_UNIT_PX, link.getTextSize() * textScale);
                pictureDescription.setTextSize(TypedValue.COMPLEX_UNIT_PX, pictureDescription.getTextSize() * textScale);
                pictureCopyright.setTextSize(TypedValue.COMPLEX_UNIT_PX, pictureCopyright.getTextSize() * textScale);

                final Resources resources = context.getResources();

                if (!prefs.isDarkTheme() && !(finish.get() instanceof InfoActivity)) {
                    date.setTextColor(ContextCompat.getColor(context, R.color.detail_date_channel_color_light));
                }

                final int channelID = c.getInt(c.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID));

                Cursor channel = null;

                try {
                    channel = context.getContentResolver().query(ContentUris.withAppendedId(TvBrowserContentProvider.CONTENT_URI_CHANNELS, channelID), new String[]{TvBrowserContentProvider.CHANNEL_KEY_NAME, TvBrowserContentProvider.CHANNEL_KEY_LOGO, TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER}, null, null, null);

                    if (IOUtils.prepareAccessFirst(channel)) {
                        final StringBuilder channelName = new StringBuilder();
                        if (prefs.getBooleanValueWithDefaultKey(R.string.SHOW_SORT_NUMBER_IN_DETAILS, R.bool.show_sort_number_in_details_default)) {
                            final int columnIndex = channel.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_ORDER_NUMBER);
                            if (!channel.isNull(columnIndex)) {
                                channelName.append(channel.getString(columnIndex)).append(". ");
                            }
                        }
                        channelName.append(channel.getString(channel.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_NAME)));

                        final Date start = new Date(startTime);
                        final java.text.DateFormat dateFormat = java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT, Locale.getDefault());
                        final java.text.DateFormat timeFormat = DateFormat.getTimeFormat(context);
                        date.setText(resources.getString(R.string.detail_date_format, start, dateFormat.format(start), timeFormat.format(start), timeFormat.format(new Date(endTime)), channelName));

                        Bitmap logo = UiUtils.createBitmapFromByteArray(channel.getBlob(channel.getColumnIndex(TvBrowserContentProvider.CHANNEL_KEY_LOGO)));

                        if (logo != null) {
                            float scale = resources.getDisplayMetrics().density;

                            int width = (int) (logo.getWidth() * scale);
                            int height = (int) (logo.getHeight() * scale);

                            BitmapDrawable l = new BitmapDrawable(resources, logo);

                            int color = prefs.getValue(R.string.PREF_LOGO_BACKGROUND_COLOR, ContextCompat.getColor(context, R.color.pref_logo_background_color_default));

                            GradientDrawable background = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{color, color});

                            int add = 2;

                            if (prefs.getBooleanValueWithDefaultKey(R.string.PREF_LOGO_BORDER, R.bool.pref_logo_border_default)) {
                                add = 3;
                                background.setStroke(1, prefs.getValue(R.string.PREF_LOGO_BORDER_COLOR, ContextCompat.getColor(context, R.color.pref_logo_border_color_default)));
                            }

                            background.setBounds(0, 0, width + add, height + add);

                            LayerDrawable logoDrawable = new LayerDrawable(new Drawable[]{background, l});
                            logoDrawable.setBounds(0, 0, width + add, height + add);

                            l.setBounds(2, 2, width, height);

                            date.setCompoundDrawables(logoDrawable, null, null, null);
                        }
                    }
                } finally {
                    IOUtils.close(channel);
                }

                final StringBuilder year = new StringBuilder();

                int yearInt = -1;
                int yearLast = -1;
                int yearFirst = -1;
                int originLength = 0;

                if (!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ORIGIN))) {
                    year.append(c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_ORIGIN)));
                    originLength = year.length();
                }

                if (!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_YEAR))) {
                    yearInt = c.getInt(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_YEAR));
                }

                if (!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_YEAR_PRODUCTION_FIRST))) {
                    yearFirst = c.getInt(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_YEAR_PRODUCTION_FIRST));
                }

                if (!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_LAST_PRODUCTION_YEAR))) {
                    yearLast = c.getInt(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_LAST_PRODUCTION_YEAR));
                }

                if (yearLast < yearInt) {
                    yearLast = -1;
                }

                if (yearInt != -1 && ((yearLast != -1 && yearFirst == -1) || (yearLast == -1 && yearFirst < yearInt && yearFirst != -1))) {
                    yearFirst = yearInt;
                }

                if (yearInt != -1) {
                    if (year.length() >= 0) {
                        year.append(" ");
                    }
                    year.append(yearInt);
                }

                if (yearLast != -1 && yearLast != yearInt) {
                    if (yearFirst != -1) {
                        if (year.length() >= 0) {
                            if (yearFirst == yearInt) {
                                year.append(" \u2013 ");
                            } else {
                                year.append(" (");
                            }
                        }

                        if (yearFirst == yearInt) {
                            year.append(yearLast);
                        } else {
                            year.append(yearFirst).append(" \u2013 ").append(yearLast).append(")");
                        }
                    } else {
                        year.append(yearLast);
                    }
                } else if (yearFirst != -1 && yearFirst != yearInt) {
                    if (yearFirst < yearInt) {
                        year.insert(originLength, " \u2013 ");
                        year.insert(originLength, yearFirst);

                        if (originLength != 0) {
                            year.insert(originLength, " ");
                        }
                    } else if (yearInt == -1) {
                        if (originLength > 0) {
                            year.append(" ");
                        }

                        year.append(yearFirst);
                    }
                }

                String originalTitle = null;
                String titleTest = c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE));

                if (!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE_ORIGINAL))) {
                    originalTitle = c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_TITLE_ORIGINAL));
                }

                if (originalTitle == null || originalTitle.equals(titleTest)) {
                    checkAndAddHiglightingForFavorites(title, titleTest, patternList, true, backgroundColorSpan);
                    //title.setText(titleTest);
                } else {
                    checkAndAddHiglightingForFavorites(title, titleTest + "/" + originalTitle, patternList, true, backgroundColorSpan);
                    //title.setText(titleTest + "/" + originalTitle);
                }

                if (!prefs.getBooleanValueWithDefaultKey(R.string.SHOW_PICTURE_IN_DETAILS, R.bool.show_picture_in_details_default) || c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE)) || c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT))) {
                    pictureCopyright.setVisibility(View.GONE);
                    pictureDescription.setVisibility(View.GONE);
                } else {
                    if (!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE_DESCRIPTION))) {
                        pictureDescription.setText(c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE_DESCRIPTION)));
                    }

                    pictureCopyright.setText(c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE_COPYRIGHT)));

                    Bitmap image = UiUtils.createBitmapFromByteArray(c.getBlob(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_PICTURE)));

                    if (image != null) {
                        BitmapDrawable b = new BitmapDrawable(resources, image);

                        float zoom = Float.parseFloat(prefs.getStringValueWithDefaultKey(R.string.DETAIL_PICTURE_ZOOM, R.string.detail_picture_zoom_default)) * resources.getDisplayMetrics().density;

                        b.setBounds(0, 0, (int) (image.getWidth() * zoom), (int) (image.getHeight() * zoom));

                        if (prefs.getStringValueWithDefaultKey(R.string.DETAIL_PICTURE_DESCRIPTION_POSITION, R.string.detail_picture_description_position_default).equals("0")) {
                            pictureDescription.setCompoundDrawables(b, null, null, null);
                        } else {
                            pictureDescription.setCompoundDrawables(null, b, null, null);
                        }
                    }
                }

                if (prefs.getBooleanValueWithDefaultKey(R.string.SHOW_GENRE_IN_DETAILS, R.bool.show_genre_in_details_default) && !c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_GENRE))) {
                    checkAndAddHiglightingForFavorites(genre, c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_GENRE)) + (year.length() > 0 ? " - " + year : ""), patternList, false, backgroundColorSpan);
//                    genre.setText();
                } else if (year.length() > 0) {
                    checkAndAddHiglightingForFavorites(genre, year.toString(), patternList, false, backgroundColorSpan);
                    //genre.setText(year);
                } else {
                    genre.setVisibility(View.GONE);
                }

                Spannable infoValue = IOUtils.getInfoString(c.getInt(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_CATEGORIES)), resources);

                if (prefs.getBooleanValueWithDefaultKey(R.string.SHOW_INFO_IN_DETAILS, R.bool.show_info_in_details_default) && infoValue != null) {
                    info.setText(infoValue);
                } else {
                    info.setVisibility(View.GONE);
                }

                String number = "";

                if (!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_NUMBER))) {
                    if (!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_COUNT))) {
                        number = IOUtils.decodeSingleFieldValueToMultipleEpisodeString(c.getInt(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_NUMBER))) + "/" + c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_COUNT)) + " ";
                    } else {
                        number = IOUtils.decodeSingleFieldValueToMultipleEpisodeString(c.getInt(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_NUMBER))) + " ";
                    }
                }

                String originalEpisode = null;

                if (!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE_ORIGINAL))) {
                    originalEpisode = c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE_ORIGINAL));
                }

                if (prefs.getBooleanValueWithDefaultKey(R.string.SHOW_EPISODE_IN_DETAILS, R.bool.show_episode_in_details_default) && !c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE))) {
                    String episodeTest = c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_EPISODE_TITLE));

                    if (originalEpisode == null || episodeTest.equals(originalEpisode)) {
                        checkAndAddHiglightingForFavorites(episode, number + episodeTest, patternList, false, backgroundColorSpan);
                        //episode.setText(number + episodeTest);
                    } else {
                        checkAndAddHiglightingForFavorites(episode, number + episodeTest + "/" + originalEpisode, patternList, false, backgroundColorSpan);
                        //episode.setText(number + episodeTest + "/" + originalEpisode);
                    }
                } else if (prefs.getBooleanValueWithDefaultKey(R.string.SHOW_EPISODE_IN_DETAILS, R.bool.show_episode_in_details_default) && number.trim().length() > 0) {
                    checkAndAddHiglightingForFavorites(episode, number, patternList, false, backgroundColorSpan);
                    //episode.setText(number);
                } else {
                    episode.setVisibility(View.GONE);
                }

                String shortDescriptionValue = null;
                String descriptionValue = null;
                boolean showShortDescription = true;

                boolean showEpgPaidInfo = prefs.getBooleanValueWithDefaultKey(R.string.PREF_EPGPAID_DESCRIPTION_MISSING_INFO, R.bool.pref_epgpaid_description_missing_default);
                long epgPaidUntil = prefs.getLongValueWithDefaultKey(R.string.PREF_EPGPAID_ACCESS_UNTIL, R.integer.pref_epgpaid_access_until_default);

                final boolean wasActive = epgPaidUntil < System.currentTimeMillis() && epgPaidUntil != context.getResources().getInteger(R.integer.pref_epgpaid_access_until_default);

                if (showEpgPaidInfo) {
                    final Set<String> channelIds = prefs.getValue(R.string.PREF_EPGPAID_DATABASE_CHANNEL_IDS, new HashSet<>());

                    showEpgPaidInfo = channelIds.contains(String.valueOf(channelID));

                    if (showEpgPaidInfo && !wasActive) {
                        final File epgPaidDir = new File(IOUtils.getDownloadDirectory(context), "epgPaidData");

                        if (epgPaidDir.isDirectory()) {
                            final File[] test = epgPaidDir.listFiles();

                            showEpgPaidInfo = (test.length <= 2);
                        }
                    }
                }

                if (!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION))) {
                    shortDescriptionValue = c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_SHORT_DESCRIPTION)).trim();
                }

                if (!c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_DESCRIPTION))) {
                    descriptionValue = c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_DESCRIPTION));

                    if (shortDescriptionValue != null) {
                        String test = shortDescriptionValue;

                        if (test.endsWith("...")) {
                            test = test.substring(0, test.length() - 3);
                        } else if (test.endsWith("\u2026")) {
                            test = test.substring(0, test.length() - 1);
                        }

                        test = test.replaceAll("\\s+", " ").trim();

                        showShortDescription = !descriptionValue.replaceAll("\\s+", " ").trim().startsWith(test);

                        if (showShortDescription) {
                            shortDescriptionValue = shortDescriptionValue.trim() + "\n";
                        }

                        String[] paragraphs = descriptionValue.split("\n+");

                        if (paragraphs.length > 1) {
                            String para0 = paragraphs[0].replaceAll("\\s+", " ").trim();
                            String para1 = paragraphs[1].replaceAll("\\s+", " ").trim();

                            if (para1.startsWith(para0) || para0.startsWith(para1)) {
                                descriptionValue = descriptionValue.substring(paragraphs[0].length() + 1).trim();
                            } else {
                                int testIndex = para0.indexOf(".");

                                if (testIndex > 0) {
                                    para0 = para0.substring(testIndex + 1).trim();

                                    if (!para0.isEmpty() && para1.endsWith(para0)) {
                                        descriptionValue = descriptionValue.substring(paragraphs[0].length() + 1).trim();
                                    }
                                }
                            }
                        }
                    }
                }

                if (showEpgPaidInfo) {
                    if (shortDescriptionValue == null && descriptionValue == null) {
                        if (wasActive) {
                            descriptionValue = context.getString(R.string.EPGpaidData_info_expiredAccess).replace("{0}", java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(new Date(epgPaidUntil)));
                        } else {
                            descriptionValue = context.getString(R.string.detail_missing_description_text);
                        }
                    } else if (wasActive) {
                        if ((shortDescriptionValue != null && shortDescriptionValue.contains("omdb.org")) ||
                                (descriptionValue != null && descriptionValue.contains("omdb.org"))) {
                            descriptionValue += context.getString(R.string.EPGpaidData_info_expiredAccessExtend).replace("{0}", java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(new Date(epgPaidUntil)));
                        } else if ((shortDescriptionValue != null && shortDescriptionValue.contains("wiki.tvbrowser.org/index.php/WirSchauen")) ||
                                (descriptionValue != null && descriptionValue.contains("wiki.tvbrowser.org/index.php/WirSchauen"))) {
                            descriptionValue = context.getString(R.string.EPGpaidData_info_expiredAccess).replace("{0}", java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(new Date(epgPaidUntil)));
                        }
                    }
                }

                if (shortDescriptionValue == null || !showShortDescription) {
                    shortDescription.setVisibility(View.GONE);
                } else {
                    checkAndAddHiglightingForFavorites(shortDescription, shortDescriptionValue.replaceAll("\\s{4,}", "\n\n").replaceAll("\\r", "").replaceAll("\\n{2,}", "\n\n"), patternList, false, backgroundColorSpan);
                }

                if (descriptionValue != null) {
                    checkAndAddHiglightingForFavorites(description, descriptionValue.replaceAll("\\s{4,}", "\n\n").replaceAll("\\r", "").replaceAll("\\n{2,}", "\n\n"), patternList, false, backgroundColorSpan);
                } else {
                    description.setVisibility(View.GONE);
                }

                if (prefs.getBooleanValueWithDefaultKey(R.string.SHOW_LINK_IN_DETAILS, R.bool.show_link_in_details_default) && !c.isNull(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_WEBSITE_LINK))) {
                    String linkText = c.getString(c.getColumnIndex(TvBrowserContentProvider.DATA_KEY_WEBSITE_LINK));
                    link.setText(linkText);
                    link.setMovementMethod(LinkMovementMethod.getInstance());
                } else {
                    link.setVisibility(View.GONE);
                }

                Set<String> keys = VALUE_MAP.keySet();

                for (String key : keys) {
                    boolean enabled = pref.getBoolean("details_" + key, true);
                    TextView textView = mLayout.findViewById(VALUE_MAP.get(key));
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textView.getTextSize() * textScale);

                    if (enabled && !c.isNull(c.getColumnIndex(key))) {
                        String text = c.getString(c.getColumnIndex(key));

                        if (key.equals(TvBrowserContentProvider.DATA_KEY_ADDITIONAL_INFO) && text.equals(descriptionValue)) {
                            text = "";
                        }

                        if (text.trim().length() > 0) {
                            try {
                                String name = resources.getString((Integer) R.string.class.getField(key).get(null));

                                boolean endWith = name.endsWith(":");

                                if (key.equals(TvBrowserContentProvider.DATA_KEY_ACTORS)) {
                                    String separator = null;

                                    if (text.contains("\n")) {
                                        separator = "\n+";
                                    } else if (text.contains(",")) {
                                        separator = ",\\s*";
                                    }

                                    SpannableStringBuilder actors = new SpannableStringBuilder(name);
                                    actors.setSpan(new StyleSpan(Typeface.BOLD), 0, name.length() - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                    actors.setSpan(new UnderlineSpan(), 0, name.length() - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                                    int screenSizeHalf = UiUtils.getScreenSize(context).x / 2;

                                    if (separator != null) {
                                        String[] lines = text.split(separator);

                                        float maxLength = 0;
                                        TextPaint actorPaint = textView.getPaint();

                                        for (int i = 0; i < lines.length; i++) {
                                            if (lines[i].contains("\t\t-\t\t")) {
                                                separator = "\t\t-\t\t";
                                            } else if (lines[i].contains("(")) {
                                                separator = "\\s*\\(";
                                            } else {
                                                separator = null;
                                            }

                                            if (separator != null) {
                                                String[] parts = lines[i].split(separator);

                                                if (parts.length > 1) {
                                                    float test = actorPaint.measureText(parts[0]);

                                                    if (test > screenSizeHalf) {
                                                        String secondSeparator = null;

                                                        if (parts[0].contains("(")) {
                                                            secondSeparator = "(";
                                                        } else if (parts[0].contains(" ")) {
                                                            secondSeparator = " ";
                                                        } else if (parts[0].contains("-")) {
                                                            secondSeparator = "-";
                                                        }

                                                        if (secondSeparator != null) {
                                                            int index = parts[0].indexOf(secondSeparator);

                                                            String firstPart = parts[0].substring(0, index).trim();
                                                            String secondPart = parts[0].substring(index).trim();

                                                            float firstLength = actorPaint.measureText(firstPart);
                                                            float secondLength = actorPaint.measureText(secondPart);

                                                            if (secondLength > screenSizeHalf) {
                                                                int whiteSpaceIndex = secondPart.indexOf(" ");

                                                                if (whiteSpaceIndex >= 0) {
                                                                    String thirdPart = secondPart.substring(0, whiteSpaceIndex).trim();
                                                                    String fifthPart = secondPart.substring(whiteSpaceIndex).trim();

                                                                    secondLength = Math.max(actorPaint.measureText(thirdPart), actorPaint.measureText(fifthPart));

                                                                    if (secondLength < screenSizeHalf) {
                                                                        secondPart = thirdPart + "\n" + fifthPart;
                                                                    }
                                                                }
                                                            }

                                                            test = Math.max(firstLength, secondLength);

                                                            if (test < screenSizeHalf) {
                                                                lines[i] = lines[i].replace(parts[0], firstPart + "\n" + secondPart);
                                                            }
                                                        }
                                                    }

                                                    maxLength = Math.max(maxLength, test);
                                                }
                                            }
                                        }

                                        if (maxLength > 0 && maxLength <= screenSizeHalf) {
                                            for (int i = 0; i < lines.length; i++) {
                                                String[] parts = lines[i].split(separator);

                                                if (i > 0) {
                                                    actors.append("\n");
                                                }

                                                if (parts.length > 1) {
                                                    if (parts[1].trim().endsWith(")") || parts[1].trim().endsWith(",")) {
                                                        parts[1] = parts[1].substring(0, parts[1].length() - 1);
                                                    }

                                                    if (parts[0].contains("\n")) {
                                                        if (parts[0].indexOf("\n") != parts[0].lastIndexOf("\n")) {
                                                            parts[1] += "\n\n";
                                                        } else {
                                                            parts[1] += "\n";
                                                        }
                                                    }

                                                    actors.append(parts[1]);

                                                    actors.setSpan(new ActorColumnSpan(parts[0], (int) maxLength + 10, patternList, favoriteMatchHighlightColor), actors.length() - parts[1].length(), actors.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                                                    mHasSpannableActors = true;
                                                } else {
                                                    actors.append(lines[i]);
                                                }
                                            }
                                        } else {
                                            actors.append(text);
                                        }
                                    } else {
                                        actors.append(text);
                                    }

                                    checkAndAddHiglightingForFavorites(textView, actors, patternList, false, backgroundColorSpan);
                                    //textView.setText(actors);
                                } else {
                                    if (key.equals(TvBrowserContentProvider.DATA_KEY_VPS)) {
                                        Calendar temp = Calendar.getInstance();
                                        temp.set(Calendar.HOUR_OF_DAY, Integer.parseInt(text) / 60);
                                        temp.set(Calendar.MINUTE, Integer.parseInt(text) % 60);

                                        text = DateFormat.getTimeFormat(context).format(temp.getTime());
                                    }

                                    text = text.replace("\n", "<br>");

                                    name = "<b><u>" + name.replace("\n", "<br>") + "</u></b>" + (endWith ? " " : "");

                                    checkAndAddHiglightingForFavorites(textView, CompatUtils.fromHtml(name + text), patternList, false, backgroundColorSpan);
                                }
                            } catch (Exception e) {
                                textView.setVisibility(View.GONE);
                            }
                        } else {
                            textView.setVisibility(View.GONE);
                        }
                    } else {
                        textView.setVisibility(View.GONE);
                    }
                }

                final String[] projection = {TvBrowserContentProvider.KEY_ID};

                Cursor prev = null;
                try {
                    prev = context.getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, projection, TvBrowserContentProvider.DATA_KEY_STARTTIME + "<" + startTime + " AND " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " IS " + channelID + " AND NOT " + TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE, null, TvBrowserContentProvider.DATA_KEY_STARTTIME + " DESC LIMIT 1");
                    if (IOUtils.prepareAccessFirst(prev)) {
                        mPreviousId = prev.getLong(prev.getColumnIndex(TvBrowserContentProvider.KEY_ID));
                    }
                } finally {
                    IOUtils.close(prev);
                }

                Cursor next = null;
                try {
                    next = context.getContentResolver().query(TvBrowserContentProvider.CONTENT_URI_DATA, projection, TvBrowserContentProvider.DATA_KEY_STARTTIME + ">" + startTime + " AND " + TvBrowserContentProvider.CHANNEL_KEY_CHANNEL_ID + " IS " + channelID + " AND NOT " + TvBrowserContentProvider.DATA_KEY_DONT_WANT_TO_SEE, null, TvBrowserContentProvider.DATA_KEY_STARTTIME + " ASC LIMIT 1");
                    if (IOUtils.prepareAccessFirst(next)) {
                        mNextId = next.getLong(next.getColumnIndex(TvBrowserContentProvider.KEY_ID));
                    }
                } finally {
                    IOUtils.close(next);
                }
            }
        } finally {
            IOUtils.close(c);
        }

        return result;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onPostExecute(Boolean result) {
        mHandler.removeCallbacks(mShowWaiting);
        mShowWaitingDialog = false;

        if (mProgress != null && mProgress.isShowing()) {
            try {
                mProgress.dismiss();
            } catch (IllegalArgumentException ignored) {}
        }

        if (result) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);

            if (finish.get() != null) {
                builder.setOnCancelListener(dialog -> finish.get().finish());
            }

            builder.setView(mLayout);

            final AlertDialog test = builder.create();

            /* Stupid hack for wrong measured dialog size */
            if (mHasSpannableActors && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                final ViewTreeObserver.OnPreDrawListener preDrawListener = new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        mLayout.getViewTreeObserver().removeOnPreDrawListener(this);

                        int scrollHeight = mLayout.findViewById(R.id.test_test_test).getMeasuredHeight();

                        if (scrollHeight < mLayout.getMeasuredHeight()) {
                            final Window w = test.getWindow();

                            if (w != null) {
                                w.setLayout(mLayout.getMeasuredWidth() + 40, scrollHeight + 100);
                            }
                        }

                        return true;
                    }
                };
                mLayout.getViewTreeObserver().addOnPreDrawListener(preDrawListener);
            }

            if (mLayout instanceof SwipeScrollView) {
                mLayout.setOnTouchListener((v, event) -> {
                    boolean result1 = false;

                    if (((SwipeScrollView) mLayout).isSwipeLeft()) {
                        ((SwipeScrollView) mLayout).resetSwipe();

                        if (mNextId != -1) {
                            test.dismiss();
                            showProgramInfo(context, mNextId, finish.get(), parent, handler);
                            result1 = true;
                        }
                    } else if (((SwipeScrollView) mLayout).isSwipeRight()) {
                        ((SwipeScrollView) mLayout).resetSwipe();

                        if (mPreviousId != -1) {
                            test.dismiss();
                            showProgramInfo(context, mPreviousId, finish.get(), parent, handler);
                            result1 = true;
                        }
                    }

                    return result1;
                });
            }

            try {
                test.show();
            } catch (WindowManager.BadTokenException ignored) {
            }
        }

        new Thread("SHOW PROGRAM INFO ALLOWING THREAD") {
            @Override
            public void run() {
                try {
                    sleep(500);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                mShowingProgramInfo = false;
            }
        }.start();
    }

    private static final class ActorColumnSpan implements LeadingMarginSpan {
        private final String[] mActorParts;
        private final int mMargin;
        private int mFirstBaseline;
        private boolean mHighlight;
        private final int mHighlightColor;

        ActorColumnSpan(String actor, int leadingMargin, ArrayList<FavoriteTypePattern> patternList, int color) {
            mHighlight = false;

            for (FavoriteTypePattern pattern : patternList) {
                if (pattern.isNotOnlyTitleType() && pattern.mPattern.matcher(actor.replaceAll("\\n+", " ")).find()) {
                    mHighlight = true;
                    break;
                }
            }

            mActorParts = actor.split("\n");
            mMargin = leadingMargin;
            mFirstBaseline = -1;
            mHighlightColor = color;
        }

        @Override
        public void drawLeadingMargin(Canvas c, Paint p, int x, int dir, int top,
                                      int baseline, int bottom, CharSequence text, int start, int end,
                                      boolean first, Layout layout) {
            if (first && (mFirstBaseline == -1 || mFirstBaseline == baseline)) {
                mFirstBaseline = baseline;

                if (mHighlight) {
                    c.save();
                    c.clipRect(x + dir, top, x + dir + p.measureText(mActorParts[0]), bottom);
                    c.drawColor(mHighlightColor);
                    c.restore();
                }

                c.drawText(mActorParts[0], x + dir, baseline, p);

                if (mActorParts.length > 1) {
                    if (mHighlight) {
                        c.save();
                        c.clipRect(x + dir, 2 * bottom - top, x + dir + p.measureText(mActorParts[1]), bottom);
                        c.drawColor(mHighlightColor);
                        c.restore();
                    }

                    c.drawText(mActorParts[1], x + dir, baseline + (bottom - top), p);
                }
                if (mActorParts.length > 2) {
                    if (mHighlight) {
                        c.save();
                        c.clipRect(x + dir, 3 * bottom - top, x + dir + p.measureText(mActorParts[2]), bottom);
                        c.drawColor(mHighlightColor);
                        c.restore();
                    }

                    c.drawText(mActorParts[2], x + dir, baseline + 2 * (bottom - top), p);
                }
            }
        }

        @Override
        public int getLeadingMargin(boolean first) {
            return mMargin;
        }
    }

    static final class FavoriteTypePattern {
        private final int mType;
        private final Pattern mPattern;

        FavoriteTypePattern(int type, Pattern pattern) {
            mType = type;
            mPattern = pattern;
        }

        SpannableStringBuilder addHighlighting(SpannableStringBuilder text, BackgroundColorSpan backgroundColorSpan) {
            final Matcher m = mPattern.matcher(text);

            int pos = 0;

            while (m.find(pos)) {
                text.setSpan(backgroundColorSpan, m.start(1), m.end(1), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                pos = m.end();
            }

            return text;
        }

        boolean isNotOnlyTitleType() {
            return mType != Favorite.KEYWORD_ONLY_TITLE_TYPE;
        }
    }

    /*    public static View applyColorToHeader(String name, Context context, AttributeSet attrs, Resources.Theme theme) {
      View result = null;

      if ("android.app.FragmentBreadCrumbs".equals(name))
      {
        result = new android.app.FragmentBreadCrumbs(context, attrs);

        try {
          int[] attrs1 = new int[] { android.R.attr.textColorSecondary };
          TypedArray a = theme.obtainStyledAttributes(UiUtils.getThemeResourceId(UiUtils.TYPE_THEME_DEFAULT, App.get().prefs().isDarkTheme()), attrs1);
          int DEFAULT_TEXT_COLOR = a.getColor(0, Color.WHITE);
          a.recycle();

          final Field titleColor = result.getClass().getDeclaredField("mTextColor");
          titleColor.setAccessible(true);
          titleColor.setInt(result,DEFAULT_TEXT_COLOR);
        } catch (final Exception ignored) {
          // Nothing to do
        }
      }

      return result;
    }*/
}