package org.elasticdroid.intf.callback;

import org.elasticdroid.model.LoginModel.LoginResult;

public interface LoginTaskCallback extends GenericCallback {

	public void loginResult(LoginResult loginResult);
}
