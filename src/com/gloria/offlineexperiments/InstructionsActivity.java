package com.gloria.offlineexperiments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class InstructionsActivity extends Activity {
	private String username;
	private String sha1Password;
	private String authorizationToken;

	/** Called when the activity is first created. */
	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.instructions);

		Bundle extras = getIntent().getExtras();
		username = extras.getString("username");
		sha1Password = extras.getString("password");
		authorizationToken = extras.getString("authorizationToken");
	}

	public void launchSunMarker(View view) {
		Intent intent = new Intent(this, SunMarkerActivity.class);
		intent.putExtra("username", username);
		intent.putExtra("password", sha1Password);
		intent.putExtra("authorizationToken", authorizationToken);
		startActivity(intent);
	}
}
