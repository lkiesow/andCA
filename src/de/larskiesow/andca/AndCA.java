package de.larskiesow.andca;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.PorterDuff;
import android.view.View;
import android.widget.Button;
import android.widget.TableRow;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.BufferedInputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.io.IOException;
import android.view.MenuInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.content.DialogInterface;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.widget.EditText;
import android.text.TextWatcher;
import android.text.Editable;
import android.view.WindowManager;
import android.widget.TextView;

import android.content.Context;
import android.provider.MediaStore;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import java.util.List;
import java.io.File;
import java.util.ArrayList;
import android.net.Uri;
import android.database.Cursor;
import android.util.Log;
import android.content.CursorLoader;

import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.HttpResponse;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.HttpVersion;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.params.HttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.message.BasicHeader;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;


public class AndCA extends Activity
{
	private AndCAApplication app;

	private static final int ACTION_TAKE_VIDEO  = 3;
	private static final int ACTION_SELECT_FILE = 2;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		activateButtons();

		/* Restore preferences */
		app = ((AndCAApplication) this.getApplication());
		SharedPreferences settings = getSharedPreferences(app.PREFS_NAME, 0);
       
		app.host   = settings.getString("host",     "131.173.168.36");
		app.port   = settings.getString("port",     "8080");
		app.user   = settings.getString("user",     "admin");
		app.passwd = settings.getString("password", "opencast");

		/* Set listener for edit fields */
		TextWatcher t = new TextWatcher(){
			public void afterTextChanged(Editable s) {activateButtons();}
			public void beforeTextChanged(CharSequence s, int start, int count, int after){}
			public void onTextChanged(CharSequence s, int start, int before, int count){}
		};
		((EditText) findViewById(R.id.set_title)).addTextChangedListener(t);
		((EditText) findViewById(R.id.set_creator)).addTextChangedListener(t);

		/* Take focus from disaled input */
		this.getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

	}


	private void activateButtons() {

		app = ((AndCAApplication) this.getApplication());
		Boolean step2 = app.lastRecordingPath != null;

		String title   = ((EditText) findViewById(R.id.set_title)).getText().toString();
		String creator = ((EditText) findViewById(R.id.set_creator)).getText().toString();
		Boolean step3 = step2 && !"".equals(title) && !"".equals(creator);
		/* Enable/Disable buttons */
		findViewById(R.id.viewbutton).setEnabled(step2);
		findViewById(R.id.set_title).setEnabled(step2);
		findViewById(R.id.set_creator).setEnabled(step2);
		findViewById(R.id.uploadbutton).setEnabled(step3);


	}


	@Override
	protected void onStop() {
		super.onStop();
	}

	public void startRecording(View view) {
		dispatchTakeVideoIntent();
	}

	public void selectRecording(View view) {
		Intent intent = new Intent("org.openintents.action.PICK_FILE");
		startActivityForResult(intent, ACTION_SELECT_FILE);
	}

	public void viewRecording(View view) {
		Intent intent = new Intent(Intent.ACTION_VIEW); 
		intent.setDataAndType(app.lastRecordingUri, "video/mp4"); 
		view.getContext().startActivity(intent); 
	}

	public void uploadRecording(View view) {
		ingest();
	}

	private void dispatchTakeVideoIntent() {
		Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		startActivityForResult(takeVideoIntent, ACTION_TAKE_VIDEO);
	}

	public static boolean isIntentAvailable(Context context, String action) {
		final PackageManager packageManager = context.getPackageManager();
		final Intent intent = new Intent(action);
		List<ResolveInfo> list =
			packageManager.queryIntentActivities(intent,
			PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}

	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (resultCode == RESULT_OK) {
			if (requestCode == ACTION_TAKE_VIDEO || requestCode == ACTION_SELECT_FILE) {
				app.lastRecordingUri = data.getData();
				app.lastRecordingPath = getPath(app.lastRecordingUri);

				String fileName = new File(app.lastRecordingPath).getName();
				((TextView) findViewById(R.id.selected_file)).setText(fileName);
				activateButtons();
			}
		}
	}


	public String getPath(Uri uri) {
		if ( "content".equals(uri.getScheme()) ) {      
			String[] projection = { MediaStore.Images.Media.DATA };
			Cursor cursor = managedQuery(uri, projection, null, null, null);
			if (cursor != null) {
				int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
				cursor.moveToFirst();
				return cursor.getString(column_index);
			}

		} else if ( "file".equals(uri.getScheme()) ) {
			return uri.getPath();
		}
		return null;
	}

	public void showErrorDialog( int resid ) {
		showErrorDialog( getString(resid) );
	}

	public void showErrorDialog( String msg ) {
	
		AlertDialog alertDialog = new AlertDialog.Builder(this).create();
		alertDialog.setTitle(getString(R.string.error));
		alertDialog.setMessage(msg);
		alertDialog.setButton(
			DialogInterface.BUTTON_POSITIVE, 
			getString(R.string.ok), 
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
				}}); 
		alertDialog.show();
	
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mainmenu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.options:
				Log.d("LOGCAT", "##### 1 ##################################");
				Intent intent = new Intent(this, Settings.class);
				Log.d("LOGCAT", "##### 2 ##################################");
				startActivity(intent);
				Log.d("LOGCAT", "##### 3 ##################################");
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}


	public void ingest() {

		DefaultHttpClient httpClient = new DefaultHttpClient();

		String title   = ((EditText) findViewById(R.id.set_title)).getText().toString();
		String creator = ((EditText) findViewById(R.id.set_creator)).getText().toString();

		try {
			/* First login to the MH core server */
			HttpPost httppost = new HttpPost("http://" + app.host + ":" + app.port + "/j_spring_security_check");

			List <NameValuePair> nvps = new ArrayList <NameValuePair>();
			nvps.add(new BasicNameValuePair("j_username", app.user));
			nvps.add(new BasicNameValuePair("j_password", app.passwd));

			httppost.setEntity(new UrlEncodedFormEntity(nvps));

			HttpResponse response = httpClient.execute(httppost);
			HttpEntity entity = response.getEntity();

			/* Then send the data to ingest */
			httppost = new HttpPost("http://" + app.host + ":" + app.port + "/ingest/addMediaPackage");

			MultipartEntity multipartEntity =  new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);  
			multipartEntity.addPart("flavor",  new StringBody("presenter/source"));
			multipartEntity.addPart("title",   new StringBody(title));
			multipartEntity.addPart("creator", new StringBody(creator));
			multipartEntity.addPart("BODY", new FileBody(new File(app.lastRecordingPath)));
			httppost.setEntity(multipartEntity);

			httpClient.execute(httppost, new PhotoUploadResponseHandler());

		} catch (Exception e) {
			Log.e("HTTP POST", e.getLocalizedMessage(), e);
		} finally {
			httpClient.getConnectionManager().shutdown();
		}
	}

	private class PhotoUploadResponseHandler implements ResponseHandler<Object> {

		@Override
		public Object handleResponse(HttpResponse response)
			throws ClientProtocolException, IOException {

			HttpEntity r_entity = response.getEntity();
			String responseString = EntityUtils.toString(r_entity);
			Log.d("UPLOAD", responseString);

			return null;
		}

	}

}
