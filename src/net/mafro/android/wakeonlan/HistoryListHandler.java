/*
Copyright (C) 2008-2011 Matt Black.
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

import android.content.ContentValues;

import android.database.Cursor;

import android.util.Log;

import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import android.view.View;

import android.net.Uri;


/**
 *	@desc	Class handles all functions of the history ListView
 */
public class HistoryListHandler implements OnItemClickListener
{

	public static final String TAG = "HistoryListHandler";

	private WakeOnLanActivity wol;
	private Cursor cursor;
	private HistoryAdapter adapter;

	private static final String[] PROJECTION = new String[]
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


	public HistoryListHandler(WakeOnLanActivity wol, ListView view)
	{
		this.wol = wol;
		this.view = view;
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

		//load History cursor via custom ResourceAdapter
		cursor = wol.getContentResolver().query(History.Items.CONTENT_URI, PROJECTION, null, null, orderBy);
		adapter = new HistoryAdapter(wol, cursor);

		//register self as listener for item clicks
		view.setOnItemClickListener(this);

		// bind to the supplied view
		view.setAdapter(adapter);
	}


	public void onItemClick(AdapterView parent, View v, int position, long id)
	{
		if(position >= 0) {
			//extract item at position of click
			HistoryItem item = getItem(position);

			//update used count in DB
			if(wol.sendPacket(item) != null) {
				incrementHistory(id);
			}
		}
	}

	public HistoryItem getItem(int position)
	{
		this.cursor.moveToPosition(position);

		int idColumn = cursor.getColumnIndex(History.Items._ID);
		int titleColumn = cursor.getColumnIndex(History.Items.TITLE);
		int macColumn = cursor.getColumnIndex(History.Items.MAC);
		int ipColumn = cursor.getColumnIndex(History.Items.IP);
		int portColumn = cursor.getColumnIndex(History.Items.PORT);

		return new HistoryItem(cursor.getInt(idColumn), cursor.getString(titleColumn), cursor.getString(macColumn), cursor.getString(ipColumn), cursor.getInt(portColumn));
	}


	public void addToHistory(String title, String mac, String ip, int port)
	{
		boolean exists = false;

		//don't allow duplicates in history list
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

		//create only if the item doesn't exist
		if(exists == false) {
			ContentValues values = new ContentValues(4);
			values.put(History.Items.TITLE, title);
			values.put(History.Items.MAC, mac);
			values.put(History.Items.IP, ip);
			values.put(History.Items.PORT, port);
			wol.getContentResolver().insert(History.Items.CONTENT_URI, values);
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
		wol.getContentResolver().update(itemUri, values, null, null);
	}

	public void incrementHistory(long id)
	{
		int usedCountColumn = cursor.getColumnIndex(History.Items.USED_COUNT);
		int usedCount = cursor.getInt(usedCountColumn);

		ContentValues values = new ContentValues(1);
		values.put(History.Items.USED_COUNT, usedCount+1);
		values.put(History.Items.LAST_USED_DATE, Long.valueOf(System.currentTimeMillis()));

		Uri itemUri = Uri.withAppendedPath(History.Items.CONTENT_URI, Long.toString(id));
		wol.getContentResolver().update(itemUri, values, null, null);
	}

	public void deleteHistory(int id)
	{
		//use HistoryProvider to remove this row
		Uri itemUri = Uri.withAppendedPath(History.Items.CONTENT_URI, Integer.toString(id));
		wol.getContentResolver().delete(itemUri, null, null);
	}

}
