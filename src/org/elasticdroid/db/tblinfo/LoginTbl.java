/**
 *  This file is part of ElasticDroid.
 *
 * ElasticDroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * ElasticDroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with ElasticDroid.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Authored by Siddhu Warrier on 4 Nov 2010
 */
package org.elasticdroid.db.tblinfo;

import android.provider.BaseColumns;

/**
 * @author Siddhu Warrier
 *
 * 4 Nov 2010
 */
public class LoginTbl implements BaseColumns {
	public static final String TBL_NAME = "LoginTbl";
	public static final int NUM_COLS = 3; 
	public static final String COL_USERNAME = "username";
	public static final String COL_ACCESSKEY = "accesskey";
	public static final String COL_SECRETACCESSKEY="secretaccesskey";
}
