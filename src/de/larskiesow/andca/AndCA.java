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

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		/* Restore preferences */
		app = ((AndCAApplication) this.getApplication());
		SharedPreferences settings = getSharedPreferences(app.PREFS_NAME, 0);
       
		app.host   = settings.getString("host",     "131.173.168.36");
		app.port   = settings.getString("port",     "8080");
		app.user   = settings.getString("user",     "admin");
		app.passwd = settings.getString("password", "opencast");

	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	public void startRecording(View view) {
		dispatchTakeVideoIntent();
	}

	public void viewRecording(View view) {
		Intent intent = new Intent(Intent.ACTION_VIEW); 
		intent.setDataAndType(app.lastRecordingUri, "video/mp4"); 
		view.getContext().startActivity(intent); 
	}

	public void uploadRecording(View view) {
		ingest();
	}

	private static final int ACTION_TAKE_VIDEO = 3;

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

	/*
	private void handleCameraVideo(Intent intent) {
		mVideoUri = intent.getData();
		mVideoView.setVideoURI(mVideoUri);
	}
	*/
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (resultCode == RESULT_OK) {
			if (requestCode == ACTION_TAKE_VIDEO) {
				app.lastRecordingUri = data.getData();
				app.lastRecordingPath = getPath(app.lastRecordingUri);
				Log.d("LOGCAT", "Video path is: " + app.lastRecordingPath);
			}
		}
	}

	public String getPath(Uri uri) {
		String[] projection = { MediaStore.Images.Media.DATA };
		Cursor cursor = managedQuery(uri, projection, null, null, null);
		if (cursor == null) return null;
		int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		cursor.moveToFirst();
		String s=cursor.getString(column_index);
		return s;
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

		try {
			/* First login to the MH core server */
			HttpPost httppost = new HttpPost("http://" + app.host + ":" + app.port + "/j_spring_security_check");

			/*
			List <NameValuePair> nvps = new ArrayList <NameValuePair>();
			nvps.add(new BasicNameValuePair("j_username", "admin"));
			nvps.add(new BasicNameValuePair("j_password", "o pencast"));

			httppost.setEntity(new UrlEncodedFormEntity(nvps, Consts.UTF_8));
			*/
			List <NameValuePair> nvps = new ArrayList <NameValuePair>();
			nvps.add(new BasicNameValuePair("j_username", app.user));
			nvps.add(new BasicNameValuePair("j_password", app.passwd));

			httppost.setEntity(new UrlEncodedFormEntity(nvps));
			/*
			MultipartEntity multipartEntity =  new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);  
			multipartEntity.addPart("j_username",  new StringBody(app.user));
			multipartEntity.addPart("j_password",  new StringBody(app.passwd));
			httppost.setEntity(multipartEntity);
			*/

			HttpResponse response = httpClient.execute(httppost);
			HttpEntity entity = response.getEntity();

			/*
			if (!response.getStatusLine().toString().contains("302 Found")) {
				Log.e("MH Login", "Login failed: Response was: " + response.getStatusLine());
				return;
			}
			*/
			/*
			EntityUtils.consume(entity);

			System.out.println("Post logon cookies:");
			List<Cookie> cookies = httpclient.getCookieStore().getCookies();
			if (cookies.isEmpty()) {
				System.out.println("None");
				Log.e("HTTP POST", e.getLocalizedMessage(), e);
			} else {
				for (int i = 0; i < cookies.size(); i++) {
					System.out.println("- " + cookies.get(i).toString());
				}
			}
			*/
			/*
			String location = response.getFirstHeader("location").getValue();
			if ( location == null || location.contains("login.html") ) {
				// Login was incorrect: Abort!
				Log.e("MH Login", "Login failed: Incorrect username");
				return;
			}
			*/

			httppost = new HttpPost("http://" + app.host + ":" + app.port + "/ingest/addMediaPackage");

			MultipartEntity multipartEntity =  new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);  
			multipartEntity.addPart("flavor",  new StringBody("presenter/source"));
			multipartEntity.addPart("title",   new StringBody("test"));
			multipartEntity.addPart("creator", new StringBody("Lars Kiesow"));
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
