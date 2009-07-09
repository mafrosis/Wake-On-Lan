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
import android.widget.CompoundButton.OnCheckedChangeListener;

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


public class WakeOnLan extends TabActivity implements OnClickListener, OnItemClickListener, OnTabChangeListener, OnFocusChangeListener, OnCheckedChangeListener
{

	private static final String TAG = "WakeOnLan";
	
    public static final int MENU_ITEM_WAKE = Menu.FIRST;
    public static final int MENU_ITEM_DELETE = Menu.FIRST + 1;

	private static final long WEEK = 604800;

	private static int _editModeID = 0;
	private static boolean typingMode = false;
	
	private Cursor cursor;	//main history cursor

    private static final String[] PROJECTION = new String[]
	{
		History.Items._ID,
		History.Items.TITLE,
		History.Items.MAC,
		History.Items.IP,
		History.Items.PORT
    };
	
	private static Toast notification;
	

	@Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

		//configure tabs
		TabHost th = getTabHost();

		th.addTab(th.newTabSpec("tab_history").setIndicator(getString(R.string.tab_history_en), getResources().getDrawable(R.drawable.ical)).setContent(R.id.historyview));
		th.addTab(th.newTabSpec("tab_wake").setIndicator(getString(R.string.tab_wake_en), getResources().getDrawable(R.drawable.wake)).setContent(R.id.wakeview));
		
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


		//load History list
		cursor = getContentResolver().query(History.Items.CONTENT_URI, PROJECTION, null, null, null);

		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.history_row, cursor, new String[] { History.Items.TITLE, History.Items.MAC, History.Items.IP, History.Items.PORT }, new int[] { R.id.history_row_title, R.id.history_row_mac, R.id.history_row_ip, R.id.history_row_port });

		ListView lvHistory = (ListView)findViewById(R.id.history);
		lvHistory.setAdapter(adapter);

		//register self as listener for item clicks
		lvHistory.setOnItemClickListener(this);
		
		//set self as context menu listener
		registerForContextMenu(lvHistory);


		//check for updates
		SharedPreferences settings = getSharedPreferences(TAG, 0);
		long last_update = settings.getLong("last_update", 0);
		long now = System.currentTimeMillis();
		
		//Log.i(TAG+"Update", Long.toString(last_update));
		//Log.i(TAG+"Update", Long.toString(now-WEEK));
		
		if((last_update == 0) || (last_update < now-WEEK)) {
			Updater.checkForUpdates(this, handler);
			
			SharedPreferences.Editor editor = settings.edit();
			editor.putLong("last_update", now);
			editor.commit();
		}
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
				port = Integer.valueOf(vport.getText().toString());
			}

			//check for edit mode - no send of packet
			if(_editModeID == 0) {
				//send the magic packet
				String formattedMac = sendPacket(title, mac, ip, port);

				//on succesful send, add to history list
				if(formattedMac != null) {
					addToHistory(title, formattedMac, ip, port, false);
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

				//remove the previous item
				Uri itemUri = Uri.withAppendedPath(History.Items.CONTENT_URI, Integer.toString(_editModeID));
				getContentResolver().delete(itemUri, null, null);
				_editModeID = 0;

				//ensure our edit is created, even if it duplicates another entry
				addToHistory(title, formattedMac, ip, port, true);
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

			sendPacket(cursor.getString(titleColumn), cursor.getString(macColumn), cursor.getString(ipColumn), cursor.getInt(portColumn));
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
				sendWake.setText(R.string.button_wake_en);
				Button clearWake = (Button)findViewById(R.id.clear_wake);
				clearWake.setText(R.string.button_clear_en);
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
				vmac.setError(getString(R.string.invalid_mac_en));
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
			notifyUser(getString(R.string.send_failed_en)+":\n"+iae.getMessage(), WakeOnLan.this);
			return null;
			
		}catch(Exception e) {
			//Log.e(TAG, "Sending Failed", e);
			notifyUser(getString(R.string.send_failed_en), WakeOnLan.this);
			return null;
		}
		
		notifyUser(getString(R.string.packet_sent_en)+" to "+title, WakeOnLan.this);
		return formattedMac;
	}
	
	private void addToHistory(String title, String mac, String ip, int port, boolean force)
	{
		boolean create = force;

		if(force == false) {
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
			if(exists == false) { create = true; }
		}
				
		if(create == true) {
			ContentValues values = new ContentValues(4);
			values.put(History.Items.TITLE, title);
			values.put(History.Items.MAC, mac);
			values.put(History.Items.IP, ip);
			values.put(History.Items.PORT, port);
			getContentResolver().insert(History.Items.CONTENT_URI, values);
		}
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
			sendPacket(cursor.getString(titleColumn), cursor.getString(macColumn), cursor.getString(ipColumn), cursor.getInt(portColumn));
			return true;
			
		case R.id.menu_edit:
			//fire this record into edit mode in the next tab
			EditText vtitle = (EditText)findViewById(R.id.title);
			EditText vmac = (EditText)findViewById(R.id.mac);
			EditText vip = (EditText)findViewById(R.id.ip);
			EditText vport = (EditText)findViewById(R.id.port);

			//save the id of record being edited - delete it on save and create new
			_editModeID = cursor.getInt(idColumn);
			
			//display editing data
			vtitle.setText(cursor.getString(titleColumn));
			vmac.setText(cursor.getString(macColumn));
			vip.setText(cursor.getString(ipColumn));
			vport.setText(cursor.getString(portColumn));

			//change text on both our button's
			Button saveEdit = (Button)findViewById(R.id.send_wake);
			saveEdit.setText(R.string.button_save_en);
			Button cancelEdit = (Button)findViewById(R.id.clear_wake);
			cancelEdit.setText(R.string.button_cancel_en);
			
			TabHost th = getTabHost();
			th.setCurrentTab(1);
			return true;
			
		case R.id.menu_delete:
			//use HistoryProvider to remove this row
			Uri itemUri = Uri.withAppendedPath(History.Items.CONTENT_URI, Integer.toString(cursor.getInt(idColumn)));
			getContentResolver().delete(itemUri, null, null);
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


	private Handler handler = new Handler()
	{
		@Override
		public void handleMessage(Message msg) {
			//prompt user for action
			new AlertDialog.Builder(WakeOnLan.this)
				.setTitle(getString(R.string.update_available_en))
				.setMessage(getString(R.string.install_latest_question_en))
				.setIcon(R.drawable.icon)
				.setPositiveButton(R.string.yes_en, new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int whichButton) {
						//if version numbers don't match then open Market application
						Intent market = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.market_url)+getPackageName()));
						startActivity(market);
					}

			}).setNegativeButton(R.string.no_en, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {}
			}).show();
		}
	};

}