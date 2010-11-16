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
 * Class to hold metadata for the table LoginTbl in the database
 * {@link org.elasticdroid.db.ElasticDroidDB.#DATABASE_NAME}.
 * @author Siddhu Warrier
 *
 * 4 Nov 2010
 */
public class LoginTbl implements BaseColumns {
	/** table name */
	public static final String TBL_NAME = "LoginTbl";
	/** Number of cols in the table */
	public static final int NUM_COLS = 3;
	/** column to hold AWS username */
	public static final String COL_USERNAME = "username";
	/** column to hold associated AWS access key */
	public static final String COL_ACCESSKEY = "accesskey";
	/** column to hold associated AWS secret access key */
	public static final String COL_SECRETACCESSKEY = "secretaccesskey";
	/** column to hold the default region */
	public static final String COL_DEFAULTREGION = "defaultregion";
}
