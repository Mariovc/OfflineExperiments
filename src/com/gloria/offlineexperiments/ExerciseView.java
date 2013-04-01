package com.gloria.offlineexperiments;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class ExerciseView extends View{

	private int screenWidth;
	private int screenHeight;
	
	//private boolean clickAction = false;
	//private float lastX=0, lastY=0;
	//private final static int DISPLACEMENT = 5;
	
	private PinObject pin;
	private boolean visiblePin = false;
	
	public ExerciseView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.setBackgroundResource(R.drawable.water_drops_wallpaper);
		pin = new PinObject(context);
	}
	
	@Override 
	protected void onSizeChanged(int width, int height, int prevWidth, int prevHeight) {
		super.onSizeChanged(width, height, prevWidth, prevHeight);
		screenWidth = width;
		screenHeight = height;
	}
	
	@Override 
	synchronized protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		Toast.makeText(getContext(), "onDraw Exercise", Toast.LENGTH_SHORT/10).show();
		if (visiblePin) {
			pin.draw(canvas);
		}
	}

	/*
	@Override
	public boolean onTouchEvent (MotionEvent event) {
		super.onTouchEvent(event);
		float x = event.getX();
		float y = event.getY();
		
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			Toast.makeText(getContext(), "click down", Toast.LENGTH_SHORT/2).show();
			clickAction=true;
			break;
		case MotionEvent.ACTION_MOVE:
			float dx = Math.abs(x - lastX);
			float dy = Math.abs(y - lastY);
			Toast.makeText(getContext(), "dx:"+dx+" dy:"+dy, Toast.LENGTH_SHORT/2).show();
			if (dy>DISPLACEMENT || dx>DISPLACEMENT){
				clickAction = false;
			}
			break;
		case MotionEvent.ACTION_UP:
			if (clickAction){
				Toast.makeText(getContext(), "click", Toast.LENGTH_SHORT/2).show();
				pin.setPos(x,y);
				pin.invalidate();
				invalidate();
				visiblePin=true;
				clickAction=false;
			}
			break;
		}
		lastX=x; lastY=y;       
		return true;
	}*/
	
	@Override
	public boolean onTouchEvent (MotionEvent event) {
		super.onTouchEvent(event);
		
		if (event.getAction() == MotionEvent.ACTION_UP) {
			drawPin(event.getX(),event.getY());
		}
		return true;
	}
	
	
	public void drawPin (float posX, float posY) {
		pin.setPos(posX,posY);
		if (!visiblePin)
			visiblePin = true;
		invalidate();
	}
}
