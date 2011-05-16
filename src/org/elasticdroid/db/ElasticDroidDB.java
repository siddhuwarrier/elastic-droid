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
package org.elasticdroid.db;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.elasticdroid.db.tblinfo.InstanceGroupTbl;
import org.elasticdroid.db.tblinfo.InstanceTbl;
import org.elasticdroid.db.tblinfo.LoginTbl;
import org.elasticdroid.db.tblinfo.MonitorTbl;
import org.elasticdroid.db.tblinfo.ResourceTypeTbl;
import org.elasticdroid.model.orm.InstanceGroup;
import org.elasticdroid.utils.CloudWatchInput;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

/**
 * Database class for ElasticDroid.
 * 
 * Has methods to create tables etc
 * 
 * @author Siddhu Warrier
 * 
 *         4 Nov 2010
 */
public class ElasticDroidDB extends SQLiteOpenHelper {

	/** Name of database */
	private static final String DATABASE_NAME = "elasticdroid.db";
	/** Database version */
	private static final int DATABASE_VERSION = 9;
	/** Logging tag */
	private static final String TAG = "org.elasticdroid.db.ElasticDroidDB";

	/**
	 * Initialises the superclass constructor.
	 * 
	 * @param context
	 *            the view that created this object/
	 */
	public ElasticDroidDB(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);

