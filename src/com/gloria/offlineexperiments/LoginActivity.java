package com.gloria.offlineexperiments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;

public class LoginActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final boolean customTitleSupported = requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.activity_login);

		if ( customTitleSupported ) {
			getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.titlebar);
		}

		ImageView infoButton = (ImageView)findViewById(R.id.infoButton);
		infoButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				exit(null);
			}
		}); 
		/*final TextView myTitleText = (TextView) findViewById(R.id.myTitle);
        if ( myTitleText != null ) {
            myTitleText.setText("NEW TITLE");

            // user can also set color using "Color" and then "Color value constant"
           // myTitleText.setBackgroundColor(Color.GREEN);
        }*/
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.login, menu);
		return true;
	}

	public void launchInfo() {
		//Intent i = new Intent(this, Info.class);
		//startActivity(i);
	}
	
	public void launchExercise(View view) {
		Intent i = new Intent(this, ExerciseActivity.class);
		startActivity(i);
	}
	
	public void exit(View view) {
		finish();
	}
}
