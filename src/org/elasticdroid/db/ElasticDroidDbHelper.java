package org.elasticdroid.db;

import java.sql.SQLException;

import org.elasticdroid.model.db.User;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

public class ElasticDroidDbHelper extends OrmLiteSqliteOpenHelper {
	/** Name of database */
	private static final String DATABASE_NAME = "eldroid.db";
	/** Database version */
	private static final int DATABASE_VERSION = 1;
	/** Logging tag */
	private static final String TAG = ElasticDroidDbHelper.class.getName();

	public ElasticDroidDbHelper(Context context, String databaseName,
			CursorFactory factory, int databaseVersion) {
		super(context, databaseName, null, databaseVersion);
	}

	public ElasticDroidDbHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);

		try {
			getWritableDatabase().close();
		} catch (SQLiteException ignore) {
			ignore.printStackTrace();
		}
	}

	/**
	 * Create a new DB with all fo the tables required using ORMLite
	 */
	@Override
	public void onCreate(SQLiteDatabase database,
			ConnectionSource connectionSource) {

		try {
			TableUtils.createTable(connectionSource, User.class);
		} catch (SQLException exception) {
			exception.printStackTrace();
			Log.e(TAG, "Exception: " + exception.getMessage());
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase database,
			ConnectionSource connectionSource, int oldVersion, int newVersion) {
	}

}