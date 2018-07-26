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

package com.amazonaws.mobile.auth.ui;

import com.amazonaws.mobile.auth.core.IdentityProvider;
import com.amazonaws.mobile.auth.core.SignInResultHandler;
import com.amazonaws.mobile.auth.core.signin.SignInManager;
import com.amazonaws.mobile.auth.core.signin.SignInProviderResultHandler;
import com.gluonhq.charm.glisten.application.MobileApplication;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Activity for handling Sign-in with an Identity Provider.
 */
public class SignInActivity {

    private static final Logger LOG = Logger.getLogger(SignInActivity.class.getName());

    private static final String VIEW_NAME = "com.amazonaws.mobile.auth.ui.SignInView";
    
    /** Reference to the singleton instance of SignInManager. */
    private SignInManager signInManager;

    /**
     * SignInProviderResultHandlerImpl handles the final result from sign in.
     */
    private class SignInProviderResultHandlerImpl implements SignInProviderResultHandler {
        /**
         * Receives the successful sign-in result and starts the main activity.
         *
         * @param provider the identity provider used for sign-in.
         */
        @Override
        public void onSuccess(final IdentityProvider provider) {
            LOG.log(Level.INFO, SignInView.getString("sign.in.succeeded.message.format", provider.getDisplayName()));

            // The sign-in manager is no longer needed once signed in.
            SignInManager.dispose();
            final SignInResultHandler signInResultsHandler = signInManager.getResultHandler();

            // Call back the results handler.
            signInResultsHandler.onSuccess(provider);
        }

        /**
         * Receives the sign-in result indicating the user canceled and shows a toast.
         *
         * @param provider the identity provider with which the user attempted sign-in.
         */
        @Override
        public void onCancel(final IdentityProvider provider) {
            LOG.log(Level.INFO, SignInView.getString("sign.in.canceled.message.format", provider.getDisplayName()));
            signInManager.getResultHandler().onIntermediateProviderCancel(provider);
        }

        /**
         * Receives the sign-in result that an error occurred signing in and shows a toast.
         *
         * @param provider the identity provider with which the user attempted sign-in.
         * @param ex       the exception that occurred.
         */
        @Override
        public void onError(final IdentityProvider provider, final Exception ex) {
            LOG.log(Level.WARNING, SignInView.getString("sign.in.error.message.format", provider.getDisplayName()), ex);
            signInManager.getResultHandler().onIntermediateProviderError(provider, ex);
        }
    }

    /**
     * This method is called when SignInActivity is created.
     * Get the instance of SignInManager and set the callback
     * to be received from SignInManager on signin.
     */
    SignInActivity(final AuthUIConfiguration config) {
        signInManager = SignInManager.getInstance();
        if (signInManager == null) {
            LOG.log(Level.WARNING, "Invoke SignInActivity.startSignInActivity() method to create the SignInManager.");
            return;
        }
        signInManager.setProviderResultsHandler(new SignInProviderResultHandlerImpl());
        if (config == null) {
            return;
        }
        if (MobileApplication.getInstance() != null) {
            MobileApplication.getInstance().removeViewFactory(VIEW_NAME);
            LOG.log(Level.FINE, "Creating SignInView instance"); 
            MobileApplication.getInstance().addViewFactory(VIEW_NAME, () -> new SignInView(config));
            LOG.log(Level.FINE, "Switching to SignInView");
            SignInView.switchView(VIEW_NAME);
        } else {
            LOG.log(Level.WARNING, "Failed to create the SignInView instance");
        }
    }

    public void onRequestPermissionsResult(final int requestCode,
                                           final String[] permissions,
                                           final int[] grantResults) {
        signInManager.handleRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    protected void onActivityResult(final int requestCode,
                                    final int resultCode,
                                    final Object data) {
        signInManager.handleActivityResult(requestCode, resultCode, data);
    }

    public void onBackPressed() {
        if (signInManager.getResultHandler().onCancel()) {
            // Since we are leaving sign-in via back, we can dispose the sign-in manager, since sign-in was cancelled.
            SignInManager.dispose();
        }
    }

    /**
     * Start the SignInActivity that kicks off the authentication flow
     * by initializing the SignInManager.
     *
     * @param config  Reference to AuthUIConfiguration object
     */
    public static void startSignInActivity(final AuthUIConfiguration config) {
        try {
            new SignInActivity(config);
        } catch (Exception exception) {
            LOG.log(Level.WARNING, "Cannot start the SignInActivity. "
              + "Check the context and the configuration object passed in.", exception);
        }
    }

    /**
     * Start the SignInActivity that kicks off the authentication flow
     * by initializing the SignInManager.
     *
     */
    public static void startSignInActivity() {
        try {
            new SignInActivity(null);
        } catch (Exception exception) {
            LOG.log(Level.WARNING, "Cannot start the SignInActivity. "
              + "Check the context passed in.", exception);
        }
    }
}
