package com.j256.ormlite.android.apptools;

import android.app.Application;

import com.j256.ormlite.support.ConnectionSource;

/**
 * Base class to use for activities in Android.
 * 
 * You can simply call {@link #getHelper()} to get your helper class, or {@link #getConnectionSource()} to get a
 * {@link ConnectionSource}.
 * 
 * The method {@link #getHelper()} assumes you are using the default helper factory -- see {@link OpenHelperManager}. If
 * not, you'll need to provide your own helper instances which will need to implement a reference counting scheme. This
 * method will only be called if you use the database, and only called once for this activity's life-cycle. 'close' will
 * also be called once for each call to createInstance.
 * 
 * @author Siddhu Warrier
 */
public abstract class OrmLiteBaseApplication<H extends OrmLiteSqliteOpenHelper> extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
	}
}
