/**
 * @author Mario Velasco Casquero
 * @version 2.00
 */

package com.gloria.offlineexperiments;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ZoomablePinView extends ImageView{

	private float posX=0, posY=0;
	private float realPosX=0, realPosY=0; // position in pixels of the real Image
	private float width=0, height=0;

	private TextView numberView;
	private int number = 1;

	public ZoomablePinView(Context context) {
		super(context);
		setImageBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.marker_selected));
	}

	@Override
	public void setImageBitmap(Bitmap bm) { 
		super.setImageBitmap(bm);
		this.width = bm.getWidth();
		this.height = bm.getHeight();
	}

	public void setPosition (float posX, float posY, PointF centerPoint, PointF centerFocus, float saveScale,
			float scale, float redundantXSpace, float redundantYSpace) {
		this.posX = posX;
		this.posY = posY;
		setRealPosition(centerPoint, centerFocus, saveScale,
				scale, redundantXSpace, redundantYSpace);
		setMargins();
	}
	
	public void setPosition (float scale, float redundantXSpace, float redundantYSpace) {
		this.posX = this.realPosX * scale + redundantXSpace;
		this.posY = this.realPosY * scale + redundantYSpace;
		setMargins();
	}

	private void setRealPosition (PointF centerPoint, PointF centerFocus, float saveScale,
			float scale, float redundantXSpace, float redundantYSpace) {
		float deltaX = (posX - centerPoint.x) / saveScale;
		float deltaY = (posY - centerPoint.y) / saveScale;
		float posXOnScreen = centerFocus.x + deltaX;
		float posYOnScreen = centerFocus.y + deltaY;
		this.realPosX = (posXOnScreen - redundantXSpace) / scale;
		this.realPosY = (posYOnScreen - redundantYSpace) / scale;
	}
	
	public void setRealPosition (float realPosX, float realPosY) {
		this.realPosX = realPosX;
		this.realPosY = realPosY;
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
		int topMargin = (int) (posY - height/2);
		RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(getLayoutParams());
		layoutParams.setMargins( leftMargin, topMargin, 0, 0);
		setLayoutParams(layoutParams);
		setTextMargins();
	}
	
	private void setTextMargins () {
		numberView.measure(
		        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
		        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
		int leftMarginText = (int) (posX - numberView.getMeasuredWidth()/2);
		int topMarginText = (int) (posY - numberView.getMeasuredHeight()/2);
		RelativeLayout.LayoutParams textLayoutParams = new RelativeLayout.LayoutParams(getLayoutParams());
		textLayoutParams.setMargins(leftMarginText, topMarginText, 0, 0);
		numberView.setLayoutParams(textLayoutParams);
	}

	public PointF getRealPosition() {
		PointF pinPos = new PointF(realPosX, realPosY);
		return pinPos;
	}

	public float getPosX() {
		return posX;
	}

	public float getPosY() {
		return posY;
	}

	public int getNumber() {
		return number;
	}

	public float getRealPosX() {
		return realPosX;
	}

	public float getRealPosY() {
		return realPosY;
	}

	public void setNumber(int number) {
		this.number = number;
	}

	public TextView getNumberView() {
		return numberView;
	}

	public void setNumberView (TextView numberView) {
		this.numberView = numberView;
		numberView.setTextColor(getResources().getColor(R.color.black));
		numberView.setTextSize(getResources().getDimension(R.dimen.pin_number));
		setNumber();
	}

	public void increaseNumber () {
		number++;
		setNumber();
	}

	public void decreaseNumber () {
		if (number > 1)
			number--;
		setNumber();
	}

	private void setNumber () {
		numberView.setText(Integer.toString(number));
		setTextMargins();
	}

	@Override
	public void bringToFront() {
		super.bringToFront();
		numberView.bringToFront();
	}
	
	public void select () {
		setImageBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.marker_selected));
		setMargins();
		numberView.setTextColor(getResources().getColor(R.color.white));
		numberView.setTypeface(null, Typeface.BOLD);
	}
	
	public void unselect () {
		setImageBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.marker));
		setMargins();
		numberView.setTextColor(getResources().getColor(R.color.black));
		numberView.setTypeface(null, Typeface.NORMAL);
	}
}
