/*
Copyright (C) 2008-2012 Matt Black.
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

import android.net.Uri;
import android.provider.BaseColumns;


/**
 *	@desc	convenience definitions for HistoryProvider
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
