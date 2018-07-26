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

import com.amazonaws.mobile.auth.core.internal.util.ViewHelper;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.TextField;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;

/**
 * User Pools Sign-in Control. This view presents a form to handle user sign-in.
 * It also presents choices for creating a new account or retrieving a forgotten password.
 */
public class UserPoolSignInView extends GluonView {

    private final TextField userNameEditText;
    private final PasswordField passwordEditText;
    private final Button signInButton;
    
    /**
     * Constructs the UserPoolSignIn View.
     * @param provider
     */
    public UserPoolSignInView(final CognitoUserPoolsSignInProvider provider) {
        
        Pane logo = new Pane();
        logo.getStyleClass().add("logo");
        
        userNameEditText = new TextField();
        userNameEditText.setFloatText(getString("sign.in.username"));
        passwordEditText = new PasswordField();
        passwordEditText.setPromptText(getString("sign.in.password"));
        
        signInButton = new Button(getString("sign.in.button.text"));
        signInButton.setOnAction(e -> {
            LOG.log(Level.FINE, "signInButton event");
            String username = userNameEditText.getText();
            String password = passwordEditText.getText();
            final Map<String, String> result = new HashMap<>();
            result.put(CognitoUserPoolsSignInProvider.AttributeKeys.USERNAME, username);
            result.put(CognitoUserPoolsSignInProvider.AttributeKeys.PASSWORD, password);
            provider.handleActivityResult(CognitoUserPoolsSignInProvider.USER_POOL_SIGN_IN_REQUEST_CODE, 0, result);
        });
        
        Label signUpText = new Label(getString("sign.in.new.account"));
        signUpText.getStyleClass().add("left");
        signUpText.setWrapText(true);
        signUpText.setOnMouseClicked(e -> {
            LOG.log(Level.FINE, "signUpText event");
            SignUpActivity signUp = new SignUpActivity(provider);
            signUp.show();
        });
        
        Label forgotPasswordText = new Label(getString("sign.in.forgot.password"));
        forgotPasswordText.getStyleClass().add("right");
        forgotPasswordText.setWrapText(true);
        forgotPasswordText.setOnMouseClicked(e -> {
            LOG.log(Level.FINE, "forgotPasswordText event");
            String username = userNameEditText.getText();
            if (username.isEmpty()) {
                LOG.log(Level.WARNING, "Missing username.");
                ViewHelper.showDialog(getString("title.activity.sign.in"), getString("sign.up.username.missing"));
            } else {
                final CognitoUser cognitoUser = provider.getCognitoUserPool().getUser(username);
                cognitoUser.forgotPasswordInBackground(provider.getForgotPasswordHandler());
            }
        });
        
        Pane pane = new Pane();
        HBox.setHgrow(pane, Priority.ALWAYS);
        HBox box = new HBox(signUpText, pane, forgotPasswordText);
        box.getStyleClass().add("box");
        
        addNodes(logo, userNameEditText, passwordEditText, signInButton, box);
    }
    
    @Override
    protected void updateAppBar(AppBar appBar) {
        super.updateAppBar(appBar);
        appBar.setTitleText(getString("title.activity.sign.in"));
    }

    String getEnteredUserName() {
        return userNameEditText.getText();
    }

    String getEnteredPassword() {
        return passwordEditText.getText();
    }

}
