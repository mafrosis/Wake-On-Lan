/*
Copyright (C) 2008-2014 Matt Black
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

package net.mafro.android.wakeonlan.adapter;

import android.os.Bundle;

import android.content.Context;
import android.content.ContentValues;

import android.database.Cursor;

import android.net.Uri;

import android.util.Log;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import android.widget.TextView;
import android.widget.AdapterView;
import android.widget.ResourceCursorAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import net.mafro.android.widget.StarButton;

import net.mafro.android.wakeonlan.database.Definitions;
import net.mafro.android.wakeonlan.R;


/**
 *	@desc	Custom adapter to aid in UI binding
 */
public class HistoryAdapter extends ResourceCursorAdapter implements OnCheckedChangeListener
{

	private static final String TAG = "HistoryAdapter";

	private Context context;

	boolean showStars;


	public HistoryAdapter(Context context, Cursor cursor, boolean showStars)
	{
		super(context, R.layout.history_row, cursor);
		this.context = context;
		this.showStars = showStars;
	}


	@Override
	public void bindView(View view, Context context, Cursor cursor)
	{
		// load our column indexes
		int idColumn = cursor.getColumnIndex(Definitions.Items._ID);
		int titleColumn = cursor.getColumnIndex(Definitions.Items.TITLE);
		int macColumn = cursor.getColumnIndex(Definitions.Items.MAC);
		int ipColumn = cursor.getColumnIndex(Definitions.Items.IP);
		int portColumn = cursor.getColumnIndex(Definitions.Items.PORT);
		int isStarredColumn = cursor.getColumnIndex(Definitions.Items.IS_STARRED);

		TextView vtitle = (TextView) view.findViewById(R.id.history_row_title);
		TextView vmac = (TextView) view.findViewById(R.id.history_row_mac);
		TextView vip = (TextView) view.findViewById(R.id.history_row_ip);
		TextView vport = (TextView) view.findViewById(R.id.history_row_port);
		StarButton star = (StarButton) view.findViewById(R.id.history_row_star);

		// bind the cursor data to the form items
		vtitle.setText(cursor.getString(titleColumn));
		vmac.setText(cursor.getString(macColumn));
		vip.setText(cursor.getString(ipColumn));
		vport.setText(Integer.toString(cursor.getInt(portColumn)));

		if(this.showStars == true) {
			// remove click handler to prevent recursive calls
			star.setOnCheckedChangeListener(null);

			// change the star state if different
			boolean starred = (cursor.getInt(isStarredColumn) != 0);	// non-zero == true
			star.setChecked(starred);
			star.render();

			// add event listener to star button
			star.setOnCheckedChangeListener(this);

			// save our record _ID in the star's tag
			star.setTag(cursor.getInt(idColumn));

		}else{
			// disable the star button
			star.setClickable(false);
			star.noRender = true;
			star.render();
		}
	}


	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
	{
		// extract record's _ID from tag
		int id = ((Integer) ((StarButton) buttonView).getTag()).intValue();

		if(isChecked) {
			setIsStarred(id, 1);
		}else{
			setIsStarred(id, 0);
		}
	}

	private void setIsStarred(int id, int value) {
		// update history setting is_starred to value
		ContentValues values = new ContentValues(1);
		values.put(Definitions.Items.IS_STARRED, value);

		Uri itemUri = Uri.withAppendedPath(Definitions.Items.CONTENT_URI, Integer.toString(id));
		context.getContentResolver().update(itemUri, values, null, null);
	}

}
