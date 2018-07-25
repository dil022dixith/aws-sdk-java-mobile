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
 * This view present the ForgotPassword screen for the user to reset the
 * password.
 */
public class ForgotPasswordView extends GluonView {

    private final TextField verificationCodeEditText;
    private final PasswordField passwordEditText;
    private final Button confirmButton;
    private final ForgotPasswordActivity activity;

    /**
     * Constructs the ForgotPassword View.
     * @param activity
     */
    public ForgotPasswordView(ForgotPasswordActivity activity) {
        this.activity = activity;
        verificationCodeEditText = new TextField();
        verificationCodeEditText.setFloatText(getString("sign.up.confirm.code"));
        passwordEditText = new PasswordField();
        passwordEditText.setPromptText(getString("sign.in.password"));
        confirmButton = new Button(getString("forgot.password.button.hint"));
        confirmButton.setOnAction(e -> {
            LOG.log(Level.FINE, "confirmButton event");
            ForgotPasswordView.this.activity.forgotPassword();
        });
        
        Label title = new Label(getString("forgot.password.header"));
        title.getStyleClass().add("title");
        Label help = new Label(getString("forgot.password.body"));
        help.getStyleClass().add("help");
        help.setWrapText(true);
        
        addNodes(title, help, verificationCodeEditText, passwordEditText, confirmButton);
    }

    @Override
    protected void updateAppBar(AppBar appBar) {
        super.updateAppBar(appBar); 
        appBar.setTitleText(getString("title.activity.forgot.password"));
    }

    String getVerificationCode() {
        return verificationCodeEditText.getText();
    }

    String getPassword() {
        return passwordEditText.getText();
    }
}
