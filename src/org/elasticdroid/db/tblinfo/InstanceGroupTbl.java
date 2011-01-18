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
 * Authored by Rodolfo Cartas on 15 Ene 2011
 */
package org.elasticdroid.db.tblinfo;

import android.provider.BaseColumns;

/**
 * Class to hold metadata for the table InstanceGroupTbl in the database
 * {@link org.elasticdroid.db.ElasticDroidDB.#DATABASE_NAME}.
 * @author Rodolfo Cartas
 *
 * 15 Ene 2011
 */
public class InstanceGroupTbl implements BaseColumns {
	/** table name */
	public static final String TBL_NAME = "InstanceGroupTbl";
	/** Number of cols in the table */
	public static final int NUM_COLS = 3;
	/** column to hold AWS username */
	public static final String COL_USERNAME = "username";
	/** column to hold the AWS region for the resource */
	public static final String COL_REGION = "region";
	/** column to hold the Instance Group Name */
	public static final String COL_GROUP_NAME = "groupname";
	/** Foreign key references */	
	public static final String FOREIGN_KEY_USERNAME = "Foreign Key (" + COL_USERNAME +  ") " +
			"references " + LoginTbl.TBL_NAME + "(" + LoginTbl.COL_USERNAME + ") on delete cascade";

}
