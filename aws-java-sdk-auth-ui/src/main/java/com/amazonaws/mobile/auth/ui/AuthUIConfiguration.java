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

import com.amazonaws.mobile.auth.core.signin.ui.buttons.SignInButton;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Stores Configuration information related to the SignIn UI screen.
 */
public final class AuthUIConfiguration implements Serializable {

    /**
     * Key for UserPools.
     */
    static final String CONFIG_KEY_ENABLE_USER_POOLS = "signInUserPoolsEnabled";

    /**
     * Key for SignInButtons.
     */
    static final String CONFIG_KEY_SIGN_IN_BUTTONS = "signInButtons";

    /**
     * Key for ability to cancel the sign-in.
     */
    static final String CONFIG_KEY_CAN_CANCEL = "canCancel";

    /**
     * Map to store the key and the corresponding objects.
     */
    private final Map<String, Object> config;

    /**
     * Constructor.
     *
     * @param config The Configuration Map
     */
    private AuthUIConfiguration(final Map<String, Object> config) {
      this.config = config;
    }

    /**
     * Checks if userpools is enabled by the user.
     * @return True if UserPools is enabled
     */
    public boolean getSignInUserPoolsEnabled() {
      Object userPoolsEnabled = config.get(CONFIG_KEY_ENABLE_USER_POOLS);
      if (userPoolsEnabled != null) {
          return (Boolean) userPoolsEnabled;
      } else {
          return false;
      }
    }

    /**
     * Gets the list of the SignInButton classes configured.
     * @return The list of SignInButton classes
     */
    public ArrayList<Class<? extends SignInButton>> getSignInButtons() {
        return (ArrayList) config.get(CONFIG_KEY_SIGN_IN_BUTTONS);
    }

    public boolean getCanCancel() {
        Object canCancel = config.get(CONFIG_KEY_CAN_CANCEL);
        if (canCancel != null) {
            return (Boolean) canCancel;
        } else {
            return false;
        }
    }

    /**
     * Class for building the AuthUIConfiguration object
     *
     * For example, create the config object with specific attributes.
     *
     *  AuthUIConfiguration config =
     *           new AuthUIConfiguration.Builder()
     *               .userPools(true)
     *               .logoResId(logoResourceIdentifier)
     *               .signInButton(CustomSignInButton.class)
     *               .build();
     */
    public static class Builder {

        /** Local object for storing the configuration. */
        private final Map<String, Object> configuration = new HashMap<>();

        /** Constructor. */
        public Builder() { }

        /**
         * Invoke this method in order to enable userpools.
         *
         * @param enabledUserPools Flag that indicates if the userpools is enabled or not
         * @return builder
         */
        public Builder userPools(final boolean enabledUserPools) {
            configuration.put(CONFIG_KEY_ENABLE_USER_POOLS, enabledUserPools);
            return this;
        }

        /**
         * Add a SignInButton to the SignIn Screen by passing in the Class
         * of the button that inherits from the SignInButton.
         *
         * @param signInButton Button Class that inherits from the SignInButton
         * @return builder
         */
        public Builder signInButton(final Class<? extends SignInButton> signInButton) {
            ArrayList<Class<? extends SignInButton>> signInButtonList;
            if (configuration.get(CONFIG_KEY_SIGN_IN_BUTTONS) == null) {
                signInButtonList = new ArrayList<>();
                signInButtonList.add(signInButton);
                configuration.put(CONFIG_KEY_SIGN_IN_BUTTONS, signInButtonList);
            } else {
                signInButtonList = (ArrayList) configuration.get(CONFIG_KEY_SIGN_IN_BUTTONS);
                signInButtonList.add(signInButton);
            }
            return this;
        }

        /**
         * Ability to cancel the signin flow.
         * 
         * @param canCancelSignIn
         * @return builder
         */
        public Builder canCancel(final boolean canCancelSignIn) {
            configuration.put(CONFIG_KEY_CAN_CANCEL, canCancelSignIn);
            return this;
        }

        /**
         * Builds the AuthUIConfiguration object.
         * @return the AuthUIConfiguration created by the parts provided
         */
        public AuthUIConfiguration build() {
            return new AuthUIConfiguration(configuration);
        }
    }
}
