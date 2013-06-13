package com.gloria.offlineexperiments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;


public class ExperimentsActivity extends Activity{

	private String username;
	private String sha1Password;

	/** Called when the activity is first created. */
	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.experiments);

		Bundle extras = getIntent().getExtras();
		username = extras.getString("username");
		sha1Password = extras.getString("password");
	}

	public void launchSunMarker(View view) {
		Intent intent = new Intent(this, SunMarkerActivity.class);
		intent.putExtra("username", username);
		intent.putExtra("password", sha1Password);
		startActivity(intent);
	}
}
