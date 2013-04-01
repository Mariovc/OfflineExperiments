package com.gloria.offlineexperiments;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class ExerciseActivity extends Activity{



	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.exercise);

		//RelativeLayout mainLayout = (RelativeLayout)findViewById(R.id.mainLayout);

		//Add view using Java Code
		//ImageView imageView = new ImageView(AndroidAddViewActivity.this);
		//imageView.setImageResource(R.drawable.icon);
		
		//imageView.setLayoutParams(imageViewLayoutParams);

		

		TouchImageView2 imgTouchable = (TouchImageView2) findViewById(R.id.touchImageView1);
		//TouchImageView imgTouchable = new TouchImageView(this);
		imgTouchable.setImageBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.water_drops_wallpaper));
		imgTouchable.setMaxZoom(4f); //change the max level of zoom, default is 3f
		
		//LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		//imgTouchable.setLayoutParams(layoutParams);
		//imgTouchable.setId(R.id.imgTouchable);
		//mainLayout.addView(imgTouchable);
		

		//MarginLayoutParams margin = new MarginLayoutParams(layoutParams);
		//margin.leftMargin = 100;
		//margin.topMargin = 200;
		//margin.width = LayoutParams.WRAP_CONTENT;
		//margin.height = LayoutParams.WRAP_CONTENT;
		//ImageView pin2= new ImageView(this);
		//pin2.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.pin));
		//pin2.setImageResource(R.drawable.pin);
		//pin2.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		//pin2.setVisibility(ImageView.GONE);
		//mainLayout.addView(pin2);
		//pin2.setId(R.id.pin2);
		
		//Matrix pinMatrix = new Matrix();
		//pinMatrix.postTranslate(0, -8);
		//pin2.setImageMatrix(pinMatrix);
		//setContentView(imgTouchable);
	}

}
