package org.elasticdroid.stub;

import org.elasticdroid.intf.AWSLogin;
import org.elasticdroid.model.LoginModel.LoginResult;

public class AWSLoginStub implements AWSLogin {
	private LoginResult loginResult;
	
	public AWSLoginStub() {
	}
	
	public AWSLoginStub(LoginResult loginResult) {
		this.loginResult = loginResult;
	}
	
	@Override
	public LoginResult validateCredentials() {
		return loginResult == null ? LoginResult.AUTH_SUCCESS : loginResult;
	}

}
