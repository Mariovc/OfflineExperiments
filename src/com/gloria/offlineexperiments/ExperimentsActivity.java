package com.gloria.offlineexperiments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;


public class ExperimentsActivity extends Activity{
	/** Called when the activity is first created. */
	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.experiments);

	}

	public void launchSunMarker(View view) {
		Intent i = new Intent(this, SunMarkerActivity.class);
		startActivity(i);
	}
}
