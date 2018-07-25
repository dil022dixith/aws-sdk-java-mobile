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

import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.TextField;
import java.util.logging.Level;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;

/**
 * The view that handles user sign-up for Cognito User Pools.
 */
public class SignUpView extends GluonView {

    private final TextField userNameEditText;
    private final PasswordField passwordEditText;
    private final TextField givenNameEditText;
    private final TextField emailEditText;
    private final TextField phoneEditText;
    private final Button signUpButton;
    
    private final SignUpActivity activity;

    /**
     * Constructs the SignUpView View.
     * @param activity
     */
    public SignUpView(SignUpActivity activity) {
        this.activity = activity;

        userNameEditText = new TextField();
        userNameEditText.setFloatText(getString("username.text"));
        passwordEditText = new PasswordField();
        passwordEditText.setPromptText(getString("sign.in.password"));
        givenNameEditText = new TextField();
        givenNameEditText.setFloatText(getString("given.name.text"));
        emailEditText = new TextField();
        emailEditText.setFloatText(getString("email.address.text"));
        phoneEditText = new TextField();
        phoneEditText.setFloatText(getString("phone.number.text"));
        signUpButton = new javafx.scene.control.Button(getString("title.activity.sign.up"));
        signUpButton.setOnAction(e -> {
            LOG.log(Level.FINE, "signUpButton event");
            SignUpView.this.activity.signUp();
        });
        
        Label title = new Label(getString("sign.up.header"));
        title.getStyleClass().add("title");
        
        addNodes(title, userNameEditText, passwordEditText, givenNameEditText, emailEditText, phoneEditText, signUpButton);
    }

    @Override
    protected void updateAppBar(AppBar appBar) {
        super.updateAppBar(appBar);
        appBar.setTitleText(getString("title.activity.sign.up"));
    }

    /**
     * @return the user's user name entered in the form.
     */
    String getUserName() {
        return userNameEditText.getText();
    }

    /**
     * @return the user's password entered in the form.
     */
    String getPassword() {
        return passwordEditText.getText();
    }

    /**
     * @return the user's given name entered in the form.
     */
    String getGivenName() {
       return givenNameEditText.getText();
    }

    /**
     * @return the user's email entered in the form.
     */
    String getEmail() {
        return emailEditText.getText();
    }

    /**
     * @return the user's phone number entered in the form.
     */
    String getPhone() {
        return phoneEditText.getText();
    }
}
