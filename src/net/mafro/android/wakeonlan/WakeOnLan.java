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

import android.app.TabActivity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.content.Context;
import android.content.UriMatcher;
import android.content.ContentValues;
import android.content.Intent;
import android.content.DialogInterface;
import android.content.SharedPreferences;

import android.database.Cursor;

import android.util.Log;

import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;

import android.net.Uri;

import android.provider.BaseColumns;


public class WakeOnLan extends TabActivity implements OnClickListener, OnItemClickListener, OnTabChangeListener, OnFocusChangeListener
{

	public static final String TAG = "WakeOnLan";
	
    public static final int MENU_ITEM_WAKE = Menu.FIRST;
    public static final int MENU_ITEM_DELETE = Menu.FIRST + 1;

	private static int _editModeID = 0;
	private static boolean typingMode = false;
	
	private Cursor cursor;
	private HistoryListItemAdapter adapter;

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

	private static final int CREATED = 0;
	private static final int LAST_USED = 1;
	private static final int USED_COUNT = 2;
	private static int sort_mode;
	
	private static Toast notification;
	

	@Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

		//configure tabs
		TabHost th = getTabHost();

		th.addTab(th.newTabSpec("tab_history").setIndicator(getString(R.string.tab_history), getResources().getDrawable(R.drawable.ical)).setContent(R.id.historyview));
		th.addTab(th.newTabSpec("tab_wake").setIndicator(getString(R.string.tab_wake), getResources().getDrawable(R.drawable.wake)).setContent(R.id.wakeview));
		
		th.setCurrentTab(0);

		//register self as tab changed listener
		th.setOnTabChangedListener(this);


		//set defaults on Wake tab
		EditText vip = (EditText)findViewById(R.id.ip);
		vip.setText(MagicPacket.BROADCAST);
		EditText vport = (EditText)findViewById(R.id.port);
		vport.setText(Integer.toString(MagicPacket.PORT));


		//register self as listener for wake button
		Button sendWake = (Button)findViewById(R.id.send_wake);
		sendWake.setOnClickListener(this);
		Button clearWake = (Button)findViewById(R.id.clear_wake);
		clearWake.setOnClickListener(this);

		//register self as mac address field focus change listener
		EditText vmac = (EditText)findViewById(R.id.mac);
		vmac.setOnFocusChangeListener(this);


		//preferences
		SharedPreferences settings = getSharedPreferences(TAG, 0);
		SharedPreferences.Editor editor;
		
		// clean up old preferences
		if(settings.contains("check_for_update") == true) {
			editor = settings.edit();
			editor.remove("check_for_update");
			editor.remove("last_update");
			editor.commit();
		}

		//load our sort mode
		sort_mode = settings.getInt("sort_mode", CREATED);


