/*
Copyright (C) 2008-2012 Matt Black.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.
* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.
* Neither the name of the author nor the names of its contributors may be used
  to endorse or promote products derived from this software without specific
  prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package net.mafro.android.widget;

import android.os.Bundle;

import android.content.Context;

import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import android.graphics.Canvas;

import android.util.AttributeSet;
import android.util.Log;

import net.mafro.android.wakeonlan.R;


/**
 *	@desc	Custom button type to implement Google-style favourite star
 */
public class StarButton extends CompoundButton implements OnCheckedChangeListener
{

	private static final String TAG = "StarButton";

	public boolean noRender = false;


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

	public void render()
	{
		//render the icon on this button
		if(noRender == true) {
			setButtonDrawable(android.R.color.transparent);
		}else if(isChecked() == true) {
			setButtonDrawable(R.drawable.btn_star_big_on);
		}else{
			setButtonDrawable(R.drawable.btn_star_big_off);
		}
	}

}
