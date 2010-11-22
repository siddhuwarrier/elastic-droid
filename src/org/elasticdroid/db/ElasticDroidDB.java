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
import java.util.Hashtable;

import org.elasticdroid.db.tblinfo.LoginTbl;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Database class for ElasticDroid.
 * 
 * Has methods to create tables etc
 * 
 * @author Siddhu Warrier
 *
 * 4 Nov 2010
 */
public class ElasticDroidDB extends SQLiteOpenHelper {

	/**Name of database */
	private static final String DATABASE_NAME = "elasticdroid.db";
	/** Database version */
	//TODO use properties file for this and above.
	private static final int DATABASE_VERSION = 1;
	
	/**
	 * Initialises the superclass constructor.
	 * 
	 * @param context the view that created this object/
	 */
	public ElasticDroidDB(Context context) {
		super(context,DATABASE_NAME, null, DATABASE_VERSION);
		
	}
	
	/** 
	 * create DB tables if not exist
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.v(this.getClass().getName(), "DB onCreate()");
		
		//execute the SQL query
		db.execSQL("Create TABLE " + LoginTbl.TBL_NAME + "(" + LoginTbl._ID + " integer " +
				"primary key autoincrement, " + LoginTbl.COL_USERNAME + " text not null UNIQUE, " +
				LoginTbl.COL_ACCESSKEY + " text not null, " + LoginTbl.COL_SECRETACCESSKEY +
				" text not null," + LoginTbl.COL_DEFAULTREGION + " text, " + "UNIQUE("+ LoginTbl.COL_ACCESSKEY + ", " +  
				LoginTbl.COL_SECRETACCESSKEY +"));");
		
		db.close();
	}
	
	/**
	 * onUpgrade: to be executed when change made to DB.
	 * 
	 * This is used to upgrade from v1 to v2 atm. will be useful for
	 * anybody who might have installed tags from iteration 1. Adds a new column to store
	 * default region.
	 * 
	 * Which is to say, nobody. But nevertheless..
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}
	
	/**
	 * Convenience method to get list of users in DB {@link #DATABASE_NAME}.
	 * @return Hashtable<String, ArrayList<String>> of user data.
	 */
	public Hashtable<String, ArrayList<String>> listUserData() {
		SQLiteDatabase db = this.getReadableDatabase();
		Hashtable<String, ArrayList<String>> userData = new Hashtable<String, ArrayList<String>>();
		Cursor queryCursor;
		
		queryCursor = db.query(LoginTbl.TBL_NAME, 
				new String[]{LoginTbl.COL_USERNAME, LoginTbl.COL_ACCESSKEY, 
					LoginTbl.COL_SECRETACCESSKEY},
				null,
				null,
				null,
				null,
				null);
		
		//loop through the query and add to hashtable. Indexing starts from 1!
		queryCursor.moveToFirst();
		while (!queryCursor.isAfterLast()) {
			
			userData.put(queryCursor.getString(0), 
					new ArrayList<String>(Arrays.asList(
							new String[]{queryCursor.getString(1), queryCursor.getString(2)})));
			
			queryCursor.moveToNext();
		}
		
		queryCursor.close();
		db.close();
		
		return userData;
	}
	
	/**
	 * get the default region for the user passed as argument
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
					new String[]{LoginTbl.COL_DEFAULTREGION}, 
					LoginTbl.COL_USERNAME + "= ?", 
					new String[]{username}, 
					null, 
					null, 
					null);
			if (queryCursor.getCount() != 1) {
				throw new SQLException("No data");
			}
			
			queryCursor.moveToFirst();
			defaultRegion = queryCursor.getString(0);
		}
		catch(SQLException exception) {
			throw exception;
		}
		finally {
			db.close();
		}
		
		return defaultRegion;
	}
	
	/**
	 * Set the default region
	 * @param username
	 * @param defaultRegion
	 * @throws SQLException
	 */
	public void setDefaultRegion(String username, String defaultRegion) {
		SQLiteDatabase db = this.getReadableDatabase();
		
		ContentValues updateValues = new ContentValues();
		updateValues.put(LoginTbl.COL_DEFAULTREGION, defaultRegion);
		
		try {
			db.update(LoginTbl.TBL_NAME, 
				updateValues, 
				LoginTbl.COL_USERNAME + "=?", 
				new String[]{username});
		}
		catch(Exception ignore) {
			Log.e("AWSUtilities.setDefaultRegion:", ignore.getMessage());
		}
		finally {
			db.close();
		}
	}
}