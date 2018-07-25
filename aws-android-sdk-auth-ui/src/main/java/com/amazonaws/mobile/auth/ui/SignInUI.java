/*
  * Copyright 2017-2017 Amazon.com, Inc. or its affiliates.
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

import com.amazonaws.ClientConfiguration;
import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobile.auth.core.IdentityProvider;
import com.amazonaws.mobile.auth.core.DefaultSignInResultHandler;

import com.amazonaws.mobile.auth.core.signin.ui.buttons.SignInButton;
import com.amazonaws.mobile.config.AWSConfigurable;
import com.amazonaws.mobile.config.AWSConfiguration;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;

public class SignInUI implements AWSConfigurable {

    private static final Logger LOG = Logger.getLogger(SignInUI.class.getName());

    /** Calling activity. */
    private String loginCallingActivity;

    /** Next activity. */
    private String loginNextActivity;

    /** Configuration information for AuthUI. */
    private AuthUIConfiguration authUIConfiguration;

    /** AWSConfiguration object that represents the `awsconfiguration.json` file. */
    private AWSConfiguration awsConfiguration;

    /** ClientConfiguration. */
    private ClientConfiguration clientConfiguration;

    /** Configuration keys for SignInProviders in awsconfiguration.json. */
    private static final String USER_POOLS  = "CognitoUserPool";
    private static final String FACEBOOK    = "FacebookSignIn";
    private static final String GOOGLE      = "GoogleSignIn";
    private static final String FACEBOOK_BUTTON = "com.amazonaws.mobile.auth.facebook.FacebookButton";
    private static final String GOOGLE_BUTTON = "com.amazonaws.mobile.auth.google.GoogleButton";
    private static final String GOOGLE_WEBAPP_CONFIG_KEY = "ClientId-WebApp";

    /**
     * Initiate the login flow and present the AuthUI.
     * Move the nextActivity if sign-in succeeded.
     * 
     * @param callingActivity The activity
     * @param nextActivity    The next activity to go when sign-in succeeded
     * @return 
     */
    public LoginBuilder login(final String callingActivity,
                              final String nextActivity) {
        this.loginCallingActivity = callingActivity;
        this.loginNextActivity = nextActivity;
        this.authUIConfiguration = getDefaultAuthUIConfiguration();
        return new LoginBuilder();
    }

    /**
     * Initiate the sign-in flow.
     * Resume any previously signed-in Auth session.
     * Check if the user is not signed in and present the AuthUI screen.
     */
    private void presentAuthUI() {
        LOG.log(Level.FINE, "Presenting the SignIn UI.");
        final IdentityManager identityManager = IdentityManager.getDefaultIdentityManager();
        final boolean canCancel = this.authUIConfiguration.getCanCancel();
        identityManager.login(new DefaultSignInResultHandler() {
            @Override
            public void onSuccess(IdentityProvider identityProvider) {
                if (identityProvider != null) {
                    LOG.log(Level.FINE, "Sign-in succeeded. The identity provider name is available here using: " +
                            identityProvider.getDisplayName());
                    startNextActivity(loginCallingActivity, loginNextActivity);
                }
            }

            @Override
            public boolean onCancel() {
                // Return false to prevent the user from dismissing the sign in screen by pressing back button.
                // Return true to allow this.
                return canCancel;
            }
        });

        SignInActivity.startSignInActivity(this.authUIConfiguration);
    }

    /**
     * Present SignIn-UI screen if the user is not signed-in
     * On successful sign-in, move to the next activity.
     */
    private void loginWithBuilder(final LoginBuilder loginBuilder) {
         try {
            LOG.log(Level.FINE, "Initiating the SignIn flow.");
            if (loginBuilder.getAuthUIConfiguration() != null) {
                this.authUIConfiguration = loginBuilder.getAuthUIConfiguration();
            }
            final IdentityManager identityManager = IdentityManager.getDefaultIdentityManager();
            if (identityManager.isUserSignedIn()) {
                LOG.log(Level.FINE, "User is already signed-in. Moving to the next activity.");
                startNextActivity(this.loginCallingActivity, this.loginNextActivity);
            } else {
                LOG.log(Level.FINE, "User is not signed-in. Presenting the SignInUI.");
                presentAuthUI();
            }
        } catch (final Exception exception) {
            LOG.log(Level.WARNING, "Error occurred in sign-in ", exception);
        }
    }

    /**
     * {@code AWSMobileClient.LoginBuilder} accepts and retrieves
     * the optional parameters necessary for presenting the 
     * SignIn UI and initiaiting the AuthUI flow.
     */
    public class LoginBuilder {

        private AuthUIConfiguration authUIConfiguration;

        /**
         * Constructor.
         */
        public LoginBuilder() {
            this.authUIConfiguration = null;
        }

        /**
         * Set the custom authUIConfiguration passed in.
         * @param authUIConfiguration
         * @return 
         */
        public LoginBuilder authUIConfiguration(final AuthUIConfiguration authUIConfiguration) {
            this.authUIConfiguration = authUIConfiguration;
            return this;
        }

        /**
         * Retrieve the custom AuthUIConfiguration object.
         * @return authUIConfiguration
         */
        public AuthUIConfiguration getAuthUIConfiguration() {
            return this.authUIConfiguration;
        }

        /**
         * Invoke loginWithBuilder.
         */
        public void execute() {
            loginWithBuilder(this);
        }
    }

    /**
     * Retrieve the 
     * {@link com.amazonaws.mobile.auth.ui.AuthUIConfiguration} based on the 
     * {@link com.amazonaws.mobile.config.AWSConfiguration}.
     * 
     * @return authUIConfiguration
     */
    private AuthUIConfiguration getDefaultAuthUIConfiguration() {
        AuthUIConfiguration.Builder configBuilder = new AuthUIConfiguration.Builder();

        try {
            if (isConfigurationKeyPresent(USER_POOLS)) {
                configBuilder.userPools(true);
            }

            if (isConfigurationKeyPresent(FACEBOOK)) {
                configBuilder.signInButton((Class<? extends SignInButton>)Class.forName(FACEBOOK_BUTTON));
            }

            if (isConfigurationKeyPresent(GOOGLE)) {
                configBuilder.signInButton((Class<? extends SignInButton>)Class.forName(GOOGLE_BUTTON));
            }

            configBuilder.canCancel(false);
        } catch (Exception exception) {
              LOG.log(Level.WARNING, "Cannot configure the SignInUI. "
                + "Check the context and the configuration object passed in.", exception);
        }

        return configBuilder.build(); 
    }

    /**
     * Check if the AWSConfiguration has the specified key.
     * 
     * @param configurationKey The key for SignIn in AWSConfiguration
     */
    private boolean isConfigurationKeyPresent(final String configurationKey) {
        try {
            JSONObject jsonObject = this.awsConfiguration.optJsonObject(configurationKey);
            if (configurationKey.equals(GOOGLE)) {
                return jsonObject != null && jsonObject.getString(GOOGLE_WEBAPP_CONFIG_KEY) != null;
            } else {
                return jsonObject != null;
            }
        } catch (final Exception exception) {
            LOG.log(Level.WARNING, configurationKey + " not found in `awsconfiguration.json`");
            return false;
        }
    }

    /**
     * Start the next activity after successful sign-in.
     * 
     * @param currentActivity   The current activity context
     * @param nextActivity      The class of next activity to move to
     */
    private void startNextActivity(final String currentActivity, final String nextActivity) {
        if (currentActivity == null || nextActivity == null) {
            LOG.log(Level.WARNING, "Cannot start the next activity. Check the context and the nextActivity passed in.");
            return;
        }

        LOG.log(Level.FINE, "Switching to view: " + nextActivity);
        SignInView.switchView(nextActivity);
    }

    /** {@inheritDoc} */
    @Override
    public AWSConfigurable initialize() throws Exception {
        return initialize(new AWSConfiguration());
    }

    /** {@inheritDoc} */
    @Override
    public AWSConfigurable initialize(final AWSConfiguration configuration) throws Exception {
        return initialize(configuration, new ClientConfiguration());
    }

    /** {@inheritDoc} */
    @Override
    public AWSConfigurable initialize(final AWSConfiguration configuration,
                                               final ClientConfiguration clientConfiguration) throws Exception {
        LOG.log(Level.FINE, "Initializing SignInUI.");
        this.awsConfiguration = configuration;
        this.clientConfiguration = clientConfiguration;
        return this;
    }
}
