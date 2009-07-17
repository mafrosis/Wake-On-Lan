package net.mafro.android.widget;

import android.os.Bundle;

import android.content.Context;
import android.content.ContentResolver;

import android.database.Cursor;

import android.util.Log;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import android.widget.TextView;
import android.widget.AdapterView;
import android.widget.ResourceCursorAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import net.mafro.android.wakeonlan.WakeOnLan;
import net.mafro.android.wakeonlan.History;
import net.mafro.android.wakeonlan.R;


public class HistoryListItemAdapter extends ResourceCursorAdapter implements OnCheckedChangeListener
{

	private static final String TAG = "HistoryListItemAdapter";

	private Context context;
	private ContentResolver content;


	public HistoryListItemAdapter(Context context, Cursor cursor)
	{
		super(context, R.layout.history_row, cursor);
		this.context = context;
		this.content = context.getContentResolver();
	}


	@Override
	public void bindView(View view, Context context, Cursor cursor)
	{
		//load our column indexes
		int idColumn = cursor.getColumnIndex(History.Items._ID);
		int titleColumn = cursor.getColumnIndex(History.Items.TITLE);
		int macColumn = cursor.getColumnIndex(History.Items.MAC);
		int ipColumn = cursor.getColumnIndex(History.Items.IP);
		int portColumn = cursor.getColumnIndex(History.Items.PORT);

		Log.d(TAG+":bindView", Integer.toString(cursor.getInt(idColumn)));

		TextView vtitle = (TextView) view.findViewById(R.id.history_row_title);
		TextView vmac = (TextView) view.findViewById(R.id.history_row_mac);
		TextView vip = (TextView) view.findViewById(R.id.history_row_ip);
		TextView vport = (TextView) view.findViewById(R.id.history_row_port);
		StarButton star = (StarButton) view.findViewById(R.id.history_row_star);

		//bind the cursor data to the form items
		vtitle.setText(cursor.getString(titleColumn));
		vmac.setText(cursor.getString(macColumn));
		vip.setText(cursor.getString(ipColumn));
		vport.setText(Integer.toString(cursor.getInt(portColumn)));

		star.setTag(cursor.getInt(idColumn));

		//add event listener to star button
		star.setOnCheckedChangeListener(this);
	}

	
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
	{
		//extract tag getting items _ID
		Log.d(TAG+":bindView", "================== "+((StarButton) buttonView).getTag());

		if(isChecked) {
			Log.d(TAG+":bindView", "true");
		}else{
			Log.d(TAG+":bindView", "false");
		}

		//change UI on button
		((StarButton) buttonView).render();
	}

}