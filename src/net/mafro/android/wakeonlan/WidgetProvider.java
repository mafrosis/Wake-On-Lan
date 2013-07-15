/*
Copyright (C) 2013 Yohan Pereira.
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


import android.app.PendingIntent;

import android.database.Cursor;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;

import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;

import android.os.Bundle;

import android.widget.RemoteViews;
import android.util.Log;

/**
 * @desc This class is used to setup the home screen widget, as well as handle click events
 */

//TODO handle deletion of widgets.

public class WidgetProvider extends AppWidgetProvider {

	public static final String SETTINGS_PREFIX="widget_";
	public static final String WIDGET_ONCLICK="net.mafro.android.wakeonlan.WidgetOnClick";

	private SharedPreferences settings;

	public WidgetProvider() {
		//Need a context to initlise this.
		settings = null;
	}

	/**
	 * @desc Called when an instance of this class is activated. Do init stuff here
	 */
	@overide
	public void onEnabled(Context context) {
		if(settings == null) {
			settings = context.getSharedPreferences(WakeOnLanActivity.TAG, 0);
		}
	}

	/**
	 * @desc this method is called once when the WidgetHost starts (usually when the OS boots).
	 */
	@overide
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

		final int N = appWidgetIds.length;
		for (int i=0; i<N; i++) {
			int widget_id = appWidgetIds[i];
			
			
			HistoryItem item = loadItemPref(context, settings, widget_id);
			if(item == null) { // item or prefrences missing.
				//TODO delete the widget probably.
				continue;
			}
			configureWidget(widget_id, item, context);
		}
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		//
		super.onReceive(context, intent);

		if (intent.getAction().startsWith(WIDGET_ONCLICK)) {

			//get the widget id
			int widget_id = getWidgetId(intent);
			if(widget_id == AppWidgetManager.INVALID_APPWIDGET_ID) {
				return;
			}
			
			//get the HistoryItem associated with the widget_id.
			HistoryItem item = loadItemPref(context, settings, widget_id);

			//send the packet.
			WakeOnLanActivity.sendPacket(context, 
						     item.title, 
						     item.mac, 
						     item.ip, 
						     item.port);
		}
	}

	@Override
	public void onDelete(Context context, int[] id) {
		final int N = appWidgetIds.length;
		for (int i=0; i<N; i++) {
			deleteItemPref(settings, id[N]);
		}
	}

	/**
	 * @desc gets the widget id from an intent.
	 */
	public static int getWidgetId(Intent intent) {
		Bundle extras = intent.getExtras();
		int _widget_id = AppWidgetManager.INVALID_APPWIDGET_ID;
		if (extras != null) {
			_widget_id = extras.getInt(
					AppWidgetManager.EXTRA_APPWIDGET_ID, 
					AppWidgetManager.INVALID_APPWIDGET_ID);

		}
		return _widget_id;	
	}

	/**
	 * @desc configures a widget for the first time. Usually called when creating a widget
	 *	 for the first time or initlising existing widgets when the AppWidgetManager 
	 *	restarts (usually when the phone reboots).
	 */
	public static void configureWidget(int widget_id, HistoryItem item, Context context) {

		RemoteViews views = new RemoteViews(context.getPackageName(),
		R.layout.widget);
		views.setTextViewText(R.id.appwidget_text, item.title);
		//FIXME hack : append id to action  to prevent clearing the extras bundle. 
		views.setOnClickPendingIntent(R.id.appwidget_button, getPendingSelfIntent(context, widget_id, WIDGET_ONCLICK + widget_id));

		//tell the widget manager
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		appWidgetManager.updateAppWidget(widget_id, views);
	}
	
	

	private static  PendingIntent getPendingSelfIntent(Context context, int widget_id, String action) {
		//TODO try and set the widget id here. (not sure what this means anymore).
		Intent intent = new Intent(context, WidgetProvider.class);
		intent.setAction(action);
		Bundle bundle = new Bundle();
		bundle.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, widget_id);
		intent.putExtras(bundle);
		return PendingIntent.getBroadcast(context, 0, intent, 0);
	}
	
	/**
	 * @desc saves the given history item/widget_id combination.
	 */
	public static void saveItemPref(SharedPreferences settings, HistoryItem item, int widget_id) {
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt(SETTINGS_PREFIX + widget_id, item.id);
		editor.commit();
	}

	public static void deleteItemPref(SharedPreferences settings, int widget_id) {
		SharedPreferences.Editor editor = settings.edit();
		editor.remove(SETTINGS_PREFIX + widget_id);
		editor.commit();
	}

	/**
	 * @desc load the HistoryItem associated with a widget_id.
	 */
	public static HistoryItem loadItemPref(Context context, SharedPreferences settings, int widget_id) {
		//get item_id
		int item_id = settings.getInt(SETTINGS_PREFIX + widget_id, -1);
		if(item_id == -1) {
			//No item_id found for given widget return null.
			return null;
		}

		//Create HistoryItem
		Cursor cursor = context.getContentResolver()
				.query(History.Items.CONTENT_URI, 
					HistoryListHandler.PROJECTION, 
					History.Items._ID +" = "+item_id,
					null, null);
		if(cursor.getCount() == 0) {
			//item_id does not exists anymore
			//TODO copy the item details so that we dont have to worry about this.
			return null;
		}
		cursor.moveToFirst();
		HistoryItem item = HistoryListHandler.getItem(cursor);
		return item;

	}
	
}