		//load cursor, bind to history adapter, set listeners
		bindHistoryList(sort_mode);
    }


	private void bindHistoryList(int sort_mode)
	{
		String orderBy = null;
		switch (sort_mode) {
		case CREATED:
			orderBy = History.Items.IS_STARRED+" DESC, "+History.Items.CREATED_DATE+" DESC";
			break;
		case LAST_USED:
			orderBy = History.Items.IS_STARRED+" DESC, "+History.Items.LAST_USED_DATE+" DESC";
			break;
		case USED_COUNT:
			orderBy = History.Items.IS_STARRED+" DESC, "+History.Items.USED_COUNT+" DESC";
			break;
		}

		//load History list
		cursor = getContentResolver().query(History.Items.CONTENT_URI, PROJECTION, null, null, orderBy);
		adapter = new HistoryListItemAdapter(this, cursor);

		ListView lvHistory = (ListView)findViewById(R.id.history);
		lvHistory.setAdapter(adapter);

		//register self as listener for item clicks
		lvHistory.setOnItemClickListener(this);

		//set self as context menu listener
		registerForContextMenu(lvHistory);
	}


	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		
		MenuItem mi = null;

		switch (sort_mode) {
		case CREATED:
			mi = (MenuItem) menu.findItem(R.id.menu_created);
			break;
		case LAST_USED:
			mi = (MenuItem) menu.findItem(R.id.menu_lastused);
			break;
		case USED_COUNT:
			mi = (MenuItem) menu.findItem(R.id.menu_usedcount);
			break;
		}

		//toggle menuitem
		mi.setChecked(true);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem mi)
	{
		switch (mi.getItemId()) {
		case R.id.menu_created:
			sort_mode = CREATED;
			break;
		case R.id.menu_lastused:
			sort_mode = LAST_USED;
			break;
		case R.id.menu_usedcount:
			sort_mode = USED_COUNT;
			break;
		case R.id.menu_sortby:
			return false;
		}

		//toggle menuitem
		mi.setChecked(true);

		//save to preferences
		SharedPreferences.Editor editor = getSharedPreferences(TAG, 0).edit();
		editor.putInt("sort_mode", sort_mode);
		editor.commit();

		//rebind the history list
		bindHistoryList(sort_mode);
		return true;
	}


	public void onClick(View v)
	{
		if(v.getId() == R.id.send_wake) {
			EditText vtitle = (EditText)findViewById(R.id.title);
			EditText vmac = (EditText)findViewById(R.id.mac);
			EditText vip = (EditText)findViewById(R.id.ip);
			EditText vport = (EditText)findViewById(R.id.port);
			
			String title = vtitle.getText().toString();
			String mac = vmac.getText().toString();

			//default IP and port unless set on form
			String ip = MagicPacket.BROADCAST;
			if(!vip.getText().toString().equals("")) {
				ip = vip.getText().toString();
			}

			int port = MagicPacket.PORT;
			if(!vport.getText().toString().equals("")) {
				try {
					port = Integer.valueOf(vport.getText().toString());
				}catch(NumberFormatException nfe) {
					notifyUser("Bad port number", WakeOnLan.this);
					return;
				}
			}

			//check for edit mode - no send of packet
			if(_editModeID == 0) {
				//send the magic packet
				String formattedMac = sendPacket(title, mac, ip, port);

				//on successful send, add to history list
				if(formattedMac != null) {
					addToHistory(title, formattedMac, ip, port);
				}else{
					//return on sending failed
					return;
				}

			}else{
				String formattedMac = null;

				try {
					//validate and clean our mac address
					formattedMac = MagicPacket.cleanMac(mac);

				}catch(IllegalArgumentException iae) {
					//Log.e(TAG, iae.getMessage(), iae);
					notifyUser(iae.getMessage(), WakeOnLan.this);
					return;
				}

				//update existing history entry
				updateHistory(_editModeID, title, formattedMac, ip, port);

				//reset now edit mode complete
				_editModeID = 0;
			}

			//finished typing (either send or edit)
			typingMode = false;

			//switch back to the history tab
			getTabHost().setCurrentTab(0);

		}else if(v.getId() == R.id.clear_wake) {
			if(_editModeID == 0) {
				//clear the form
				EditText vtitle = (EditText)findViewById(R.id.title);
				vtitle.setText(null);
				EditText vmac = (EditText)findViewById(R.id.mac);
				vmac.setText(null);
				vmac.setError(null);
				EditText vip = (EditText)findViewById(R.id.ip);
				vip.setText(null);
				EditText vport = (EditText)findViewById(R.id.port);
				vport.setText(null);
			}else{
				//cancel editing
				_editModeID = 0;
				typingMode = false;

				//switch back to the history tab
				getTabHost().setCurrentTab(0);
			}
		}
	}

	public void onItemClick(AdapterView parent, View v, int position, long id)
	{
		if(position >= 0) {
			//extract position of clicked item
            Cursor cursor = (Cursor) parent.getItemAtPosition(position);

			int titleColumn = cursor.getColumnIndex(History.Items.TITLE);
			int macColumn = cursor.getColumnIndex(History.Items.MAC);
			int ipColumn = cursor.getColumnIndex(History.Items.IP);
			int portColumn = cursor.getColumnIndex(History.Items.PORT);

			String mac = sendPacket(cursor.getString(titleColumn), cursor.getString(macColumn), cursor.getString(ipColumn), cursor.getInt(portColumn));

			//update used count in DB
			if(mac != null) {
				incrementHistory(id);
			}
        }
	}

	public void onTabChanged(String tabId)
	{
		if(tabId.equals("tab_wake")) {
			//enter typing mode - no clear of form until exit typing mode
			typingMode = true;

		}else if(tabId.equals("tab_history")) {
			//set form back to defaults, if typing mode has ended (button was clicked)
			if(typingMode == false) {
				EditText vtitle = (EditText)findViewById(R.id.title);
				EditText vmac = (EditText)findViewById(R.id.mac);
				EditText vip = (EditText)findViewById(R.id.ip);
				EditText vport = (EditText)findViewById(R.id.port);

				vtitle.setText(null);
				vmac.setText(null);
				vip.setText(MagicPacket.BROADCAST);
				vport.setText(Integer.toString(MagicPacket.PORT));
				
				//clear any errors
				vmac.setError(null);

				//reset both our button's text
				Button sendWake = (Button)findViewById(R.id.send_wake);
				sendWake.setText(R.string.button_wake);
				Button clearWake = (Button)findViewById(R.id.clear_wake);
				clearWake.setText(R.string.button_clear);
			}
		}
	}

	public void onFocusChange(View v, boolean hasFocus)
	{
		//validate mac address on field exit
		if(hasFocus == false) {
			EditText vmac = (EditText)v;
			
			try {
				//validate our mac address
				String mac = vmac.getText().toString();
				if(mac.length() > 0) {
					mac = MagicPacket.cleanMac(mac);
					vmac.setText(mac);
				}
				vmac.setError(null);

			}catch(IllegalArgumentException iae) {
				vmac.setError(getString(R.string.invalid_mac));
			}
		}
	}


	private String sendPacket(String title, String mac, String ip, int port)
	{
		//Log.i(TAG, mac+" "+ip+":"+Integer.toString(port));
		String formattedMac = null;
		
		try {
			formattedMac = MagicPacket.send(mac, ip, port);
			
		}catch(IllegalArgumentException iae) {
			//Log.e(TAG, "Sending Failed", iae);
			notifyUser(getString(R.string.send_failed)+":\n"+iae.getMessage(), WakeOnLan.this);
			return null;
			
		}catch(Exception e) {
			//Log.e(TAG, "Sending Failed", e);
			notifyUser(getString(R.string.send_failed), WakeOnLan.this);
			return null;
		}
		
		//display sent message to user
		notifyUser(getString(R.string.packet_sent)+" to "+title, WakeOnLan.this);
		return formattedMac;
	}


	private void addToHistory(String title, String mac, String ip, int port)
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
			getContentResolver().insert(History.Items.CONTENT_URI, values);
		}
	}

	private void updateHistory(int id, String title, String mac, String ip, int port)
	{
		ContentValues values = new ContentValues(4);
		values.put(History.Items.TITLE, title);
		values.put(History.Items.MAC, mac);
		values.put(History.Items.IP, ip);
		values.put(History.Items.PORT, port);
		
		Uri itemUri = Uri.withAppendedPath(History.Items.CONTENT_URI, Integer.toString(id));
		getContentResolver().update(itemUri, values, null, null);
	}

	private void incrementHistory(long id)
	{
		int usedCountColumn = cursor.getColumnIndex(History.Items.USED_COUNT);
		int usedCount = cursor.getInt(usedCountColumn);
		
		ContentValues values = new ContentValues(1);
		values.put(History.Items.USED_COUNT, usedCount+1);
		values.put(History.Items.LAST_USED_DATE, Long.valueOf(System.currentTimeMillis()));
		
		Uri itemUri = Uri.withAppendedPath(History.Items.CONTENT_URI, Long.toString(id));
		getContentResolver().update(itemUri, values, null, null);
	}

	private void deleteHistory(int id)
	{
		//use HistoryProvider to remove this row
		Uri itemUri = Uri.withAppendedPath(History.Items.CONTENT_URI, Integer.toString(id));
		getContentResolver().delete(itemUri, null, null);
	}


	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.history_menu, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		//extract data about clicked item
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		
		//move bound cursor to item that was clicked
		cursor.moveToPosition(info.position);
		
		int idColumn = cursor.getColumnIndex(History.Items._ID);
		int titleColumn = cursor.getColumnIndex(History.Items.TITLE);
		int macColumn = cursor.getColumnIndex(History.Items.MAC);
		int ipColumn = cursor.getColumnIndex(History.Items.IP);
		int portColumn = cursor.getColumnIndex(History.Items.PORT);
		
		switch (item.getItemId()) {
		case R.id.menu_wake:
			String mac = sendPacket(cursor.getString(titleColumn), cursor.getString(macColumn), cursor.getString(ipColumn), cursor.getInt(portColumn));
			
			//update used count in DB
			if(mac != null) {
				incrementHistory(cursor.getInt(idColumn));
			}
			return true;
			
		case R.id.menu_edit:
			//fire this record into edit mode in the next tab
			EditText vtitle = (EditText)findViewById(R.id.title);
			EditText vmac = (EditText)findViewById(R.id.mac);
			EditText vip = (EditText)findViewById(R.id.ip);
			EditText vport = (EditText)findViewById(R.id.port);

			//save the id of record being edited
			_editModeID = cursor.getInt(idColumn);
			
			//display editing data
			vtitle.setText(cursor.getString(titleColumn));
			vmac.setText(cursor.getString(macColumn));
			vip.setText(cursor.getString(ipColumn));
			vport.setText(cursor.getString(portColumn));

			//clear any previous errors
			vmac.setError(null);

			//change text on both our buttons
			Button saveEdit = (Button)findViewById(R.id.send_wake);
			saveEdit.setText(R.string.button_save);
			Button cancelEdit = (Button)findViewById(R.id.clear_wake);
			cancelEdit.setText(R.string.button_cancel);
			
			TabHost th = getTabHost();
			th.setCurrentTab(1);
			return true;
			
		case R.id.menu_delete:
			deleteHistory(cursor.getInt(idColumn));
			return true;

		default:
			return super.onContextItemSelected(item);
		}
	}


	public static void notifyUser(String message, Context context)
	{
		if (notification != null) {
			notification.setText(message);
			notification.show();
		} else {
			notification = Toast.makeText(context, message, Toast.LENGTH_SHORT);
			notification.show();
		}
	}

}
