package org.elasticdroid.model;

import org.elasticdroid.model.db.User;

import com.ideaimpl.patterns.pipeline.PipelineContextAdaptor;


public class LoginModel extends PipelineContextAdaptor {
	private User user;
	
	private LoginResult loginResult;
	
	public LoginModel(User user) {
		this.user = user;
	}
	
	public User getUser() {
		return user;
	}
	
	public void setLoginResult(LoginResult loginResult) {
		this.loginResult = loginResult;
	}
	
	public LoginResult getLoginResult() {
		return loginResult;
	}
	
	/**
	 * Reasons why an operation with AWS may fail.
	 * @author siddhu
	 *
	 */
	public enum LoginResult {
		AUTH_SUCCESS,
		AUTH_FAIL,
		AWS_CLIENT_ERROR,
		DB_WRITE_FAILED
	}
}
