package org.elasticdroid.tpl;


import org.elasticdroid.db.ElasticDroidDbHelper;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.provider.SyncStateContract.Constants;

import com.j256.ormlite.android.apptools.OrmLiteBaseFragmentActivity;

/**
 * GenericFragmentActivity class. 
 * Ideally all of our views should inherit from this and not Activity.
 * @author siddhuwarrier
 *
 */
public abstract class GenericFragmentActivity 
	extends OrmLiteBaseFragmentActivity<ElasticDroidDbHelper> 
	implements OnCancelListener{

	/** Dialog box for credential verification errors */
	AlertDialog alertDialogBox;
	
	/** message displayed in {@link #alertDialogBox alertDialogBox}. */
	String alertDialogMessage;
	
	/** Is the alert dialog box displayed? */
	boolean alertDialogDisplayed;
	
	/**Is progress bar displayed */
    boolean progressDialogDisplayed;
    
	/** Should we kill the activity on error? */
	boolean killActivityOnError;
    
    /** Delegate activity which does a lot of the common work */
    private final DelegateActivity delegate = new DelegateActivity();
    
    /**
     * Executed when the activity is created. Sets up the alert dialog.
     */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// create and initialise the alert dialog
		delegate.buildAlertDialogBox(this);
	}
	
	/**
	 * Save instance state. This one makes sure you save the progress dialog state!
	 */
	@Override
    public void onSaveInstanceState(Bundle saveState) {
		super.onSaveInstanceState(saveState);
		
		delegate.saveInstanceState(this, saveState);
    }
    
	/**
	 * Restore instance state. This one makes sure you restore the progress dialog state!
	 */
    @Override
    public void onRestoreInstanceState(Bundle stateToRestore) {
        super.onRestoreInstanceState(stateToRestore);
        
        delegate.restoreInstanceState(this, stateToRestore);
    }
    
	/**
	 * Function that handles the display of a progress dialog. Overriden from
	 * Activity, called once in the lifetime of an activity.
	 * 
	 * @param id Dialog ID - Special treatment for {@link Constants.PROGRESS_DIALOG}
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		try {
			return delegate.createProgressDialog(this, id);
		}
		//exception thrown because the dialog required is not a progress dialog.
		catch(IllegalArgumentException ignore) {
			return super.onCreateDialog(id);
		}
	}
	
	/**
	 * Called each time a dialog is created for display.
	 */
	@Override
	protected void onPrepareDialog(int id, Dialog dialog, Bundle ignore) {
		delegate.onPrepareDialog(this, id);
	}
	
    /**
     * Shows a dialog message
     * 
     * @param id The resource id to read the string from
     * @param _killActivityOnError kills the activity if true after the dialog returns
     */
    public void displayAlertDialog(int id, boolean killActivityOnError) {
    	//call the delegate; let it deal with this.
    	delegate.displayAlertDialog(this, id, killActivityOnError);
    }
    
    /**
     * Shows a dialog message with a custom string
     * 
     * @param id The resource id to read the string from
     * @param _killActivityOnError kills the activity if true after the dialog returns
     */
    public void displayAlertDialog(String message, boolean killActivityOnError) {
    	//call the delegate; let it deal with this.
    	delegate.displayAlertDialog(this, message, killActivityOnError);
    }
    
    /**
     * Dismisses a previously displayed alert dialog
     */
    public void dismissAlertDialog() {
    	//call the delegate; let it deal with this.
    	delegate.dismissAlertDialog(this);    	
    }
    
    /**
     * Dismisses the progress dialog
     * 
     */
    public void dismissProgressDialog() {
    	delegate.dismissProgressDialog(this);
    }

    /**
     * Return whether the alert dialog box is displayed/
     * @return @link {@link AlertDialogDisplayed}
     */
    protected boolean isAlertDialogDisplayed() {
    	return delegate.isAlertDialogDisplayed(this);
    }
    

}
