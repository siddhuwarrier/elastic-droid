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
 * Authored by siddhu on 4 Jan 2011
 */
package org.elasticdroid.db.tblinfo;

import android.provider.BaseColumns;

/**
 * @author siddhu
 *
 * 4 Jan 2011
 */
public class ResourceTypeTbl implements BaseColumns {
	/** table name */
	public static final String TBL_NAME = "ResourceTypeTbl";
	/** Number of cols in the table */
	public static final int NUM_COLS = 2;
	/** column to hold the type of the resource */
	public static final String COL_RESTYPE = "restype";
	/** column to hold the name of the resource type (our name, not necessarily AWS's name) */
	public static final String COL_RESNAME = "resname";
}
