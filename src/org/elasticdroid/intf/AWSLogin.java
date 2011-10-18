package org.elasticdroid.intf;

import org.elasticdroid.model.LoginModel.LoginResult;


public interface AWSLogin {
	public LoginResult validateCredentials();
}
