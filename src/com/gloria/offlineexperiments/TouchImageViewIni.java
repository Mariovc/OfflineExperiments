package com.gloria.offlineexperiments;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class TouchImageViewIni extends ImageView {

	Matrix matrix = new Matrix();

	// We can be in one of these 3 states
	static final int NONE = 0;
	static final int DRAG = 1;
	static final int ZOOM = 2;
	int mode = NONE;

	// Remember some things for zooming
	PointF last = new PointF();
	PointF start = new PointF();
	float minScale = 1f;
	float maxScale = 3f;
	float[] m;

	float redundantXSpace, redundantYSpace;

	float screenWidth, screenHeight;
	static final int CLICK = 3;
	float saveScale = 1f;
	float right, bottom, bmScaledWidth, bmScaledHeight, bmWidth, bmHeight;

	ScaleGestureDetector mScaleDetector;

	Context context;

	private boolean visiblePin = false;
	private ImageView pin2;
	private float pinPosX=0, pinPosY=0;

	private PointF centerPoint = new PointF();


	public TouchImageViewIni(Context context, AttributeSet attrs) {
		super(context, attrs);
		super.setClickable(true);
		this.context = context;

		mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
		matrix.setTranslate(1f, 1f);
		m = new float[9];
		setImageMatrix(matrix);
		setScaleType(ScaleType.MATRIX);


		setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				mScaleDetector.onTouchEvent(event);

				matrix.getValues(m);
				float x = m[Matrix.MTRANS_X];
				float y = m[Matrix.MTRANS_Y];
				PointF curr = new PointF(event.getX(), event.getY());

				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					last.set(event.getX(), event.getY());
					start.set(last);
					mode = DRAG;
					break;
				case MotionEvent.ACTION_MOVE:
					if (mode == DRAG) {
						float deltaX = curr.x - last.x;
						float deltaY = curr.y - last.y;
						float scaleWidth = Math.round(bmScaledWidth * saveScale);
						float scaleHeight = Math.round(bmScaledHeight * saveScale);
						if (scaleWidth < screenWidth) {
							deltaX = 0;
							if (y + deltaY > 0)
								deltaY = -y;
							else if (y + deltaY < -bottom)
								deltaY = -(y + bottom); 
						} else if (scaleHeight < screenHeight) {
							deltaY = 0;
							if (x + deltaX > 0)
								deltaX = -x;
							else if (x + deltaX < -right)
								deltaX = -(x + right);
						} else {
							if (x + deltaX > 0)
								deltaX = -x;
							else if (x + deltaX < -right)
								deltaX = -(x + right);

							if (y + deltaY > 0)
								deltaY = -y;
							else if (y + deltaY < -bottom)
								deltaY = -(y + bottom);
						}
						matrix.postTranslate(deltaX, deltaY);
						last.set(curr.x, curr.y);
						movePinPosition(deltaX, deltaY);
						moveCenterPointDrag(deltaX, deltaY);
					}
					break;

				case MotionEvent.ACTION_UP:
					mode = NONE;
					int xDiff = (int) Math.abs(curr.x - start.x);
					int yDiff = (int) Math.abs(curr.y - start.y);
					if (xDiff < CLICK && yDiff < CLICK) {
						markPosition(start.x, start.y);
						performClick();
					}
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


	private void movePinPositionZoom (float focusX, float focusY, float scale) {
		if (visiblePin) {
			pinPosX = (scale * (pinPosX - focusX)) + focusX;
			pinPosY = (scale * (pinPosY - focusY)) + focusY;
			adjustPinPosition();
		}
	}

	private void movePinPosition (float dx, float dy) {
		if (visiblePin) {
			pinPosX += dx;
			pinPosY += dy;
			adjustPinPosition();
		}
	}
	private void markPosition (float posX, float posY) {
		if (!visiblePin)
			drawPin();
		pinPosX = posX;
		pinPosY = posY;
		adjustPinPosition();
	}

	private void adjustPinPosition() {
		int leftMargin = (int) (pinPosX - pin2.getWidth()/2);
		int topMargin = (int) (pinPosY - pin2.getHeight());
		MarginLayoutParams marginParams = new MarginLayoutParams(pin2.getLayoutParams());
		marginParams.setMargins( leftMargin, topMargin, 0, 0);
		RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(marginParams);
		pin2.setLayoutParams(layoutParams);
	}

	private void drawPin() {
		visiblePin = true;
		View parent = (View) getParent();
		//pin2 = (ImageView) parent.findViewById(R.id.pinView);
		pin2.setVisibility(VISIBLE);
	}

	@Override
	public void setImageBitmap(Bitmap bm) { 
		super.setImageBitmap(bm);
		bmWidth = bm.getWidth();
		bmHeight = bm.getHeight();
	}

	public void setMaxZoom(float x)
	{
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
			float mScaleFactor = (float)Math.min(Math.max(.95f, detector.getScaleFactor()), 1.05);
			float origScale = saveScale;
			float focusX;
			float focusY;
			saveScale *= mScaleFactor;
			if (saveScale > maxScale) {
				saveScale = maxScale;
				mScaleFactor = maxScale / origScale;
			} else if (saveScale < minScale) {
				saveScale = minScale;
				mScaleFactor = minScale / origScale;
			}
			right = screenWidth * saveScale - screenWidth - (2 * redundantXSpace * saveScale);
			bottom = screenHeight * saveScale - screenHeight - (2 * redundantYSpace * saveScale);

			//if the scaled image doesn't fill the screen width/height
			if (bmScaledWidth * saveScale <= screenWidth || bmScaledHeight * saveScale <= screenHeight) {
				matrix.postScale(mScaleFactor, mScaleFactor, screenWidth / 2, screenHeight / 2);
				focusX = screenWidth/2;
				focusY = screenHeight/2;
				if (mScaleFactor < 1) {		//zoom out
					matrix.getValues(m);
					float x = m[Matrix.MTRANS_X];
					float y = m[Matrix.MTRANS_Y];
					if (mScaleFactor < 1) {		//zoom out
						if (Math.round(bmScaledWidth * saveScale) < screenWidth) {
							if (y < -bottom) {
								//Toast.makeText(getContext(), "1", Toast.LENGTH_SHORT).show();
								matrix.postTranslate(0, -(y + bottom));
								focusY = screenHeight;
							}else if (y > 0){
								//Toast.makeText(getContext(), "2", Toast.LENGTH_SHORT).show();
								matrix.postTranslate(0, -y);
								focusY = 0;
							}
						} else {
							if (x < -right) {
								//Toast.makeText(getContext(), "3", Toast.LENGTH_SHORT).show();
								matrix.postTranslate(-(x + right), 0);
								focusX = screenWidth;
							}
							else if (x > 0) {
								//Toast.makeText(getContext(), "4", Toast.LENGTH_SHORT).show();
								matrix.postTranslate(-x, 0);
								focusX = 0;
							}
						}
					}
				}
			} else {
				matrix.postScale(mScaleFactor, mScaleFactor, detector.getFocusX(), detector.getFocusY());
				focusX = detector.getFocusX();
				focusY = detector.getFocusY();
				matrix.getValues(m);
				float x = m[Matrix.MTRANS_X];
				float y = m[Matrix.MTRANS_Y];
				if (mScaleFactor < 1) {
					if (x < -right) {
						matrix.postTranslate(-(x + right), 0);
						focusX = screenWidth;
					} else if (x > 0) {
						matrix.postTranslate(-x, 0);
						focusX = 0;
					}
					if (y < -bottom) {
						matrix.postTranslate(0, -(y + bottom));
						focusY = screenHeight;
					} else if (y > 0) {
						matrix.postTranslate(0, -y);
						focusY = 0;
					}
				}
			}
			// movePin
			movePinPositionZoom(focusX, focusY, mScaleFactor);
			moveCenterPointZoom(focusX,focusY, mScaleFactor);
			return true;
		}
	}

	@Override
	protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec)
	{
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		screenWidth = MeasureSpec.getSize(widthMeasureSpec);
		screenHeight = MeasureSpec.getSize(heightMeasureSpec);

		centerPoint.x = screenWidth/2;
		centerPoint.y = screenHeight/2;

		//Fit to screen.
		float scale;
		float scaleX =  (float)screenWidth / (float)bmWidth;
		float scaleY = (float)screenHeight / (float)bmHeight;
		scale = Math.min(scaleX, scaleY);
		matrix.setScale(scale, scale);
		setImageMatrix(matrix);
		saveScale = 1f;

		// Center the image
		redundantYSpace = (float)screenHeight - (scale * (float)bmHeight) ;
		redundantXSpace = (float)screenWidth - (scale * (float)bmWidth);
		redundantYSpace /= (float)2;
		redundantXSpace /= (float)2;
		matrix.postTranslate(redundantXSpace, redundantYSpace);
		setImageMatrix(matrix);

		bmScaledWidth = screenWidth - 2 * redundantXSpace;
		bmScaledHeight = screenHeight - 2 * redundantYSpace;
		right = screenWidth * saveScale - screenWidth - (2 * redundantXSpace * saveScale);
		bottom = screenHeight * saveScale - screenHeight - (2 * redundantYSpace * saveScale);
	}

	private void moveCenterPointZoom (float focusX, float focusY, float scale) {
		float centerViewX = screenWidth/2;
		float centerViewY = screenHeight/2;
		float focusDistanceX = centerViewX - focusX;
		float focusDistanceY = centerViewY - focusY;
		float deltaX = 0;
		float deltaY = 0;
		if (scale != 1) {
			deltaX = focusDistanceX / scale - focusDistanceX;
			deltaY = focusDistanceY / scale - focusDistanceY;
			centerPoint.x += deltaX / saveScale / scale;
			centerPoint.y += deltaY / saveScale / scale;
		}
	}

	private void moveCenterPointDrag (float deltaX, float deltaY) {
		centerPoint.x -= deltaX / saveScale;
		centerPoint.y -= deltaY / saveScale;
	}

	public PointF getPinPosition() {
		PointF pinPos = new PointF();
		float deltaX;
		float deltaY;
		if (!visiblePin) {
			//pinPos.x = pinPosY = 0;

			pinPos.x = centerPoint.x;
			pinPos.y = centerPoint.y;
		}
		else {
			deltaX = (pinPosX - screenWidth/2) / saveScale;
			deltaY = (pinPosY - screenHeight/2) / saveScale;
			pinPos.x = centerPoint.x + deltaX;
			pinPos.y = centerPoint.y + deltaY;
		}
		return pinPos;
	}
}
