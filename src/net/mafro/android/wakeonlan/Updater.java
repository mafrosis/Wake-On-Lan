package net.mafro.android.wakeonlan;

import android.os.Handler;

import android.util.Log;

import android.net.Uri;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;

import java.lang.Thread;
import java.lang.IllegalThreadStateException;

import org.apache.http.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;
import java.io.IOException;


public class Updater extends Thread
{

	private static final String TAG = "Updater";

	private Context context = null;
	private Handler handler = null;

	private static boolean running = false;
	private static Updater u = null;
	private Updater(Context c, Handler h) {
		context = c;
		handler = h;
	}

	public static void checkForUpdates(Context context, Handler handler) {
		if(u == null) { u = new Updater(context, handler); }
		if(running == false) {
			try{
				u.start();
			}catch(IllegalThreadStateException itse) {}
		}
	}

	public void run() {
		running = true;

		String url = context.getString(R.string.version_url)+context.getPackageName();

		//Log.i(TAG, url);

		//retrieve current latest version number
		HttpClient hc = new DefaultHttpClient();
		HttpGet req = new HttpGet(url);
		ResponseHandler<String> rh = new BasicResponseHandler();

		String response = null;

		try{
			response = hc.execute(req, rh);
		}catch(IOException ioe) {
			running = false;
			return;
		}

		String currentVersion = getVersionNumber();

		//Log.i(TAG, "local: "+currentVersion);
		//Log.i(TAG, "remote: "+response);

		//compare version numbers
		if(!currentVersion.equals(response)) {
			//parse into doubles
			double current, latest;
			
			String[] v1 = currentVersion.split("\\.");
			switch(v1.length) {
			case 1: current = Double.valueOf(v1[0]); break;
			case 2: current = Double.valueOf(v1[0]+"."+v1[1]); break;
			case 3: current = Double.valueOf(v1[0]+"."+v1[1]+v1[2]); break;
			default: current = 0;
			}
			
			String[] v2 = response.split("\\.");
			switch(v2.length) {
			case 1: latest = Double.valueOf(v2[0]); break;
			case 2: latest = Double.valueOf(v2[0]+"."+v2[1]); break;
			case 3: latest = Double.valueOf(v2[0]+"."+v2[1]+v2[2]); break;
			default: latest = 0;
			}
			
			//Log.i(TAG, Double.toString(current));
			//Log.i(TAG, Double.toString(latest));
			
			if(Double.compare(latest, current) > 0) {
				handler.sendEmptyMessage(0);
			}
		}

		//Log.i(TAG, "finished checking version");
		running = false;
	}


	public String getVersionNumber() {
		String version = "?";
		try {
			PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			version = pi.versionName;
		} catch (NameNotFoundException e) {
			//Log.e(TAG, "Package name not found", e);
		};
		return version;
	}

}