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
 * View for showing MFA confirmation upon sign-in.
 */
public class MFAView extends GluonView {

    private final TextField mfaCodeEditText;
    private final Button confirmButton;
    private final MFAActivity activity;

    /**
     * Constructs the ForgotPassword View.
     * @param activity
     */
    public MFAView(MFAActivity activity) {
        this.activity = activity;
        
        mfaCodeEditText = new TextField();
        mfaCodeEditText.setFloatText(getString("forgot.password.input.code.hint"));
        confirmButton = new Button(getString("verify.button.text"));
        confirmButton.setOnAction(e -> {
            LOG.log(Level.FINE, "confirmButton event");
            MFAView.this.activity.verify();
        });
        
        Label title = new Label(getString("mfa.header"));
        title.getStyleClass().add("title");
        Label help = new Label(getString("mfa.code.sent.message"));
        help.getStyleClass().add("help");
        help.setWrapText(true);
        
        addNodes(title, help, mfaCodeEditText, confirmButton);
    }

    @Override
    protected void updateAppBar(AppBar appBar) {
       super.updateAppBar(appBar);
        appBar.setTitleText(getString("title.activity.mfa"));
    }

    String getMFACode() {
        return mfaCodeEditText.getText();
    }

}
