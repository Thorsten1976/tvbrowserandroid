package org.tvbrowser.tvbrowser;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;

import org.tvbrowser.devplugin.PluginDefinition;

final class PluginUpdateHelperImpl extends PluginUpdateHelper {

	PluginUpdateHelperImpl(@NonNull final TvBrowser tvBrowser) {
		super(tvBrowser);
	}

	@Override
	void prepareLinks(@NonNull final StringBuilder pluginsText, @NonNull final PluginDefinition pluginDefinition) {
		if (pluginDefinition.isOnGooglePlay()) {
			pluginsText.append("<p><a href=\"http://play.google.com/store/apps/details?id=");
			pluginsText.append(pluginDefinition.getPackageName());
			pluginsText.append("\">").append(tvBrowser.getString(R.string.plugin_open_google_play)).append("</a></p>");
		}
	}

	@Override
	void loadPlugin(@NonNull final String url) {
		if (url.startsWith("http://play.google.com/store/apps/details?id=")) {
			try {
				tvBrowser.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url.replace("http://play.google.com/store/apps", "market:/"))));
			} catch (ActivityNotFoundException anfe) {
				tvBrowser.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
			}
		}
		mLoadingPlugin = false;
	}

	@Override
	void cleanup() {
	}

	@Override
	boolean pluginSupported(@NonNull final PluginDefinition pluginDefinition) {
		return pluginDefinition.isOnGooglePlay();
	}

	@NonNull
	@Override
	String url() {
		return "download/android-plugins-play.gz";
	}
}