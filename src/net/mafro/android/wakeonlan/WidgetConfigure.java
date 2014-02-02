/*
Copyright (C) 2013-2014 Yohan Pereira, Matt Black
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

package net.mafro.android.wakeonlan;

import android.os.Bundle;

import android.app.Activity;

import android.appwidget.AppWidgetManager;

import android.net.Uri;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.widget.ListView;

/**
 * @desc	This class is used to configure the home screen widget
 */
public class WidgetConfigure extends Activity
{

	public static final String TAG = "WidgetConfigure";

	private HistoryListHandler historyListHandler;
	private int widget_id;
	private SharedPreferences settings;


	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Set the result to CANCELED. This will cause the widget host to cancel
		// out of the widget placement if they press the back button.
		setResult(RESULT_CANCELED);

		setContentView(R.layout.widget_configure);
		ListView lv = (ListView)findViewById(R.id.history);
		historyListHandler = new HistoryListHandler(this, lv);

		settings = getSharedPreferences(WakeOnLanActivity.TAG, 0);
		int sort_mode = settings.getInt("sort_mode", WakeOnLanActivity.CREATED);
		historyListHandler.bind(sort_mode);

		// add on click listener
		historyListHandler.addHistoryListClickListener(new HistoryListClickListener () {
			public void onClick(HistoryItem item) {
				selected(item);
			}
		});

		// get the widget_id from the Intent
		this.widget_id = getIntent().getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
	}

	private void selected(HistoryItem item)
	{
		// save widget_id against the History item in our DB
		ContentValues values = new ContentValues(1);
		values.put(History.Items.WIDGET_ID, widget_id);

		Uri itemUri = Uri.withAppendedPath(History.Items.CONTENT_URI, Integer.toString(item.id));
		getContentResolver().update(itemUri, values, null, null);

		// configure the widget
		WidgetProvider.configureWidget(widget_id, item.title, this);

		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widget_id);
		setResult(RESULT_OK, resultValue);
		finish();
	}

}
