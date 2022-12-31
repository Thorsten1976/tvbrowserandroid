/*
 * TV-Browser for Android
 * Copyright (C) 2018 René Mach (rene@tvbrowser.org)
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
package org.tvbrowser.widgets;

import static androidx.activity.result.contract.ActivityResultContracts.*;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import org.tvbrowser.tvbrowser.InfoActivity;
import org.tvbrowser.tvbrowser.R;
import org.tvbrowser.utils.CompatUtils;

public class ActivityConfigurationWidgetRunning extends AppCompatActivity {

  private final ActivityResultLauncher<Intent> activityResultLauncher =
    registerForActivityResult(new StartActivityForResult(), result -> finish());

  private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_configuration_widget_running);

    if (getIntent().hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
      mAppWidgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

      if (CompatUtils.showWidgetRefreshInfo()) {
        final Intent info = new Intent(getApplicationContext(), InfoActivity.class)
          .setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
          .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activityResultLauncher.launch(info);
      }
      else {
        finish();
      }
    }
    else {
      setResult(RESULT_OK);
      finish();
    }
  }

  @Override
  public void finish() {
    if (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
      Intent result = new Intent();
      result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
      setResult(RESULT_OK, result);
    }
    else {
      setResult(RESULT_OK);
    }
    super.finish();
  }
}