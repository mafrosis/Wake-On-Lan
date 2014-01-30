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

package net.mafro.android.wakeonlan;

import android.app.Activity;

import android.content.ContentValues;

import android.database.Cursor;

import android.util.Log;

import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import android.view.View;

import android.net.Uri;

import java.util.List;
import java.util.ArrayList;


/**
 *	@desc	Class handles all functions of the history ListView
 */
public class HistoryListHandler implements OnItemClickListener
{

	public static final String TAG = "HistoryListHandler";

	private Activity parent;
	private Cursor cursor;
	private HistoryAdapter adapter;
	private List<HistoryListClickListener> listeners;


	public static final String[] PROJECTION = new String[]
	{
		History.Items._ID,
		History.Items.TITLE,
		History.Items.MAC,
		History.Items.IP,
		History.Items.PORT,
		History.Items.LAST_USED_DATE,
		History.Items.USED_COUNT,
		History.Items.IS_STARRED
	};

	private ListView view = null;


	public HistoryListHandler(Activity parent, ListView view)
	{
		this.parent = parent;
		this.view = view;
		this.listeners = new ArrayList<HistoryListClickListener>();
	}

	public void bind(int sort_mode)
	{
		String orderBy = null;
		switch (sort_mode) {
			case WakeOnLanActivity.CREATED:
				orderBy = History.Items.IS_STARRED+" DESC, "+History.Items.CREATED_DATE+" DESC";
				break;
			case WakeOnLanActivity.LAST_USED:
				orderBy = History.Items.IS_STARRED+" DESC, "+History.Items.LAST_USED_DATE+" DESC";
				break;
			case WakeOnLanActivity.USED_COUNT:
				orderBy = History.Items.IS_STARRED+" DESC, "+History.Items.USED_COUNT+" DESC";
				break;
		}

		// determine if we render the favourite star buttons
		boolean showStars = false;
		if(parent instanceof WakeOnLanActivity) {
			showStars = true;
		}

		// load History cursor via custom ResourceAdapter
		cursor = parent.getContentResolver().query(History.Items.CONTENT_URI, PROJECTION, null, null, orderBy);
		adapter = new HistoryAdapter(parent, cursor, showStars);

		// register self as listener for item clicks
		view.setOnItemClickListener(this);

		// bind to the supplied view
		view.setAdapter(adapter);
	}


	public void onItemClick(AdapterView av, View v, int position, long id)
	{
		if(position >= 0) {
			// extract item at position of click
			HistoryItem item = getItem(position);

			// fire onClick event to HistoryListListeners
			for(HistoryListClickListener l : listeners) {
				l.onClick(item);
			}
		}
	}

	public HistoryItem getItem(int position)
	{
		this.cursor.moveToPosition(position);
		return getItem(this.cursor);
	}

	public static HistoryItem getItem(Cursor cursor)
	{
		return new HistoryItem(
			cursor.getInt(cursor.getColumnIndex(History.Items._ID)),
			cursor.getString(cursor.getColumnIndex(History.Items.TITLE)),
			cursor.getString(cursor.getColumnIndex(History.Items.MAC)),
			cursor.getString(cursor.getColumnIndex(History.Items.IP)),
			cursor.getInt(cursor.getColumnIndex(History.Items.PORT)),
			cursor.getInt(cursor.getColumnIndex(History.Items.WIDGET_ID))
		);
	}

	public void addToHistory(String title, String mac, String ip, int port)
	{
		boolean exists = false;

		// don't allow duplicates in history list
		if(cursor.moveToFirst()) {
			int macColumn = cursor.getColumnIndex(History.Items.MAC);
			int ipColumn = cursor.getColumnIndex(History.Items.IP);
			int portColumn = cursor.getColumnIndex(History.Items.PORT);

			do {
				if(mac.equals(cursor.getString(macColumn)) && ip.equals(cursor.getString(ipColumn)) && (port == cursor.getInt(portColumn))) {
					exists = true;
					break;
				}
			} while (cursor.moveToNext());
		}

		// create only if the item doesn't exist
		if(exists == false) {
			ContentValues values = new ContentValues(4);
			values.put(History.Items.TITLE, title);
			values.put(History.Items.MAC, mac);
			values.put(History.Items.IP, ip);
			values.put(History.Items.PORT, port);
			this.parent.getContentResolver().insert(History.Items.CONTENT_URI, values);
		}
	}

	public void updateHistory(int id, String title, String mac, String ip, int port)
	{
		ContentValues values = new ContentValues(4);
		values.put(History.Items.TITLE, title);
		values.put(History.Items.MAC, mac);
		values.put(History.Items.IP, ip);
		values.put(History.Items.PORT, port);

		Uri itemUri = Uri.withAppendedPath(History.Items.CONTENT_URI, Integer.toString(id));
		this.parent.getContentResolver().update(itemUri, values, null, null);
	}

	public void incrementHistory(long id)
	{
		int usedCount = cursor.getInt(cursor.getColumnIndex(History.Items.USED_COUNT));

		ContentValues values = new ContentValues(2);
		values.put(History.Items.USED_COUNT, usedCount+1);
		values.put(History.Items.LAST_USED_DATE, Long.valueOf(System.currentTimeMillis()));

		Uri itemUri = Uri.withAppendedPath(History.Items.CONTENT_URI, Long.toString(id));
		this.parent.getContentResolver().update(itemUri, values, null, null);
	}

	public void deleteHistory(int id)
	{
		// use HistoryProvider to remove this row
		Uri itemUri = Uri.withAppendedPath(History.Items.CONTENT_URI, Integer.toString(id));
		this.parent.getContentResolver().delete(itemUri, null, null);
	}

	public void addHistoryListClickListener(HistoryListClickListener l)
	{
		this.listeners.add(l);
	}

	public void removeHistoryListClickListener(HistoryListClickListener l)
	{
		this.listeners.remove(l);
	}

}
