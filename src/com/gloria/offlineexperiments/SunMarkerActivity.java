package com.gloria.offlineexperiments;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class SunMarkerActivity extends Activity{

	private SunMarkerView imgTouchable;


	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sun_marker);

		imgTouchable = (SunMarkerView) findViewById(R.id.touchImageView1);
		imgTouchable.setImageBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.sol2));
		imgTouchable.setMaxZoom(4f); //change the max level of zoom, default is 3f
		imgTouchable.setWolfNumberText((TextView) findViewById(R.id.wolfNumberText));
	}

	public void showPinPosition(View view) {
		ZoomablePinView pin = imgTouchable.getPin();
		if (pin != null) {
			PointF pinPos = pin.getPositionInPixels();
			Toast.makeText(this, "pin position: " + pinPos.x + ", " + pinPos.y, Toast.LENGTH_SHORT).show();
		}
		else {
			Toast.makeText(this, "no pin selected", Toast.LENGTH_SHORT).show();
		}
	}
	
	public void removePin (View view) {
		imgTouchable.removePin();
	}

	public void increase (View view) {
		imgTouchable.increasePin();
	}
	
	public void decrease (View view) {
		imgTouchable.decreasePin();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle savedState){
		super.onSaveInstanceState(savedState);
		/*if (mp != null) {
			int pos = mp.getCurrentPosition();
			estadoGuardado.putInt("posicion", pos);
		}*/
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedState){
		super.onRestoreInstanceState(savedState);
		/*if (estadoGuardado != null && mp != null) {
			int pos = estadoGuardado.getInt("posicion");
			mp.seekTo(pos);
		}*/
	}
}
