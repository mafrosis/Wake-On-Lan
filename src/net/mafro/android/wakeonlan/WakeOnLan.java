package net.mafro.android.wakeonlan;

import android.app.TabActivity;
import android.os.Bundle;

import android.content.Context;
import android.content.UriMatcher;

import android.database.Cursor;

import android.util.Log;

import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import android.view.View;
import android.view.View.OnClickListener;

import android.net.Uri;
import android.provider.BaseColumns;


public class WakeOnLan extends TabActivity implements OnClickListener, OnTabChangeListener
{
	private Cursor cursor;	//main history cursor

    private static final String[] PROJECTION = new String[]
	{
            History.Items._ID,
            History.Items.MAC,
    };

	@Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

		//configure tabs
		TabHost th = getTabHost();
		
		String tab_history = getString(R.string.tab_history_en);
		String tab_wake = getString(R.string.tab_wake_en);

		th.addTab(th.newTabSpec("tab_history").setIndicator(tab_history).setContent(R.id.history));
		th.addTab(th.newTabSpec("tab_wake").setIndicator(tab_wake).setContent(R.id.wakeview));
		
		th.setOnTabChangedListener(this);
		th.setCurrentTab(0);
		
		//load history
		
		// Perform a managed query. The Activity will handle closing and requerying the cursor when needed.
		cursor = managedQuery(History.Items.CONTENT_URI, PROJECTION, null, null, History.Items.DEFAULT_SORT_ORDER);

		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.history_row, cursor, new String[] { History.Items.MAC }, new int[] { R.id.history_row_mac });

		ListView lv = (ListView)findViewById(R.id.history);
		lv.setAdapter(adapter);
    }

	public void onTabChanged(String tabId)
	{
		Log.i("---------------- TAB", tabId);

		if(tabId.equals("tab_history")) {
			//Log.i("---------------- LIST", Integer.toString(list.length));
		}
	}
	
	public void onClick(View v)
	{
		if(v.getId() == R.id.send_wake) {
			EditText vtitle = (ListView)findViewById(R.id.title);
			EditText vmac = (ListView)findViewById(R.id.mac);
			EditText vip = (ListView)findViewById(R.id.ip);
			EditText vport = (ListView)findViewById(R.id.port);
			
			String title = vtitle.getText().getString();
			String mac = vmac.getText().getString();
			String ip = vip.getText().getString();
			int port = Integer.valueOf(vport.getText().getString());
			
			//send magic packet
			MagicPacket.send(mac, ip, port);
			
			addToHistory(title, mac, ip, port);
		}
	}
	
	private void addToHistory(String title, String mac, String ip, int port)
	{	
		boolean exists = false;
		
		//check mac doesnt already exist
		if(cursor.moveToFirst()) {
			int macColumn = cursor.getColumnIndex(History.Items.MAC);

			do {
				if(mac.equals(cursor.getString(macColumn))) {
					exists = true;
					break;
				}
			} while (cursor.moveToNext());
		}
		
		//create if not exists
		if(exists == false) {
			ContentValues values = new ContentValues(4);
			values.put(History.Items.TITLE, title);
			values.put(History.Items.MAC, mac);
			values.put(History.Items.IP, ip);
			values.put(History.Items.PORT, port);

			Uri uri = getContentResolver().insert(History.Items.CONTENT_URI, values);
		}
	}
	
}
