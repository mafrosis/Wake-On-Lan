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

		Log.i(TAG, url);

		//retrieve current latest version number
		HttpClient hc = new DefaultHttpClient();
		HttpGet req = new HttpGet(url);
		ResponseHandler<String> rh = new BasicResponseHandler();

		String response = null;

		try{
			response = hc.execute(req, rh);
		}catch(IOException ioe) {
			return;
		}

		String currentVersion = getVersionNumber();

		Log.i(TAG, currentVersion);
		Log.i(TAG, response);

		//compare version numbers
		if(!currentVersion.equals(response)) {
			handler.sendEmptyMessage(0);
		}

		Log.i(TAG, "finished checking version");
		running = false;
	}


	public String getVersionNumber() {
		String version = "?";
		try {
			PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			version = pi.versionName;
		} catch (NameNotFoundException e) {
			Log.e(TAG, "Package name not found", e);
		};
		return version;
	}

}