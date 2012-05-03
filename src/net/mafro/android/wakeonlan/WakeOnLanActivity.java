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

import android.app.Activity;
import android.app.LocalActivityManager;

import android.os.Bundle;

import android.content.Context;
import android.content.UriMatcher;
import android.content.ContentValues;
import android.content.SharedPreferences;

import android.util.Log;

import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;


/**
 *	@desc	Base activity, handles all UI events except history ListView clicks
 */
public class WakeOnLanActivity extends Activity implements OnClickListener, OnTabChangeListener, OnFocusChangeListener
{

	public static final String TAG = "WakeOnLan";

    public static final int MENU_ITEM_WAKE = Menu.FIRST;
    public static final int MENU_ITEM_DELETE = Menu.FIRST + 1;

	private static int _editModeID = 0;
	private static boolean typingMode = false;

	private static boolean isTablet = false;
	private TabHost th;

	private HistoryListHandler histHandler;
	private static int sort_mode;

	public static final int CREATED = 0;
	public static final int LAST_USED = 1;
	public static final int USED_COUNT = 2;

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

	private static Toast notification;


	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		//configure tabs
        th = (TabHost)findViewById(R.id.tabhost);

		// tabs only exist in phone layouts
		if(th != null) {
			WakeOnLanActivity.isTablet = true;

			LocalActivityManager lam = new LocalActivityManager(this, false);
			//lam.dispatchCreate(savedInstanceState);
			th.setup(lam);

			th.addTab(th.newTabSpec("tab_history").setIndicator(getString(R.string.tab_history), getResources().getDrawable(R.drawable.ical)).setContent(R.id.historyview));
			th.addTab(th.newTabSpec("tab_wake").setIndicator(getString(R.string.tab_wake), getResources().getDrawable(R.drawable.wake)).setContent(R.id.wakeview));

			th.setCurrentTab(0);

			//register self as tab changed listener
			th.setOnTabChangedListener(this);
		}else{
			//set the background colour of the titles
        	TextView historytitle = (TextView)findViewById(R.id.historytitle);
			historytitle.setBackgroundColor(0xFF999999);
        	TextView waketitle = (TextView)findViewById(R.id.waketitle);
			waketitle.setBackgroundColor(0xFF999999);
		}

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


		// grab the history ListView
		ListView lv = (ListView)findViewById(R.id.history);

		//load history handler (deals with cursor and history ListView)
		histHandler = new HistoryListHandler(this, lv);
		histHandler.bind(sort_mode);

		//register main Activity as context menu handler
		registerForContextMenu(lv);
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
		histHandler.bind(sort_mode);
		return true;
	}


	public void onClick(View v)
	{
		if(v.getId() == R.id.send_wake) {
			EditText vtitle = (EditText)findViewById(R.id.title);
			EditText vmac = (EditText)findViewById(R.id.mac);
			EditText vip = (EditText)findViewById(R.id.ip);
			EditText vport = (EditText)findViewById(R.id.port);

			String title = vtitle.getText().toString().trim();
			String mac = vmac.getText().toString().trim();

			//default IP and port unless set on form
			String ip = MagicPacket.BROADCAST;
			if(!vip.getText().toString().trim().equals("")) {
				ip = vip.getText().toString().trim();
			}

			int port = MagicPacket.PORT;
			if(!vport.getText().toString().trim().equals("")) {
				try {
					port = Integer.valueOf(vport.getText().toString().trim());
				}catch(NumberFormatException nfe) {
					notifyUser("Bad port number", WakeOnLanActivity.this);
					return;
				}
			}

			//update form with cleaned variables
			vtitle.setText(title);
			vmac.setText(mac);
			vip.setText(ip);
			vport.setText(Integer.toString(port));

			//check for edit mode - no send of packet
			if(_editModeID == 0) {
				//send the magic packet
				String formattedMac = sendPacket(title, mac, ip, port);

				//on successful send, add to history list
				if(formattedMac != null) {
					histHandler.addToHistory(title, formattedMac, ip, port);
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
					notifyUser(iae.getMessage(), WakeOnLanActivity.this);
					return;
				}

				//update existing history entry
				histHandler.updateHistory(_editModeID, title, formattedMac, ip, port);

				//reset now edit mode complete
				_editModeID = 0;
			}

			//finished typing (either send or edit)
			typingMode = false;

			//switch back to the history tab
			if(WakeOnLanActivity.isTablet == true) {
				th.setCurrentTab(0);
			}

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
				if(WakeOnLanActivity.isTablet == true) {
					th.setCurrentTab(0);
				}
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


	public String sendPacket(HistoryItem item)
	{
		return sendPacket(item.title, item.mac, item.ip, item.port);
	}

	public String sendPacket(String title, String mac, String ip, int port)
	{
		String formattedMac = null;

		try {
			formattedMac = MagicPacket.send(mac, ip, port);

		}catch(IllegalArgumentException iae) {
			notifyUser(getString(R.string.send_failed)+":\n"+iae.getMessage(), WakeOnLanActivity.this);
			return null;

		}catch(Exception e) {
			notifyUser(getString(R.string.send_failed), WakeOnLanActivity.this);
			return null;
		}

		//display sent message to user
		notifyUser(getString(R.string.packet_sent)+" to "+title, WakeOnLanActivity.this);
		return formattedMac;
	}


	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.history_menu, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem mi)
	{
		//extract data about clicked item
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) mi.getMenuInfo();

		//extract history item
		HistoryItem item = histHandler.getItem(info.position);

		switch (mi.getItemId()) {
		case R.id.menu_wake:
			String mac = sendPacket(item);

			//update used count in DB
			if(sendPacket(item) != null) {
				histHandler.incrementHistory(item.id);
			}
			return true;

		case R.id.menu_edit:
			//save the id of record being edited
			_editModeID = item.id;

			//fire this record into edit mode in the next tab
			EditText vtitle = (EditText)findViewById(R.id.title);
			EditText vmac = (EditText)findViewById(R.id.mac);
			EditText vip = (EditText)findViewById(R.id.ip);
			EditText vport = (EditText)findViewById(R.id.port);

			//display editing data
			vtitle.setText(item.title);
			vmac.setText(item.mac);
			vip.setText(item.ip);
			vport.setText(Integer.toString(item.port));

			//clear any previous errors
			vmac.setError(null);

			//change text on both our buttons
			Button saveEdit = (Button)findViewById(R.id.send_wake);
			saveEdit.setText(R.string.button_save);
			Button cancelEdit = (Button)findViewById(R.id.clear_wake);
			cancelEdit.setText(R.string.button_cancel);

			if(WakeOnLanActivity.isTablet == true) {
				th.setCurrentTab(1);
			}
			return true;

		case R.id.menu_delete:
			histHandler.deleteHistory(item.id);
			return true;

		default:
			return super.onContextItemSelected(mi);
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
