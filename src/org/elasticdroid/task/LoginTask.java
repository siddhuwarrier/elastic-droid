package org.elasticdroid.task;

import org.elasticdroid.intf.callback.LoginTaskCallback;
import org.elasticdroid.model.LoginModel;
import org.elasticdroid.model.db.User;
import org.elasticdroid.stages.login.AWSLoginStage;
import org.elasticdroid.task.tpl.GenericTask;

import android.content.Context;
import android.util.Log;

import com.ideaimpl.patterns.pipeline.Pipeline;
import com.ideaimpl.patterns.pipeline.SequentialPipeline;

public class LoginTask extends GenericTask<Void, Void, Void, LoginTaskCallback> {
	
	private Pipeline loginPipeline;
	
	private static final String TAG = LoginTask.class.getName();
	
	private LoginModel loginModel;
	
	public LoginTask(User userModel, Context context, LoginTaskCallback callback) {
		super(context, callback);
		
		this.loginModel = new LoginModel(userModel);
		
		createPipeline();
	}
	
	@Override
	protected Void doInBackground(Void... ignore) {
		//execute the pipeline
		Log.d(TAG, "Executing pipeline in background...");
		
		loginPipeline.execute(loginModel);
		
		Log.d(TAG, "Pipeline executed...");
		return null;
	}
	
	
	private void createPipeline() {
		loginPipeline = new SequentialPipeline();
		loginPipeline.addStage(new AWSLoginStage());
	}

	/**
	 * Inject mock/stub pipeline
	 */
	public void setPipeline(Pipeline loginPipeline) {
		this.loginPipeline = loginPipeline;
	}
	
	@Override
	protected void onPostExecute(Void result) {
		Log.d(TAG, "Execution complete...");
		callback.cleanUpAfterTaskExecution();
		callback.loginResult(loginModel.getLoginResult());
	}
	
	/**
	 * To retrieve results if executed in own thread
	 * @return
	 */
	LoginModel getResults() {
		return loginModel;
	}

}
