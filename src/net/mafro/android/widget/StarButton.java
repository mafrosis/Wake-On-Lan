package net.mafro.android.widget;

import android.os.Bundle;

import android.content.Context;

import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import android.graphics.Canvas;

import android.util.AttributeSet;
import android.util.Log;

import net.mafro.android.wakeonlan.WakeOnLan;
import net.mafro.android.wakeonlan.R;


public class StarButton extends CompoundButton implements OnCheckedChangeListener
{

	private static final String TAG = "StarButton";


	public StarButton(Context context)
	{
		super(context);
		init(context);
	}

	public StarButton(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		init(context);
	}

	public StarButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}


	private void init(Context context)
	{
		setOnCheckedChangeListener(this);
		render();
	}

	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
	{
		render();
	}

	private void render()
	{
		//render the icon on this button
		if(isChecked() == true) {
			setButtonDrawable(R.drawable.star_on);
		}else{
			setButtonDrawable(R.drawable.star_off);
		}
	}

}