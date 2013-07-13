package de.larskiesow.andca;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.widget.EditText;
import android.util.Log;
import android.content.SharedPreferences;

public class Settings extends Activity {

	private AndCAApplication app;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set the text view as the activity layout
		setContentView(R.layout.options);

		app = (AndCAApplication) this.getApplication();

		//((EditText) findViewById(R.id.hostedit)).setEnabled(true);
		if (app.host != null) {
			((EditText) findViewById(R.id.hostedit)).setText( app.host );
		}
		if (app.port != null) {
			((EditText) findViewById(R.id.portedit)).setText( app.port );
		}
		if (app.user != null) {
			((EditText) findViewById(R.id.useredit)).setText( app.user );
		}
		if (app.passwd != null) {
			((EditText) findViewById(R.id.passwdedit)).setText( app.passwd );
		}
	}

	public void saveSettings(View view) {

		app.host = ((EditText) findViewById(R.id.hostedit)).getText().toString();
		app.port = ((EditText) findViewById(R.id.portedit)).getText().toString();
		app.user = ((EditText) findViewById(R.id.useredit)).getText().toString();
		app.passwd = ((EditText) findViewById(R.id.passwdedit)).getText().toString();

      SharedPreferences settings = getSharedPreferences(app.PREFS_NAME, 0);
      SharedPreferences.Editor editor = settings.edit();
      editor.putString("host",     app.host);
      editor.putString("port",     app.port);
      editor.putString("user",     app.user);
		editor.putString("password", app.passwd);
      editor.commit();

		finish();

	}

}
