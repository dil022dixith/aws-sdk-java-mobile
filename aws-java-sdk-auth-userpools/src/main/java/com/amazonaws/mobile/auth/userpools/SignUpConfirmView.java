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

/**
 * This view presents the confirmation screen for user sign up.
 */
public class SignUpConfirmView extends GluonView {

    private final TextField userNameEditText;
    private final TextField confirmCodeEditText;
    private final Button confirmButton;
    private final SignUpConfirmActivity activity;

    /**
     * Constructs the SignUpConfirmView View.
     * @param activity
     */
    public SignUpConfirmView(SignUpConfirmActivity activity) {
        this.activity = activity;
        confirmCodeEditText = new TextField();
        confirmCodeEditText.setFloatText(getString("sign.up.confirm.code"));
        userNameEditText = new TextField();
        userNameEditText.setFloatText(getString("username.text"));
        confirmButton = new Button(getString("sign.up.confirm.text"));
        confirmButton.setOnAction(e -> {
            LOG.log(Level.FINE, "confirmButton event");
            SignUpConfirmView.this.activity.confirmAccount();
        });
        
        Label title = new Label(getString("sign.up.confirm.title"));
        title.getStyleClass().add("title");
        Label help = new Label(getString("sign.up.confirm.code.sent") +"\n\n" + getString("sign.up.confirm.enter.code"));
        help.getStyleClass().add("help");
        help.setWrapText(true);
        
        addNodes(title, help, confirmCodeEditText, userNameEditText, confirmButton);
    }

    @Override
    protected void updateAppBar(AppBar appBar) {
        super.updateAppBar(appBar);
        appBar.setTitleText(getString("title.activity.sign.up.confirm"));
    }
    
    void setUserName(String value) {
        userNameEditText.setText(value);
        userNameEditText.requestFocus();
    }

    String getConfirmCode() {
        return confirmCodeEditText.getText();
    }

    String getUserName() {
        return userNameEditText.getText();
    }

}
