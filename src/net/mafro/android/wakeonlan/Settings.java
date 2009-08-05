package net.mafro.android.wakeonlan;

import android.os.Bundle;

import android.preference.Preference;
import android.preference.PreferenceActivity;

import android.content.Context;

import android.util.Log;


public class Settings extends PreferenceActivity
{

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//use the application SharedPreferences store
		getPreferenceManager().setSharedPreferencesName(WakeOnLan.TAG);
		
		//inflate our layout
		addPreferencesFromResource(R.layout.settings);
	}

}