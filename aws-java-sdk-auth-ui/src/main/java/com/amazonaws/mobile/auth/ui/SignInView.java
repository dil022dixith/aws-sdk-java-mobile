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

import com.amazonaws.mobile.auth.core.signin.SignInManager;
import com.amazonaws.mobile.auth.core.signin.ui.buttons.SignInButton;
import com.amazonaws.mobile.auth.userpools.CognitoUserPoolsSignInProvider;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.application.ViewStackPolicy;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import java.text.MessageFormat;

import java.util.ArrayList;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * View for displaying sign-in components.
 */
public class SignInView extends View {

    private static final Logger LOG = Logger.getLogger(SignInView.class.getName());
    
    private static final ResourceBundle BUNDLE;
    static {
        BUNDLE = ResourceBundle.getBundle("com/amazonaws/mobile/auth/ui/ui");
    }

    /** String that represents the SDK Version. */
    private static final String SDK_VERSION = "2.6.19-b2";

    /** Common Prefix of the namespaces of different SignIn providers. */
    private static final String NAMESPACE_COMMON_PREFIX = "com.amazonaws.mobile.auth";

    /** Group name. */
    private static final String AWS_MOBILE_AUTH_GROUP_NAME = "com.amazonaws";

    /** Dependency name for Facebook Button class. */
    private static final String FACEBOOK_BUTTON = NAMESPACE_COMMON_PREFIX + ".facebook.FacebookButton";

    /** Dependency name for Facebook SignIn package. */
    private static final String FACEBOOK_SIGN_IN_IMPORT = AWS_MOBILE_AUTH_GROUP_NAME
            + ":aws-android-sdk-auth-facebook:"
            + SDK_VERSION;

    /** Dependency name for Google Button class. */
    private static final String GOOGLE_BUTTON = NAMESPACE_COMMON_PREFIX + ".google.GoogleButton";

    /** Dependency name for Google SignIn package. */
    private static final String GOOGLE_SIGN_IN_IMPORT =  AWS_MOBILE_AUTH_GROUP_NAME
            + ":aws-android-sdk-auth-google:"
            +  SDK_VERSION;

    /** Configuration Key to store AuthUIConfiguration objects. */
    public static final String CONFIGURATION_KEY = "com.amazonaws.mobile.auth.ui.configurationkey";

    /** Reference to the AuthUIConfiguration. */
    private AuthUIConfiguration config = null;

    /** Stores the list of SignInButtons. */
    private ArrayList<SignInButton> buttonStore = null;

    private final VBox center;
    private final Separator divider;

    /**
     * Sets up the UserPools UI with the Email and Password FormView.
     * @param context The activity context.
     */
    private void setUpUserPools() {
        /**
         * Use Reflection for UserPoolSignIn dependency.
         */
        if (this.config != null && this.config.getSignInUserPoolsEnabled()) {
            LOG.log(Level.FINE, "Trying to create an instance of UserPoolSignInView");
            
            Button buttonUserPool = new Button(getString("user.pool.sign.in"));
            center.getChildren().add(buttonUserPool);
            SignInManager.getInstance().initializeSignInButton(
                    CognitoUserPoolsSignInProvider.class, buttonUserPool);
        }
    }

    /**
     * Sets up the divider that divides the UserPools UI and the SignInButtons.
     * @param context The activity context.
     */
    private void setUpDivider() {
        center.getChildren().add(divider);
    }

    /**
     * Sets up the SignIn Buttons.
     * @param context The activity context.
     */
    private void setUpSignInButtons() {

        /**
         * Add the signInButtons configured to the view.
         */
        this.addSignInButtonsToView();

        /**
         * There are two conditions on which the divider is set.
         *
         * 1. If UserPools is configured and one or more buttons are added.
         * 2. If One of more buttons are added.
         */
        divider.setVisible(this.buttonStore.size() > 0);
    }

    /**
     * Constructor.
     * @param config
     */
    public SignInView(AuthUIConfiguration config) {
        getStyleClass().add("aws");
        this.center = new VBox();
        center.getStyleClass().add("center");
        setCenter(center);
        
        Pane logo = new Pane();
        logo.getStyleClass().add("logo");
        center.getChildren().add(logo);
        
        this.buttonStore = new ArrayList<>();
        this.config = config;

        this.divider = new Separator(Orientation.HORIZONTAL);
        
        this.setUpUserPools();
        this.setUpDivider();
        this.setUpSignInButtons();
    }

    @Override
    protected void updateAppBar(AppBar appBar) {
        appBar.setNavIcon(MaterialDesignIcon.CHEVRON_LEFT.button(e -> {
            getApplication().switchToPreviousView();
        }));
        appBar.setTitleText(getString("title.activity.sign.in"));
    }

    /** 
     * Add SignInButtons to the view.
     * @param context The activity context.
     */
    private void addSignInButtonsToView() {
        try {
            if (this.config != null) {
                ArrayList<Class<? extends SignInButton>> signInButtons = this.config.getSignInButtons();
                if (signInButtons == null) {
                    LOG.log(Level.WARNING, "Skipping creating the SignInButtons. No SignInbuttons were added to the view.");
                    return;
                }

                for (Class<? extends SignInButton> signInButton : signInButtons) {
                    // TODO: implement
                    SignInButton buttonObject = new SignInButton();
                    LOG.log(Level.FINE, "Adding SignInButton "+ signInButton.getCanonicalName());
                    this.buttonStore.add(buttonObject);
                    center.getChildren().add(buttonObject);
                }
            } else {
                LOG.log(Level.FINE, "AuthUIConfiguration is not configured with any SignInButtons. "
                                + "There are no buttons to add to the view");
            }
        } catch (Exception exception) {
            LOG.log(Level.WARNING, "Cannot access the configuration or error in adding the signin button to the view", exception);
        }
    }
    
    @Override
    public String getUserAgentStylesheet() {
        return SignInView.class.getResource("aws-style.css").toExternalForm();
    }
    
    static String getString(String value) {
        try {
            return BUNDLE.getString(value);
        } catch (MissingResourceException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        return "";
    }
    
    static String getString(String value, Object... arguments) {
        return MessageFormat.format(getString(value), arguments);
    }
    
    static void switchView(final String viewName) {
        if (Platform.isFxApplicationThread()) {
            MobileApplication.getInstance().switchView(viewName);
        } else {
            Platform.runLater(() -> {
                MobileApplication.getInstance().switchView(viewName);
            });
        }
    }
}
