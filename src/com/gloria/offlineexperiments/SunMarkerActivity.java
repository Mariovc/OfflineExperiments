package com.gloria.offlineexperiments;

import java.util.ArrayList;

import android.app.Activity;
import android.graphics.Bitmap;
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
		// ask the bitmap factory not to scale the loaded bitmaps
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inScaled = false;
		// load the bitmap
		Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.sol2, opts);
		imgTouchable.setImageBitmap(bitmap);
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
		ArrayList<ZoomablePinView> pins = imgTouchable.getPins();
		savedState.putInt("numPins", pins.size());
		for (int i = 0; i < pins.size(); i++) {
			String pinName = "pin" + i;
			savedState.putInt(pinName + "number", pins.get(i).getNumber());
			PointF pos = pins.get(i).getPositionInPixels();
			savedState.putFloat(pinName + "posX", pos.x);
			savedState.putFloat(pinName + "posY", pos.y);
		}
		savedState.putInt("selectedPin", imgTouchable.getSelectedPin());
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedState){
		super.onRestoreInstanceState(savedState);
		ArrayList<ZoomablePinView> pins = new ArrayList<ZoomablePinView>();
		int numPins = savedState.getInt("numPins");
		for (int i = 0; i < numPins; i++) {
			ZoomablePinView pin = new ZoomablePinView(this);
			pin.setNumber(savedState.getInt("pin" + i + "number"));
			pin.setImageBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.marker));
			float posX = savedState.getFloat("pin" + i + "posX");
			float posY = savedState.getFloat("pin" + i + "posY");
			pin.setRealPosition(posX, posY);
			pins.add(pin);
		}
		imgTouchable.setPins(pins);
		imgTouchable.setSelectedPin(savedState.getInt("selectedPin"));
		imgTouchable.setReload(true);
		//imgTouchable.selectPin(savedState.getInt("selectedPin"));
	}
}
