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
 * Activity to prompt for a a verification code.
 */
public class MFAActivity {
    
    private static final Logger LOG = Logger.getLogger(MFAActivity.class.getName());

    private static final String VIEW_NAME = "com.amazonaws.mobile.auth.userpools.MFAView";
    
    private MFAView mfaView;
    
    private final SignInProvider provider;

    public MFAActivity(SignInProvider provider) {
        this.provider = provider;
    }
    
    public void show() {
        if (MobileApplication.getInstance() != null) {
            MobileApplication.getInstance().removeViewFactory(VIEW_NAME);
            LOG.log(Level.FINE, "Creating MFAView instance"); 
            MobileApplication.getInstance().addViewFactory(VIEW_NAME, () -> {
                mfaView = new MFAView(MFAActivity.this);
                return mfaView;
            });
            LOG.log(Level.FINE, "Switching to MFAView");
            GluonView.switchView(VIEW_NAME);
        } else {
            LOG.log(Level.WARNING, "Failed to create the MFAView instance");
        }
    }

    /**
     * Retrieve input and return to caller.
     */
    public void verify() {
        final String verificationCode = mfaView.getMFACode();

        LOG.log(Level.FINE, "verificationCode = " + verificationCode);

        final Map<String, String> result = new HashMap<>();
        result.put(CognitoUserPoolsSignInProvider.AttributeKeys.VERIFICATION_CODE, verificationCode);
        provider.handleActivityResult(CognitoUserPoolsSignInProvider.MFA_REQUEST_CODE, 0, result);
        if (MobileApplication.getInstance() != null) {
            MobileApplication.getInstance().switchToPreviousView();
        }
    }
    
}
