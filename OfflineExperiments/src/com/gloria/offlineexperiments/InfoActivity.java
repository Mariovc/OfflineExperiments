/**
 * @author Mario Velasco Casquero
 * @version 2.00
 */

package com.gloria.offlineexperiments;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;


public class InfoActivity extends Activity{
	/** Called when the activity is first created. */
	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.info);
		((TextView)findViewById(R.id.infoText)).setMovementMethod(LinkMovementMethod.getInstance());
	}
}
