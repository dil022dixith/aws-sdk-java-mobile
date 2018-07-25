/*
  * Copyright 2013-2017 Amazon.com, Inc. or its affiliates.
  * All Rights Reserved.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

package com.amazonaws.mobile.auth.userpools;

import com.amazonaws.mobile.auth.core.signin.SignInProvider;
import com.gluonhq.charm.glisten.application.MobileApplication;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Activity to prompt for account sign up information.
 */
public class SignUpActivity {
    
    private static final Logger LOG = Logger.getLogger(SignUpActivity.class.getName());

    private static final String VIEW_NAME = "com.amazonaws.mobile.auth.userpools.SignUpView";
    
    private SignUpView signUpView;
    
    private final SignInProvider provider;

    public SignUpActivity(SignInProvider provider) {
        this.provider = provider;
    }
    
    public void show() {
        if (MobileApplication.getInstance() != null) {
            MobileApplication.getInstance().removeViewFactory(VIEW_NAME);
            LOG.log(Level.FINE, "Creating SignUpView instance"); 
            MobileApplication.getInstance().addViewFactory(VIEW_NAME, () -> {
                signUpView = new SignUpView(SignUpActivity.this);
                return signUpView;
            });
            LOG.log(Level.FINE, "Switching to SignUpView");
            GluonView.switchView(VIEW_NAME);
        } else {
            LOG.log(Level.WARNING, "Failed to create the SignUpView instance");
        }
    }

    /**
     * Retrieve input and return to caller.
     */
    public void signUp() {
        final String username = signUpView.getUserName();
        final String password = signUpView.getPassword();
        final String givenName = signUpView.getGivenName();
        final String email = signUpView.getEmail();
        final String phone = signUpView.getPhone();


        LOG.log(Level.FINE, "username = " + username + ", password = " + password + 
                ", givenName = " + givenName + ", email = " + email + ", phone = " + phone);

        final Map<String, String> result = new HashMap<>();
        result.put(CognitoUserPoolsSignInProvider.AttributeKeys.USERNAME, username);
        result.put(CognitoUserPoolsSignInProvider.AttributeKeys.PASSWORD, password);
        result.put(CognitoUserPoolsSignInProvider.AttributeKeys.GIVEN_NAME, givenName);
        result.put(CognitoUserPoolsSignInProvider.AttributeKeys.EMAIL_ADDRESS, email);
        result.put(CognitoUserPoolsSignInProvider.AttributeKeys.PHONE_NUMBER, phone);
        provider.handleActivityResult(CognitoUserPoolsSignInProvider.SIGN_UP_REQUEST_CODE, 0, result);
        if (MobileApplication.getInstance() != null) {
            MobileApplication.getInstance().switchToPreviousView();
        }
    }
    
}
