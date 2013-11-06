package com.gloria.offlineexperiments;

import com.gloria.offlineexperiments.ui.fonts.TypefaceManager;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;

public class InstructionsActivity extends Activity {
	private String username;
	private String sha1Password;
	private String authorizationToken;
	
	/** Called when the activity is first created. */
	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle extras = getIntent().getExtras();
		username = extras.getString("username");
		sha1Password = extras.getString("password");
		authorizationToken = extras.getString("authorizationToken");

		setContentView(R.layout.instructions);
		prepareWidgets();
		
	}

	public void showUsageImage(View view) {
		findViewById(R.id.imageButton).setVisibility(View.GONE);
		findViewById(R.id.textLayout).setVisibility(View.GONE);
		findViewById(R.id.usageImageView).setVisibility(View.VISIBLE);
	}

	public void launchSunMarker(View view) {
		Intent intent = new Intent(this, SunMarkerActivity.class);
		intent.putExtra("username", username);
		intent.putExtra("password", sha1Password);
		intent.putExtra("authorizationToken", authorizationToken);
		startActivity(intent);
	}
	
	private void prepareWidgets() {
		final Typeface typeface = TypefaceManager.INSTANCE.getTypeface(
				getApplicationContext(), TypefaceManager.VERDANA);
		TypefaceManager.INSTANCE.applyTypefaceToAllViews(this, typeface);
		findViewById(R.id.usageImageView).setVisibility(View.GONE);
	}	
	
}
