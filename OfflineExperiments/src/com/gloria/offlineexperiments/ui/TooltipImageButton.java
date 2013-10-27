package com.gloria.offlineexperiments.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.ImageButton;
import android.widget.Toast;

public class TooltipImageButton extends ImageButton implements
		OnLongClickListener {

	public TooltipImageButton(Context context) {
		super(context);
		setOnLongClickListener(this);
	}

	public TooltipImageButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		setOnLongClickListener(this);
	}

	public TooltipImageButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setOnLongClickListener(this);
	}

	@Override
	public boolean onLongClick(View v) {
		Toast.makeText(getContext(), getContentDescription(),
				Toast.LENGTH_SHORT).show();
		return true;
	}

}
