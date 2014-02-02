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

import android.app.PendingIntent;

import android.database.Cursor;

import android.net.Uri;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;

import android.content.Intent;
import android.content.Context;
import android.content.ContentValues;

import android.widget.RemoteViews;

import android.util.Log;

/**
 * @desc	This class is used to setup the home screen widget, as well as handle click events
 */

public class WidgetProvider extends AppWidgetProvider
{

	public static final String TAG = "WidgetProvider";

	public static final String WIDGET_ONCLICK = "net.mafro.android.wakeonlan.WidgetOnClick";

	/**
	 * @desc	this method is called when the widgets need to be drawn
	 */
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
	{
		super.onUpdate(context, appWidgetManager, appWidgetIds);

		final int N = appWidgetIds.length;
		for(int i=0; i<N; i++) {
			int widget_id = appWidgetIds[i];

			HistoryItem item = loadHistoryItem(context, widget_id);

			// do nothing for widgets which point to a missing HistoryItem
			if(item == null) {
				return;
			}
			configureWidget(widget_id, item.title, context);
		}
	}

	@Override
	public void onReceive(Context context, Intent intent)
	{
		super.onReceive(context, intent);

		if(intent.getAction().startsWith(WIDGET_ONCLICK)) {
			// get the widget_id from the Intent
			int widget_id = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

			// get the HistoryItem associated with the widget_id
			HistoryItem item = loadHistoryItem(context, widget_id);

			if(item == null) {
				// this can only happen if a user clears all app data via settings
				// update the HistoryItem to be widget_id = 0, so it's removed on the next AppWidgetManager update
				configureWidget(widget_id, "", context);
				return;
			}

			// send the packet
			WakeOnLanActivity.sendPacket(context, item.title, item.mac, item.ip, item.port);
		}
	}

	@Override
	public void onDeleted(Context context, int[] id)
	{
		super.onDeleted(context, id);

		final int N = id.length;
		for(int i=0; i<N; i++) {
			deleteHistoryItemWidget(context, id[i]);
		}
	}

	/**
	 * @desc	configures a widget for the first time. Usually called when creating a widget
	 *				or initialising existing widgets when the device is rebooted.
	 */
	public static void configureWidget(int widget_id, String title, Context context)
	{
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
		views.setTextViewText(R.id.appwidget_text, title);

		// append id to action to prevent clearing the extras bundle
		views.setOnClickPendingIntent(R.id.appwidget_button, getPendingSelfIntent(context, widget_id, WIDGET_ONCLICK + widget_id));

		// tell the widget manager
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		appWidgetManager.updateAppWidget(widget_id, views);
	}

	private static PendingIntent getPendingSelfIntent(Context context, int widget_id, String action)
	{
		Intent intent = new Intent(context, WidgetProvider.class);
		intent.setAction(action);
		Bundle bundle = new Bundle();
		bundle.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, widget_id);
		intent.putExtras(bundle);
		return PendingIntent.getBroadcast(context, 0, intent, 0);
	}

	public void deleteHistoryItemWidget(Context context, int widget_id)
	{
		HistoryItem item = loadHistoryItem(context, widget_id);

		// if back-button pressed during WidgetConfigure, item is null
		if(item == null) {
			return;
		}

		// update widget_id to zero on History table
		ContentValues values = new ContentValues(1);
		values.put(History.Items.WIDGET_ID, 0);

		Uri itemUri = Uri.withAppendedPath(History.Items.CONTENT_URI, Integer.toString(item.id));
		context.getContentResolver().update(itemUri, values, null, null);
	}

	private HistoryItem loadHistoryItem(Context context, int widget_id)
	{
		// load History cursor via custom ResourceAdapter
		Cursor cursor = context.getContentResolver().query(History.Items.CONTENT_URI, HistoryListHandler.PROJECTION, null, null, null);
		HistoryAdapter adapter = new HistoryAdapter(context, cursor, false);

		while(cursor.moveToNext()) {
			HistoryItem item = new HistoryItem(cursor);
			if(item.widget_id == widget_id) {
				return item;
			}
		}

		// this covers widgets which point to deleted HistoryItems
		return null;
	}

}
