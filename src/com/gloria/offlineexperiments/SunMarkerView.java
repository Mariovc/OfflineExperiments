package com.gloria.offlineexperiments;


import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class SunMarkerView extends ImageView {

	Matrix matrix;

	// We can be in one of these 3 states
	static final int NONE = 0;
	static final int DRAG = 1;
	static final int ZOOM = 2;
	static final int DRAG_PIN = 3;
	int mode = NONE;

	// Remember some things for zooming
	PointF last = new PointF();
	PointF start = new PointF();
	float minScale = 1f;
	float maxScale = 3f;
	float[] m;


	int viewWidth, viewHeight;
	static final int CLICK = 3;
	float saveScale = 1f;
	protected float origWidth, origHeight;
	int oldMeasuredWidth, oldMeasuredHeight;


	ScaleGestureDetector mScaleDetector;

	Context context;

	private ArrayList<ZoomablePinView> pins = new ArrayList<ZoomablePinView>();
	private int selectedPin = -1;
	private int dragginPin = -1;
	// Maximum distance between the clicked point and the pin, to be selected
	private static final int SELECTION_DISTANCE = 0;

	// Center of the focused area in pixels
	private PointF centerFocus = new PointF();
	// Center point of TouchImageView
	private final PointF centerPointView = new PointF();
	
	private int wolfNumber = 0;
	private TextView wolfNumberText = null;
	//observatory factor or the personal reduction coefficient
	private static final int k = 1;

	public SunMarkerView(Context context) {
		super(context);
		sharedConstructing(context);
	}

	public SunMarkerView(Context context, AttributeSet attrs) {
		super(context, attrs);
		sharedConstructing(context);
	}

	private void sharedConstructing(Context context) {
		super.setClickable(true);
		this.context = context;
		mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
		matrix = new Matrix();
		m = new float[9];
		setImageMatrix(matrix);
		setScaleType(ScaleType.MATRIX);

		setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				mScaleDetector.onTouchEvent(event);
				PointF curr = new PointF(event.getX(), event.getY());

				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					last.set(curr);
					start.set(last);
					dragginPin = searchClosestPin(curr.x, curr.y);
					if (dragginPin > -1)
						mode = DRAG_PIN;
					else 
						mode = DRAG;
					break;

				case MotionEvent.ACTION_MOVE:
					if (mode == DRAG) {
						float deltaX = curr.x - last.x;
						float deltaY = curr.y - last.y;
						float fixTransX = getFixDragTrans(deltaX, viewWidth, origWidth * saveScale);
						float fixTransY = getFixDragTrans(deltaY, viewHeight, origHeight * saveScale);
						matrix.postTranslate(fixTransX, fixTransY);
						fixTrans();
						last.set(curr.x, curr.y);
						moveCenterPointDrag(fixTransX, fixTransY);
						movePinsOnDrag(fixTransX, fixTransY);
					}
					else if (mode == DRAG_PIN) {
						float deltaX = curr.x - last.x;
						float deltaY = curr.y - last.y;
						last.set(curr.x, curr.y);
						pins.get(dragginPin).drag(deltaX, deltaY, centerPointView, centerFocus, saveScale);
					}
					break;

				case MotionEvent.ACTION_UP:
					if (mode == DRAG_PIN) {
						selectPin(dragginPin);
						//Toast.makeText(getContext(), "pin: " + selectedPin, Toast.LENGTH_SHORT).show();
					}
					else {
						int xDiff = (int) Math.abs(curr.x - start.x);
						int yDiff = (int) Math.abs(curr.y - start.y);
						if (xDiff < CLICK && yDiff < CLICK) {
							performClick();
							addPin(curr.x, curr.y, centerPointView, centerFocus, saveScale);
						}
					}
					mode = NONE;
					break;

				case MotionEvent.ACTION_POINTER_UP:
					mode = NONE;
					break;
				}

				setImageMatrix(matrix);
				invalidate();
				return true; // indicate event was handled
			}

		});
	}

	public void setMaxZoom(float x) {
		maxScale = x;
	}

	private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector) {
			mode = ZOOM;
			return true;
		}

		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			float mScaleFactor = detector.getScaleFactor();
			float origScale = saveScale;
			saveScale *= mScaleFactor;
			if (saveScale > maxScale) {
				saveScale = maxScale;
				mScaleFactor = maxScale / origScale;
			} else if (saveScale < minScale) {
				saveScale = minScale;
				mScaleFactor = minScale / origScale;
			}

			if (origWidth * saveScale <= viewWidth || origHeight * saveScale <= viewHeight) {
				matrix.postScale(mScaleFactor, mScaleFactor, centerPointView.x, centerPointView.y);
				moveCenterPointZoom(centerPointView.x, centerPointView.y, mScaleFactor);
				movePinsOnZoom(centerPointView.x, centerPointView.y, mScaleFactor);
			}
			else {
				matrix.postScale(mScaleFactor, mScaleFactor, detector.getFocusX(), detector.getFocusY());
				moveCenterPointZoom(detector.getFocusX(), detector.getFocusY(), mScaleFactor);
				movePinsOnZoom(detector.getFocusX(), detector.getFocusY(), mScaleFactor);
			}
			fixTrans();
			return true;
		}
	}

	void fixTrans() {
		matrix.getValues(m);
		float transX = m[Matrix.MTRANS_X];
		float transY = m[Matrix.MTRANS_Y];

		float fixTransX = getFixTrans(transX, viewWidth, origWidth * saveScale);
		float fixTransY = getFixTrans(transY, viewHeight, origHeight * saveScale);

		if (fixTransX != 0 || fixTransY != 0) {
			matrix.postTranslate(fixTransX, fixTransY);
			moveCenterPointDrag(fixTransX, fixTransY);
			movePinsOnDrag(fixTransX, fixTransY);
		}
	}

	float getFixTrans(float trans, float viewSize, float contentSize) {
		float minTrans, maxTrans;

		if (contentSize <= viewSize) {
			minTrans = 0;
			maxTrans = viewSize - contentSize;
		} else {
			minTrans = viewSize - contentSize;
			maxTrans = 0;
		}

		if (trans < minTrans)
			return -trans + minTrans;
		else if (trans > maxTrans)
			return -trans + maxTrans;
		else if (contentSize <= viewSize && mode == ZOOM)
			return ((minTrans + maxTrans) / 2) - trans;
		return 0;
	}

	float getFixDragTrans(float delta, float viewSize, float contentSize) {
		if (contentSize <= viewSize) {
			return 0;
		}
		return delta;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		viewWidth = MeasureSpec.getSize(widthMeasureSpec);
		viewHeight = MeasureSpec.getSize(heightMeasureSpec);

		centerFocus.x = centerPointView.x = (float) viewWidth/2;
		centerFocus.y = centerPointView.y = (float) viewHeight/2;

		//
		// Rescales image on rotation
		//
		if (oldMeasuredHeight == viewWidth && oldMeasuredHeight == viewHeight
				|| viewWidth == 0 || viewHeight == 0)
			return;
		oldMeasuredHeight = viewHeight;
		oldMeasuredWidth = viewWidth;

		if (saveScale == 1) {
			//Fit to screen.
			float scale;

			Drawable drawable = getDrawable();
			if (drawable == null || drawable.getIntrinsicWidth() == 0 || drawable.getIntrinsicHeight() == 0)
				return;
			int bmWidth = drawable.getIntrinsicWidth();
			int bmHeight = drawable.getIntrinsicHeight();

			Log.d("bmSize", "bmWidth: " + bmWidth + " bmHeight : " + bmHeight);

			float scaleX = (float) viewWidth / (float) bmWidth;
			float scaleY = (float) viewHeight / (float) bmHeight;
			scale = Math.min(scaleX, scaleY);
			matrix.setScale(scale, scale);

			// Center the image
			float redundantYSpace = (float) viewHeight - (scale * (float) bmHeight);
			float redundantXSpace = (float) viewWidth - (scale * (float) bmWidth);
			redundantYSpace /= 2;
			redundantXSpace /= 2;

			matrix.postTranslate(redundantXSpace, redundantYSpace);

			origWidth = viewWidth - 2 * redundantXSpace;
			origHeight = viewHeight - 2 * redundantYSpace;
			setImageMatrix(matrix);
		}
		fixTrans();
	}

	public void addPin(float posX, float posY, PointF centerPoint, PointF centerFocus, float saveScale) {
		ZoomablePinView pin = new ZoomablePinView(context);
		pin.setLayoutParams(new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT));
		pins.add(pin);
		ViewGroup parent = (ViewGroup) getParent();
		parent.addView(pin);
		TextView numberView = new TextView(context);
		parent.addView(numberView);
		pin.setNumberView(numberView);
		selectPin(pins.size() - 1);
		pin.setPosition(posX, posY, centerPoint, centerFocus, saveScale);
		calculateWolfNumber();
	}

	public void removePin(){
		if (selectedPin > -1){
			ViewGroup parent = (ViewGroup) getParent();
			parent.removeView(pins.get(selectedPin).getNumberView());
			parent.removeView(pins.get(selectedPin));
			pins.remove(selectedPin);
			selectedPin = -1;
			selectPin(pins.size() - 1);
			calculateWolfNumber();
		}
	}

	private void selectPin(int pinNumber){
		if (selectedPin > -1) 
			pins.get(selectedPin).unselect();
		if (pinNumber > -1) {
			selectedPin = pinNumber;
			pins.get(selectedPin).select();
			pins.get(selectedPin).bringToFront();
		}
	}

	public void increasePin () {
		if (selectedPin > -1) {
			pins.get(selectedPin).increaseNumber();
		calculateWolfNumber();
		}
	}

	public void decreasePin () {
		if (selectedPin > -1){
			pins.get(selectedPin).decreaseNumber();
		calculateWolfNumber();
		}
	}

	public ZoomablePinView getPin() {
		if (selectedPin == -1)
			return null;
		else
			return pins.get(selectedPin);
	}

	private void moveCenterPointZoom (float focusX, float focusY, float scale) {
		float focusDistanceX = centerPointView.x - focusX;
		float focusDistanceY = centerPointView.y - focusY;
		float deltaX = focusDistanceX / scale - focusDistanceX;
		float deltaY = focusDistanceY / scale - focusDistanceY;
		centerFocus.x += deltaX / (saveScale / scale);
		centerFocus.y += deltaY / (saveScale / scale);
	}

	private void moveCenterPointDrag (float deltaX, float deltaY) {
		centerFocus.x -= deltaX / saveScale;
		centerFocus.y -= deltaY / saveScale;
	}

	private void movePinsOnZoom(float focusX, float focusY, float scale) {
		for (int i = 0; i < pins.size(); i++) 
			pins.get(i).moveOnZoom(focusX, focusY, scale);
	}

	private void movePinsOnDrag(float deltaX, float deltaY) {
		for (int i = 0; i < pins.size(); i++) 
			pins.get(i).moveOnDrag(deltaX, deltaY);
	}

	private int searchClosestPin (float posX, float posY){
		int index = -1;
		int maxDistanceX = 0;
		int maxDistanceY = 0;
		double distanceClosestPin = 1000f;
		for (int i = 0; i < pins.size(); i++) {
			ZoomablePinView pin = pins.get(i);
			if (i == 0){
				maxDistanceX = pin.getWidth() + SELECTION_DISTANCE;
				maxDistanceY = pin.getHeight() + SELECTION_DISTANCE;
			}
			float distanceX = Math.abs(pin.getCenterPointViewX() - posX);
			float distanceY = Math.abs(pin.getCenterPointViewY() - posY);
			double distance = Math.hypot(distanceX, distanceY);
			if (distanceX < maxDistanceX && distanceY < maxDistanceY && distance < distanceClosestPin) {
				index = i;
				distanceClosestPin = distance;
			}
		}
		return index;
	}

	private void calculateWolfNumber() {
		if (wolfNumberText != null) {
			int individualSpots = 0;
			for (int i = 0; i < pins.size(); i++) {
				individualSpots += pins.get(i).getNumber();
			}
			wolfNumber = k * (10 * pins.size() + individualSpots);
			wolfNumberText.setText("Wolf Number: R = " + wolfNumber);
		}
	}
	
	public void setWolfNumberText (TextView textView) {
		this.wolfNumberText = textView;
	}
}