		// check for upgrades by getting a writable database and then close it.
		this.getWritableDatabase().close();
	}

	/**
	 * create DB tables if not exist
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.v(this.getClass().getName(), "DB onCreate()");

		// Enable foreign key constraints
		db.execSQL("PRAGMA foreign_keys=ON;");

		// execute the SQL queries to create the tables
		createLoginTbl(db);
		createResourceTypeTbl(db);
		createMonitorTbl(db);
		createInstanceGroupTbl(db);
		createInstanceTbl(db);
	}

	/**
	 * onUpgrade: to be executed when change made to DB.
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

		Log.v(TAG, "Upgrading database from v" + oldVersion + " to v"
				+ newVersion);

		switch (oldVersion) {
		case 1: // drop through
		case 2: // drop through
		case 3:
			db.execSQL("PRAGMA foreign_keys=ON;");
			db.execSQL("DROP table if exists " + ResourceTypeTbl.TBL_NAME);
			db.execSQL("DROP table if exists " + MonitorTbl.TBL_NAME);
			// recreate them properly

			createResourceTypeTbl(db);
			createMonitorTbl(db);
		case 4:
			// write in values
			ContentValues insertValues = new ContentValues();
			insertValues.put(ResourceTypeTbl.COL_RESNAME, "instance");
			db.insert(ResourceTypeTbl.TBL_NAME, ResourceTypeTbl.COL_RESNAME,
					insertValues);
			insertValues.put(ResourceTypeTbl.COL_RESNAME, "volume");
			db.insert(ResourceTypeTbl.TBL_NAME, ResourceTypeTbl.COL_RESNAME,
					insertValues);

			break;
		case 5:
			db.execSQL("Alter TABLE " + MonitorTbl.TBL_NAME + " add COLUMN "
					+ MonitorTbl.COL_WATCH + " integer not null default 0;");
		case 6:
			// drop table and recreate
			db.execSQL("DROP table if exists " + MonitorTbl.TBL_NAME);
			createMonitorTbl(db);
		case 7:
			// drop table and recreate
			db.execSQL("DROP table if exists " + MonitorTbl.TBL_NAME);
			createMonitorTbl(db);
		case 8:
			createInstanceGroupTbl(db);
			createInstanceTbl(db);
		}
	}

	private void createLoginTbl(SQLiteDatabase db) {
		db.execSQL("Create TABLE " + LoginTbl.TBL_NAME + "(" + LoginTbl._ID
				+ " integer primary key autoincrement, "
				+ LoginTbl.COL_USERNAME + " text not null UNIQUE, "
				+ LoginTbl.COL_ACCESSKEY + " text not null, "
				+ LoginTbl.COL_SECRETACCESSKEY + " text not null,"
				+ LoginTbl.COL_DEFAULTREGION + " text, " + "UNIQUE("
				+ LoginTbl.COL_ACCESSKEY + ", " + LoginTbl.COL_SECRETACCESSKEY
				+ "));");
	}

	private void createResourceTypeTbl(SQLiteDatabase db) {
		db.execSQL("Create TABLE " + ResourceTypeTbl.TBL_NAME + "("
				+ ResourceTypeTbl.COL_RESTYPE
				+ " integer primary key autoincrement, "
				+ ResourceTypeTbl.COL_RESNAME + " text not null);");
	}

	private void createInstanceGroupTbl(SQLiteDatabase db) {
		db.execSQL("Create TABLE " + InstanceGroupTbl.TBL_NAME + "("
				+ InstanceGroupTbl._ID + " integer primary key autoincrement, "
				+ InstanceGroupTbl.COL_USERNAME + " text not null, "
				+ InstanceGroupTbl.COL_REGION + " text not null, "
				+ InstanceGroupTbl.COL_GROUP_NAME + " text not null unique, "
				+ InstanceGroupTbl.FOREIGN_KEY_USERNAME + ");");
	}
	
	private void createInstanceTbl(SQLiteDatabase db) {
		db.execSQL("Create TABLE " + InstanceTbl.TBL_NAME + "("
				+ InstanceTbl._ID + " integer primary key autoincrement, "
				+ InstanceTbl.COL_INSTANCEID + " text not null unique, "
				+ InstanceTbl.COL_INSTANCEGROUPID + " integer not null, "
				+ InstanceTbl.FOREIGN_KEY_INSTANCEGROUPID + ");");
		
	}


	private void createMonitorTbl(SQLiteDatabase db) {
		db.execSQL("Create TABLE " + MonitorTbl.TBL_NAME + "(" + MonitorTbl._ID
				+ " integer primary key autoincrement, "
				+ MonitorTbl.COL_USERNAME + " text not null, "
				+ MonitorTbl.COL_AWSID + " integer not null, "
				+ MonitorTbl.COL_REGION + " text not null, "
				+ MonitorTbl.COL_RESTYPE + " integer not null, "
				+ MonitorTbl.COL_DEFAULTMEASURENAME + " text not null, "
				+ MonitorTbl.COL_DEFAULTDURATION + " integer not null, "
				+ MonitorTbl.COL_PERIOD + " integer not null, "
				+ MonitorTbl.COL_NAMESPACE + " text not null, "
				+ MonitorTbl.COL_WATCH + " integer not null, "
				+ MonitorTbl.FOREIGN_KEY_USERNAME + ", "
				+ MonitorTbl.FOREIGN_KEY_RESTYPE + ");");
	}

	/**
	 * Convenience method to get list of users in DB {@link #DATABASE_NAME}.
	 * 
	 * @return Hashtable<String, ArrayList<String>> of user data.
	 */
	public Hashtable<String, ArrayList<String>> listUserData() {
		SQLiteDatabase db = this.getReadableDatabase();
		Hashtable<String, ArrayList<String>> userData = new Hashtable<String, ArrayList<String>>();
		Cursor queryCursor;

		queryCursor = db.query(LoginTbl.TBL_NAME, new String[] {
				LoginTbl.COL_USERNAME, LoginTbl.COL_ACCESSKEY,
				LoginTbl.COL_SECRETACCESSKEY }, null, null, null, null, null);

		// loop through the query and add to hashtable. Indexing starts from 1!
		queryCursor.moveToFirst();
		while (!queryCursor.isAfterLast()) {

			userData.put(
					queryCursor.getString(0),
					new ArrayList<String>(
							Arrays.asList(new String[] {
									queryCursor.getString(1),
									queryCursor.getString(2) })));

			queryCursor.moveToNext();
		}

		queryCursor.close();
		db.close();

		return userData;
	}

	/**
	 * get the default region for the user passed as argument
	 * 
	 * @param username
	 * @return
	 * @throws SQLException
	 */
	public String getDefaultRegion(String username) throws SQLException {
		SQLiteDatabase db = this.getReadableDatabase();
		String defaultRegion = null;
		Cursor queryCursor;

		try {
			queryCursor = db.query(LoginTbl.TBL_NAME,
					new String[] { LoginTbl.COL_DEFAULTREGION },
					LoginTbl.COL_USERNAME + "= ?", new String[] { username },
					null, null, null);
			if (queryCursor.getCount() != 1) {
				throw new SQLException("No data");
			}

			queryCursor.moveToFirst();
			defaultRegion = queryCursor.getString(0);
		} catch (SQLException exception) {
			throw exception;
		} finally {
			db.close();
		}

		return defaultRegion;
	}

	/**
	 * Set the default region
	 * 
	 * @param username
	 * @param defaultRegion
	 * @throws SQLException
	 */
	public void setDefaultRegion(String username, String defaultRegion) {
		SQLiteDatabase db = this.getReadableDatabase();

		ContentValues updateValues = new ContentValues();
		updateValues.put(LoginTbl.COL_DEFAULTREGION, defaultRegion);

		try {
			db.update(LoginTbl.TBL_NAME, updateValues, LoginTbl.COL_USERNAME
					+ "=?", new String[] { username });
		} catch (Exception ignore) {
			Log.e(TAG, ignore.getMessage());
		} finally {
			db.close();
		}
	}

	/**
	 * Get the default monitoring info for this AWS resource
	 */
	public CloudWatchInput getMonitoringDefaults(String awsId, String region)
			throws SQLException {
		CloudWatchInput cloudWatchInput = null;

		SQLiteDatabase db = this.getReadableDatabase();

		Cursor queryCursor;

		try {
			queryCursor = db.query(MonitorTbl.TBL_NAME, new String[] {
					MonitorTbl.COL_DEFAULTMEASURENAME,
					MonitorTbl.COL_DEFAULTDURATION, MonitorTbl.COL_PERIOD,
					MonitorTbl.COL_NAMESPACE }, MonitorTbl.COL_AWSID + "= ?",
					new String[] { awsId }, null, null, null);
			if (queryCursor.getCount() != 1) {
				throw new SQLException("No data");
			}

			queryCursor.moveToFirst(); // move to the first position

			long duration = queryCursor.getLong(1);
			long endTime = new Date().getTime();
			long startTime = endTime - duration;

			cloudWatchInput = new CloudWatchInput(startTime, endTime,
					Integer.valueOf(queryCursor.getInt(2)),
					queryCursor.getString(0), queryCursor.getString(3),
					new ArrayList<String>(Arrays
							.asList(new String[] { "Average" })),// TODO fix
																	// this
					region);
		} catch (SQLException exception) {
			throw exception;
		} finally {
			db.close();
		}

		return cloudWatchInput;
	}

	/**
	 * Set monitoring defaults for resource awsID.
	 * 
	 * @param username
	 * @param awsId
	 * @param resName
	 * @param input
	 * @param watch
	 * @return
	 * @throws SQLException
	 */
	public long setMonitoringDefaults(String username, String awsId,
			String resName, CloudWatchInput input, boolean watch)
			throws SQLException {

		int resType; // the resource type (id in ResourceTypeTbl) for the
						// resName passed in.
		SQLiteDatabase db = this.getWritableDatabase();
		// get the res ID for this resName
		Cursor queryCursor;
		try {
			queryCursor = db.query(ResourceTypeTbl.TBL_NAME,
					new String[] { ResourceTypeTbl.COL_RESTYPE },
					ResourceTypeTbl.COL_RESNAME + "= ?",
					new String[] { resName }, null, null, null);

			if (queryCursor.getCount() != 1) {
				throw new SQLException("No data");
			}

			queryCursor.moveToFirst();
			resType = queryCursor.getInt(0);
		} catch (SQLException exception) {
			throw exception;
		}

		Log.v(TAG, "ResType: " + resType);
		// write in values
		ContentValues insertValues = new ContentValues();
		insertValues.put(MonitorTbl.COL_AWSID, awsId);
		insertValues.put(MonitorTbl.COL_REGION, input.getRegion());
		insertValues.put(MonitorTbl.COL_DEFAULTDURATION, input.getEndTime()
				- input.getStartTime());
		insertValues.put(MonitorTbl.COL_DEFAULTMEASURENAME,
				input.getMeasureName());
		insertValues.put(MonitorTbl.COL_NAMESPACE, input.getNamespace());
		insertValues.put(MonitorTbl.COL_PERIOD, input.getPeriod());
		insertValues.put(MonitorTbl.COL_RESTYPE, resType);
		insertValues.put(MonitorTbl.COL_USERNAME, username);
		insertValues.put(MonitorTbl.COL_WATCH, watch ? 1 : 0); // 1 if true, 0
																// if false cuz
																// SQLite hasn't
		// a boolean type

		long retVal = db.insert(MonitorTbl.TBL_NAME, MonitorTbl.COL_WATCH,
				insertValues); // null
		// column hack is ignored

		db.close(); // Close DB. This is, like, as important as brushing ur
					// teeth before bed, man.

		return retVal;
	}

	public int updateMonitoringDefaults(String[] columns, String[] data,
			String awsId) {
		SQLiteDatabase db = this.getReadableDatabase();
		int numAffectedRows = 0; // number of Rows affected
		ContentValues updateValues = new ContentValues();

		for (int idx = 0; idx < columns.length; idx++) {
			updateValues.put(columns[idx], data[idx]);
		}

		try {
			numAffectedRows = db.update(MonitorTbl.TBL_NAME, updateValues,
					MonitorTbl.COL_AWSID + "=?", new String[] { awsId });
		} catch (Exception ignore) {
			Log.e(TAG, ignore.getMessage());
		} finally {
			db.close();
		}

		return numAffectedRows;
	}

	/**
	 * Return the list of instances watched for this AWS username
	 * 
	 * @param username
	 *            The username to retrieve the list of usernames.
	 * @param resName
	 *            Acceptable values: instance, volume
	 * @return Hashmap of watched instances and the region of each watched
	 *         instancem indexed by the former
	 */
	public HashMap<String, String> getWatchedResources(String username,
			String resName) throws SQLException {
		HashMap<String, String> watchedResources = new HashMap<String, String>();

		SQLiteDatabase db = this.getReadableDatabase(); // handle to the DB
		Cursor resCursor; // the query cursor for resource type tbl
		Cursor monitorCursor; // query cursor for monitor tabl
		int resType; // the resource type for the resource name passed in

		// first, get the res ID for this resName
		// then get the watched instances
		try {
			resCursor = db.query(ResourceTypeTbl.TBL_NAME,
					new String[] { ResourceTypeTbl.COL_RESTYPE },
					ResourceTypeTbl.COL_RESNAME + "= ?",
					new String[] { resName }, null, null, null);

			if (resCursor.getCount() != 1) {
				throw new SQLException("No data");
			}

			resCursor.moveToFirst();
			resType = resCursor.getInt(0);

			monitorCursor = db
					.query(MonitorTbl.TBL_NAME, new String[] {
							MonitorTbl.COL_AWSID, MonitorTbl.COL_REGION },
							MonitorTbl.COL_RESTYPE + "=?" + " AND "
									+ MonitorTbl.COL_USERNAME + "=?" + " AND "
									+ MonitorTbl.COL_WATCH + " = ?",
							new String[] { String.valueOf(resType), username,
									String.valueOf(1) }, null, null, null);

			// loop through the data in the cursor, and add watched resources
			// data
			// hashmap indexed by instance ID
			while (monitorCursor.moveToNext()) {
				watchedResources.put(monitorCursor.getString(0),
						monitorCursor.getString(1));
			}
			
			monitorCursor.close();
		} catch (SQLException exception) {
			throw exception;
		} finally {
			db.close();
		}

		Log.v(TAG, "ResType: " + resType);

		return watchedResources;

	}

	/**
	 * Returns the count of instance groups for a given username and region
	 * 
	 * @param username
	 *            The user name
	 * @param region
	 *            The AWS region
	 * @return the count of instance groups
	 * @throws SQLException
	 */
	public int instanceGroupCount(String username, String region)
			throws SQLException {
		int count;

		SQLiteDatabase db = this.getReadableDatabase(); // handle to the DB

		Log.v(TAG, "(username, region): (" + username + "," + region + ")");

		// trying to get the instance groups
		try {
			Cursor igCursor = db.query(InstanceGroupTbl.TBL_NAME,
					new String[] { " count(*) " }, InstanceGroupTbl.COL_REGION
							+ "= ? AND " + InstanceGroupTbl.COL_USERNAME
							+ " = ? ", new String[] { region, username }, null,
					null, null);

			if (igCursor.getCount() != 1) {
				throw new SQLException("No data");
			}

			igCursor.moveToFirst();
			count = igCursor.getInt(0);
			igCursor.close();
			
		} catch (SQLException exception) {
			throw exception;
		} finally {
			db.close();
		}

		return count;
	}

	/**
	 * Returns the list of instance groups for a given username and region
	 * 
	 * @param username
	 *            The user name
	 * @param region
	 *            The AWS region
	 * @return the list of instance groups
	 */
	public List<InstanceGroup> listInstanceGroups(String username, String region) {
		List<InstanceGroup> instanceGroups = new ArrayList<InstanceGroup>();

		SQLiteDatabase db = this.getReadableDatabase(); // handle to the DB

		Log.v(TAG, "ListInstanceGroups (username, region): (" + username + "," + region + ")");
		
		// trying to get the instance groups
		try {
			Cursor igCursor = db.query(InstanceGroupTbl.TBL_NAME,
					new String[] { InstanceGroupTbl._ID, InstanceGroupTbl.COL_GROUP_NAME },
					InstanceGroupTbl.COL_REGION + "= ? and "
							+ InstanceGroupTbl.COL_USERNAME + " = ? ",
					new String[] { region, username }, null, null, null);
			
			Log.d(TAG, "Number of instance groups: " + igCursor.getCount());

			if (igCursor.getCount() >= 1) {
				
				Log.d(TAG, "Instance groups found...");

				// reading the instance group names and instance ids
				while (igCursor.moveToNext()) {
					InstanceGroup instanceGroup = new InstanceGroup(
							igCursor.getLong(0), igCursor.getString(1));
					Set<String> instances = new HashSet<String>();
					
					Cursor iCursor = db.query(InstanceTbl.TBL_NAME,
							new String[] { InstanceTbl.COL_INSTANCEID},
							InstanceTbl.COL_INSTANCEGROUPID + "= ?",
							new String[] { instanceGroup.getId().toString() }, 
							null, null, null);
					
					Log.d(TAG, iCursor.getCount() + " Instances found in this instance group...");
					
					while(iCursor.moveToNext()) {
						Log.d(TAG, "Insertion of instance: " + instances.add(iCursor.getString(0)));
					}
					iCursor.close();
					
					instanceGroup.setInstanceIds(instances);
					instanceGroups.add(instanceGroup);
				}
				igCursor.close();
			}
		} finally {
			db.close();
		}

		Log.v(TAG, "Number of instance groups found: " + instanceGroups.size());

		return instanceGroups;
	}
	
	/**
	 * Write new instance group to Db
	 * @param awsUsername
	 * @param region
	 * @param groupName
	 * @param instanceIds
	 */
	public void writeInstanceGroupsToDb(String awsUsername, String region, String groupName, 
			List<String> instanceIds) throws SQLException {
		
		SQLiteDatabase db = this.getWritableDatabase();
		SQLiteStatement bulkInsert = null;
		
		try {
			//create a new group in the InstanceGroupTbl
			ContentValues insertValues = new ContentValues();
			insertValues.put(InstanceGroupTbl.COL_USERNAME, awsUsername);
			insertValues.put(InstanceGroupTbl.COL_REGION, region);
			insertValues.put(InstanceGroupTbl.COL_GROUP_NAME, groupName);
			
			db.insert(InstanceGroupTbl.TBL_NAME, null, insertValues);
			
			//query the DB to get the group ID
			Cursor resultCursor = db.query(
					InstanceGroupTbl.TBL_NAME,
					new String[]{InstanceGroupTbl._ID},
					InstanceGroupTbl.COL_GROUP_NAME + "=?",
					new String[]{groupName},
					null,
					null,
					null);
					
			if (resultCursor.getCount() != 1) {
				Log.e(TAG, "Insert failed.");
				return;
			}
			resultCursor.moveToFirst();
			long instanceGroupId = resultCursor.getLong(0);
			
			//use the instance group ID to write the data into the InstanceTbl
			String bulkInsertString = "insert into " + InstanceTbl.TBL_NAME + "(" +
			InstanceTbl.COL_INSTANCEGROUPID + ", " + InstanceTbl.COL_INSTANCEID + ") values(?,?)";
			
			Log.d(TAG, "Bulk insert statement: " + bulkInsertString);
			
			bulkInsert = db.compileStatement(bulkInsertString);
			
			short idx;
			for (String instanceId : instanceIds) {
				Log.d(TAG, "Writing " + instanceId + " to DB...");
				idx = 1;
				bulkInsert.bindLong(idx ++, instanceGroupId);
				bulkInsert.bindString(idx ++, instanceId);
				bulkInsert.execute();
			}
		}
		finally {
			db.close();
			if (bulkInsert != null) {
				bulkInsert.close();
			}
		}
	}
}