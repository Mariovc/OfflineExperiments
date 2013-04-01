package com.gloria.offlineexperiments;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Toast;

public class PinObject extends View{

	

	private View view;
	private Drawable drawablePin;
	
	private int width, height;
	private float posX=0, posY=0;

	public PinObject(Context context) {
		super(context);
		drawablePin = context.getResources().getDrawable(R.drawable.pin);
		width = drawablePin.getIntrinsicWidth();  
		height = drawablePin.getIntrinsicHeight();
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		//Toast.makeText(getContext(), "onDraw Pin", Toast.LENGTH_SHORT/10).show();
		drawablePin.setBounds((int)posX-width/2, (int)posY-height, (int)posX+width/2, (int)posY);
		drawablePin.draw(canvas);
	}
	/*public void drawPin(Canvas canvas){
		//canvas.save();
		//int x=(int) (posX+width/2);
		//int y=(int) (posY+height/2);
		//canvas.rotate((float) angulo,(float) x,(float) y);
		
		drawablePin.draw(canvas);
		//canvas.restore();
		//int rInval = (int) Math.hypot(width,height)/2 + MAX_VELOCIDAD;
		//view.invalidate(x-rInval, y-rInval, x+rInval, y+rInval);
	}*/

	public void setPos(float posX, float posY) {
		this.posX = posX;
		this.posY = posY;
		//invalidate();
		//Toast.makeText(getContext(), "setPos " + posX + "," + posY, Toast.LENGTH_SHORT).show();
	}
	

}
