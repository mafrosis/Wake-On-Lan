package net.mafro.android.wakeonlan;

import android.net.Uri;
import android.provider.BaseColumns;

/*
 *	convenience definitions for HistoryProvider
 */
public final class History {
	public static final String AUTHORITY = "net.mafro.android.wakeonlan.historyprovider";

	private History() {}

	public static final class Items implements BaseColumns {
		private Items() {}

		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/history");

		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.mafro.wakeonlan.history";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.mafro.wakeonlan.history";

		public static final String DEFAULT_SORT_ORDER = "last_used DESC";

		public static final String TITLE = "title";
		public static final String MAC = "mac";
		public static final String IP = "ip";
		public static final String PORT = "port";
		public static final String CREATED_DATE = "created";
		public static final String LAST_USED_DATE = "last_used";
		public static final String USED_COUNT = "used_count";
		public static final String IS_STARRED = "is_starred";
	}
}