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
public class MonitorTbl implements BaseColumns {
	/** table name */
	public static final String TBL_NAME = "MonitorTbl";
	/** Number of cols in the table */
	public static final int NUM_COLS = 8;
	/** column to hold AWS username */
	public static final String COL_USERNAME = "username";
	/** column to hold the AWS ID of the resource to be monitored (instance or vol id) */
	public static final String COL_AWSID = "awsid";
	/** column to hold the AWS region for the resource */
	public static final String COL_REGION = "region";
	/** column to hold the default measurename for this resource */
	public static final String COL_DEFAULTMEASURENAME = "defaultmeasurename";
	/** column to hold the default duration for this resource */
	public static final String COL_DEFAULTDURATION = "defaultduration";
	/** column to hold the default period */
	public static final String COL_PERIOD = "period";
	/** column to hold the default namespace */
	public static final String COL_NAMESPACE = "namespace";
	/** column to hold information on whether this resource should be added to the watch list */
	public static final String COL_WATCH = "watch";
	/** column to hold the resource type (instance or vol) */
	public static final String COL_RESTYPE = "restype";	
	/** Foreign key references */	
	public static final String FOREIGN_KEY_USERNAME = "Foreign Key (" + COL_USERNAME +  ") " +
			"references " + LoginTbl.TBL_NAME + "(" + LoginTbl.COL_USERNAME + ") on delete cascade";
	public static final String FOREIGN_KEY_RESTYPE = "Foreign Key (" + COL_RESTYPE +  ") " +
	"references " + ResourceTypeTbl.TBL_NAME + "(" + ResourceTypeTbl.COL_RESTYPE + ") on delete " +
			"cascade";	
}
