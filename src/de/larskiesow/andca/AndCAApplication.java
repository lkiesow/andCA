package de.larskiesow.andca;

import android.app.Application;
import android.net.Uri;

public class AndCAApplication extends Application {

	public static final String PREFS_NAME = "AndCAPrefs";

	public String host;
	public String port;
	public String user;
	public String passwd;
	public String lastRecordingPath;
	public Uri lastRecordingUri;

}
