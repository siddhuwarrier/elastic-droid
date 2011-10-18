package org.elasticdroid.task.tpl;

import org.elasticdroid.intf.callback.GenericCallback;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Generic Task.
 * 
 * @author siddhuwarrier
 * 
 * @param <T>Parameter passed to doInBackground
 * @param <U>Parameter passed to the calling thread to indicate progress
 * @param <V>Return value from the task
 * @param <W>The callback type to call with results.
 */
public abstract class GenericTask<T, U, V, W extends GenericCallback> 
	extends AsyncTask<T, U, V> {
    /** The callback class */
    protected W callback;
    
    /** The context; used to get the DB */
    protected Context context;
    
    
    
    private static final String TAG = GenericTask.class.getName();

    /**
     * Constructor. Saves the activity that called this. This is used to return
     * the data back to the Activity.
     * 
     * @param activity
     *            The Android UI activity that created the GenericModel.
     */
    public GenericTask(Context context, W callback) {
        this.context = context;
    	this.callback = callback;
    }
    
    @Override
    protected void onPreExecute() {
    }

    /**
     * Set the callback object referred to by the model. This is used by the
     * activity to reset itself to null when it is being destroyed temporarily
     * (for instance whenever the screen orientation is changed), and to reset
     * it whenever the object is restored after being destroyed.
     * 
     * @param callback
     *            the GenericCallback referred to in the Model
     */
    public void setCallback(W callback) {
        this.callback = callback;
    }
    
	/**
	 * Executed on the UI thread when the progress bar is cancelled.
	 * It returns null to the activity; the activity can process this if it likes.
	 */
    @Override
	protected void onCancelled () {
		Log.w(TAG, "Background operation cancelled! Callback will not be called.");
		this.callback = null;
	}
    
    public Context getContext() {
    	return context;
    }
}
