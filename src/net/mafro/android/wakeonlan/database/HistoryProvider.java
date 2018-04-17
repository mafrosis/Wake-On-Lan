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

package net.mafro.android.wakeonlan.database;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.res.Resources;

import android.net.Uri;

import android.util.Log;

import android.text.TextUtils;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;

import java.util.HashMap;


/**
 *	@desc	Custom ContentProvider to wrap underlying datastore
 */
public class HistoryProvider extends ContentProvider {

	private static final String TAG = "HistoryProvider";

	private static final String DATABASE_NAME = "wakeonlan_history.db";
	private static final int DATABASE_VERSION = 3;

	private static HashMap<String, String> sHistoryProjectionMap;

	private static final String HISTORY_TABLE_NAME = "history";

	private static final int HISTORY = 1;
	private static final int HISTORY_ID = 2;

	private static final UriMatcher sUriMatcher;

	/**
	 * This class helps open, create, and upgrade the database file.
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper {

		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE history ("
					+ Definitions.Items._ID + " INTEGER PRIMARY KEY,"
					+ Definitions.Items.TITLE + " TEXT,"
					+ Definitions.Items.MAC + " TEXT,"
					+ Definitions.Items.IP + " TEXT,"
					+ Definitions.Items.PORT + " INTEGER,"
					+ Definitions.Items.CREATED_DATE + " INTEGER,"
					+ Definitions.Items.LAST_USED_DATE + " INTEGER,"
					+ Definitions.Items.USED_COUNT + " INTEGER DEFAULT 1,"
					+ Definitions.Items.IS_STARRED + " INTEGER DEFAULT 0,"
					+ Definitions.Items.WIDGET_ID + " INTEGER DEFAULT 0"
					+ ");");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			if(oldVersion == 1) {
				// upgrade from v1 to v2
				db.execSQL("ALTER TABLE history ADD COLUMN " + Definitions.Items.USED_COUNT + " INTEGER DEFAULT 1;");
				db.execSQL("ALTER TABLE history ADD COLUMN " + Definitions.Items.IS_STARRED + " INTEGER DEFAULT 0;");

				// extra command for upgrade to v3
				if(newVersion == 3) {
					db.execSQL("ALTER TABLE history ADD COLUMN " + Definitions.Items.WIDGET_ID + " INTEGER DEFAULT 0;");
				}

			}else if(oldVersion == 2) {
				// upgrade to v3
				db.execSQL("ALTER TABLE history ADD COLUMN " + Definitions.Items.WIDGET_ID + " INTEGER DEFAULT 0;");
			}
		}
	}

	private DatabaseHelper mOpenHelper;

	@Override
	public boolean onCreate() {
		mOpenHelper = new DatabaseHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(HISTORY_TABLE_NAME);
		qb.setProjectionMap(sHistoryProjectionMap);

		// if no sort order is specified use the default
		String orderBy;
		if(TextUtils.isEmpty(sortOrder)) {
			orderBy = Definitions.Items.DEFAULT_SORT_ORDER;
		}else{
			orderBy = sortOrder;
		}

		// get the database and run the query
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);

		// tell the cursor what uri to watch, so it knows when its source data changes
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public String getType(Uri uri) {
		switch (sUriMatcher.match(uri)) {
			case HISTORY:
				return Definitions.Items.CONTENT_TYPE;

			case HISTORY_ID:
				return Definitions.Items.CONTENT_ITEM_TYPE;

			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		// validate the requested uri
		if(sUriMatcher.match(uri) != HISTORY) {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		ContentValues values;
		if(initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}

		Long now = Long.valueOf(System.currentTimeMillis());

		// make sure that the fields are all set
		if(values.containsKey(Definitions.Items.TITLE) == false) {
			values.put(Definitions.Items.TITLE, "");
		}
		if(values.containsKey(Definitions.Items.MAC) == false) {
			values.put(Definitions.Items.MAC, "");
		}
		if(values.containsKey(Definitions.Items.IP) == false) {
			values.put(Definitions.Items.IP, "");
		}
		if(values.containsKey(Definitions.Items.PORT) == false) {
			values.put(Definitions.Items.PORT, "");
		}
		if(values.containsKey(Definitions.Items.CREATED_DATE) == false) {
			values.put(Definitions.Items.CREATED_DATE, now);
		}
		if(values.containsKey(Definitions.Items.LAST_USED_DATE) == false) {
			values.put(Definitions.Items.LAST_USED_DATE, now);
		}

		SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		// insert record, 2nd param is NULLABLE field for if values is empty
		long rowId = db.insert(HISTORY_TABLE_NAME, Definitions.Items.MAC, values);
		if(rowId > 0) {
			Uri histUri = ContentUris.withAppendedId(Definitions.Items.CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(histUri, null);
			return histUri;
		}

		throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count;
		switch (sUriMatcher.match(uri)) {
			case HISTORY:
				count = db.delete(HISTORY_TABLE_NAME, where, whereArgs);
				break;

			case HISTORY_ID:
				String histId = uri.getPathSegments().get(1);
				count = db.delete(HISTORY_TABLE_NAME, Definitions.Items._ID + "=" + histId
						+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
				break;

			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		int count;
		switch (sUriMatcher.match(uri)) {
			case HISTORY:
				count = db.update(HISTORY_TABLE_NAME, values, where, whereArgs);
				break;

			case HISTORY_ID:
				String historyId = uri.getPathSegments().get(1);
				count = db.update(HISTORY_TABLE_NAME, values, Definitions.Items._ID + "=" + historyId + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""), whereArgs);
				break;

			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(Definitions.AUTHORITY, "history", HISTORY);
		sUriMatcher.addURI(Definitions.AUTHORITY, "history/#", HISTORY_ID);

		sHistoryProjectionMap = new HashMap<String, String>();
		sHistoryProjectionMap.put(Definitions.Items._ID, Definitions.Items._ID);
		sHistoryProjectionMap.put(Definitions.Items.TITLE, Definitions.Items.TITLE);
		sHistoryProjectionMap.put(Definitions.Items.MAC, Definitions.Items.MAC);
		sHistoryProjectionMap.put(Definitions.Items.IP, Definitions.Items.IP);
		sHistoryProjectionMap.put(Definitions.Items.PORT, Definitions.Items.PORT);
		sHistoryProjectionMap.put(Definitions.Items.CREATED_DATE, Definitions.Items.CREATED_DATE);
		sHistoryProjectionMap.put(Definitions.Items.LAST_USED_DATE, Definitions.Items.LAST_USED_DATE);
		sHistoryProjectionMap.put(Definitions.Items.USED_COUNT, Definitions.Items.USED_COUNT);
		sHistoryProjectionMap.put(Definitions.Items.IS_STARRED, Definitions.Items.IS_STARRED);
		sHistoryProjectionMap.put(Definitions.Items.WIDGET_ID, Definitions.Items.WIDGET_ID);
	}

}
