package com.gloria.offlineexperiments;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class ZoomablePinView extends ImageView{

	private float posX=0, posY=0;
	private float posXInPixels=0, posYInPixels=0;
	private float width=0, height=0;

	public ZoomablePinView(Context context) {
		super(context);
		setImageBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.pin));
	}

	@Override
	public void setImageBitmap(Bitmap bm) { 
		super.setImageBitmap(bm);
		this.width = bm.getWidth();
		this.height = bm.getHeight();
	}

	public void setPosition (float posX, float posY, PointF centerPoint, PointF centerFocus, float saveScale) {
		this.posX = posX;
		this.posY = posY;
		setRealPosition(centerPoint, centerFocus, saveScale);
		setMargins();
	}

	public void moveOnZoom (float focusX, float focusY, float scale) {
		posX = (scale * (posX - focusX)) + focusX;
		posY = (scale * (posY - focusY)) + focusY;
		setMargins();
	}

	public void moveOnDrag (float dx, float dy) {
		posX += dx;
		posY += dy;
		setMargins();
	}

	private void setMargins() {
		int leftMargin = (int) (posX - width/2);
		int topMargin = (int) (posY - height);
		RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(getLayoutParams());
		layoutParams.setMargins( leftMargin, topMargin, 0, 0);
		setLayoutParams(layoutParams);
	}
	
	public void drag (float dx, float dy, PointF centerPoint, PointF centerFocus, float saveScale) {
		moveOnDrag(dx, dy);
		setRealPosition(centerPoint, centerFocus, saveScale);
	}
	
	private void setRealPosition (PointF centerPoint, PointF centerFocus, float saveScale) {
		float deltaX = (posX - centerPoint.x) / saveScale;
		float deltaY = (posY - centerPoint.y) / saveScale;
		this.posXInPixels = centerFocus.x + deltaX;
		this.posYInPixels = centerFocus.y + deltaY;
	}

	public PointF getPositionInPixels() {
		PointF pinPos = new PointF(posXInPixels, posYInPixels);
		return pinPos;
	}

	public float getCenterPointViewX() {
		return posX;
	}

	public float getCenterPointViewY() {
		return posY - height/2;
	}
	
}
