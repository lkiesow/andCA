package de.larskiesow.andca;

import android.app.Activity;
import android.drm.DrmStore;
import android.os.Bundle;
import android.graphics.PorterDuff;
import android.os.Environment;
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
import android.app.ProgressDialog;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.BasicHttpContext;
import android.os.AsyncTask;


import android.content.Context;
import android.provider.MediaStore;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.text.SimpleDateFormat;
import java.util.List;
import java.io.File;
import java.util.ArrayList;
import android.net.Uri;
import android.database.Cursor;
import android.util.Log;
import android.content.CursorLoader;
import android.widget.Toast;

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
	private ProgressDialog dialog;

    private Uri fileUri;
    public static final int MEDIA_TYPE_VIDEO = 2;
	private static final int ACTION_TAKE_VIDEO  = 200;
	private static final int ACTION_SELECT_FILE = 2;
    static final int REQUEST_VIDEO_CAPTURE = 1;


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

		app.host   = settings.getString("host",     "repo.virtuos.uos.de");
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
		if (!isIntentAvailable(this, MediaStore.ACTION_VIDEO_CAPTURE)) {
			showDialog( R.string.error, R.string.cap_missing);
			return;
		}
		Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        // create a file to save the video
        fileUri = getOutputMediaFileUri(MEDIA_TYPE_VIDEO);

        // set the image file name
        takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);


        startActivityForResult(takeVideoIntent, ACTION_TAKE_VIDEO);


	}

	public void selectRecording(View view) {
		if (isIntentAvailable(this, Intent.ACTION_GET_CONTENT)) {
			Intent pickMedia = new Intent(Intent.ACTION_GET_CONTENT);
			pickMedia.setType("video/*");
			startActivityForResult(pickMedia, ACTION_SELECT_FILE);

		} else {
			if (!isIntentAvailable(this, "org.openintents.action.PICK_FILE")) {
				showDialog( R.string.error, R.string.oif_missing);
				return;
			}
			Intent intent = new Intent("org.openintents.action.PICK_FILE");
			startActivityForResult(intent, ACTION_SELECT_FILE);
		}
	}

	public void viewRecording(View view) {
		if (!isIntentAvailable(this, Intent.ACTION_VIEW)) {
			showDialog( R.string.error, R.string.play_missing);
			return;
		}
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setDataAndType(app.lastRecordingUri, "video/mp4");
		view.getContext().startActivity(intent);
	}

	public void uploadRecording(View view) {
		dialog = ProgressDialog.show(this, getString(R.string.upload_rec),
				getString(R.string.uploading), true);
		new IngestTask().execute();
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


    /** Create a file Uri for saving an image or video */
    private static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type){

        // Check that the SDCard is mounted
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES), "MatterhornVideos");


        // Create the storage directory(MyCameraVideo) if it does not exist
        if (! mediaStorageDir.exists()){

            if (! mediaStorageDir.mkdirs()){

                //output.setText("Failed to create directory MyCameraVideo.");

                Log.d("MyCameraVideo", "Failed to create directory MyCameraVideo.");
                return null;
            }
        }


        // Create a media file name

        // For unique file name appending current timeStamp with file name
        java.util.Date date= new java.util.Date();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(date.getTime());

        File mediaFile;

        if(type == MEDIA_TYPE_VIDEO) {

            // For unique video file name appending current timeStamp with file name
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
            Log.d("StorageDir",mediaFile.toString());
        } else {
            return null;
        }

        return mediaFile;
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

	public void showDialog( int title, int msg ) {
		showDialog( getString(title), getString(msg) );
	}

	public void showDialog( String title, int msg ) {
		showDialog( title, getString(msg) );
	}

	public void showDialog( int title, String msg ) {
		showDialog( getString(title), msg );
	}

	public void showDialog( String title, String msg ) {

		AlertDialog alertDialog = new AlertDialog.Builder(this).create();
		alertDialog.setTitle(title);
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
				Intent intent = new Intent(this, Settings.class);
				startActivity(intent);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}


	private class IngestTask extends AsyncTask <Void, Void, String>{
		@Override
		protected String doInBackground(Void... unsued) {

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

				response = httpClient.execute(httppost);
				BufferedReader reader = new BufferedReader( new InputStreamReader(
							response.getEntity().getContent(), "UTF-8"));

				String sResponse = reader.readLine();
				return sResponse;

			} catch (Exception e) {
				if (dialog.isShowing())
					dialog.dismiss();
				Log.e("HTTP POST", e.getLocalizedMessage(), e);
				showDialog(R.string.error, R.string.ingest_failed);
				return null;
			} finally {
				httpClient.getConnectionManager().shutdown();
			}
		}

		@Override
		protected void onProgressUpdate(Void... unsued) {}

		@Override
		protected void onPostExecute(String response) {
			if (dialog.isShowing())
				dialog.dismiss();

			if (response != null) {
				if (response.contains("<workflow ")) {
					showDialog(R.string.success, R.string.ingest_success);
				} else {
					showDialog(R.string.error, R.string.ingest_failed);
				}
			}
		}
	}

}
