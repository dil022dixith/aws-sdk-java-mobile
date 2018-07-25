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

import com.amazonaws.mobile.config.AWSConfiguration;

import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.VerificationHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoDevice;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserAttributes;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserCodeDeliveryDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ChallengeContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.ForgotPasswordContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.ForgotPasswordHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.GenericHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.SignUpHandler;

import com.amazonaws.services.cognitoidentityprovider.model.InvalidParameterException;
import com.amazonaws.services.cognitoidentityprovider.model.NotAuthorizedException;
import com.amazonaws.services.cognitoidentityprovider.model.UserNotConfirmedException;
import com.amazonaws.services.cognitoidentityprovider.model.UserNotFoundException;

import com.amazonaws.mobile.auth.core.signin.SignInProvider;
import com.amazonaws.mobile.auth.core.signin.SignInProviderResultHandler;
import com.amazonaws.mobile.auth.core.internal.util.ViewHelper;

import com.amazonaws.regions.Regions;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.mvc.View;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.control.Button;

/**
 * Manages sign-in using Cognito User Pools.
 */
public class CognitoUserPoolsSignInProvider implements SignInProvider {

    private static final Logger LOG = Logger.getLogger(CognitoUserPoolsSignInProvider.class.getName());
    
    /**
     * Cognito User Pools attributes.
     */
    public static final class AttributeKeys {

        /** Username attribute. */
        public static final String USERNAME = "username";

        /** Password attribute. */
        public static final String PASSWORD = "password";

        /** Verification code attribute. */
        public static final String VERIFICATION_CODE = "verification_code";

        /** Given name attribute. */
        public static final String GIVEN_NAME = "given_name";

        /** Email address attribute. */
        public static final String EMAIL_ADDRESS = "email";

        /** Phone number attribute. */
        public static final String PHONE_NUMBER = "phone_number";
    }

    /** Start of Intent request codes owned by the Cognito User Pools app. */
    private static final int REQUEST_CODE_START = 0x2970;

    /** Request code for password reset Intent. */
    public static final int FORGOT_PASSWORD_REQUEST_CODE = REQUEST_CODE_START + 42;

    /** Request code for account registration Intent. */
    public static final int SIGN_UP_REQUEST_CODE = REQUEST_CODE_START + 43;

    /** Request code for MFA Intent. */
    public static final int MFA_REQUEST_CODE = REQUEST_CODE_START + 44;

    /** Request code for account verification Intent. */
    public static final int VERIFICATION_REQUEST_CODE = REQUEST_CODE_START + 45;

    /** Request code for account verification Intent. */
    public static final int USER_POOL_SIGN_IN_REQUEST_CODE = REQUEST_CODE_START + 46;

    /** Request codes that the Cognito User Pools can handle. */
    private static final Set<Integer> REQUEST_CODES = new HashSet<Integer>() { {
        add(FORGOT_PASSWORD_REQUEST_CODE);
        add(SIGN_UP_REQUEST_CODE);
        add(MFA_REQUEST_CODE);
        add(VERIFICATION_REQUEST_CODE);
        add(USER_POOL_SIGN_IN_REQUEST_CODE);
    } };

    /** Stores the configuration file name. */
    private static final String AWS_CONFIGURATION_FILE = "AWSConfiguration";

    /** Minimum length of password supported by Cognito. */
    private static final int PASSWORD_MIN_LENGTH = 6;

    /** Prefix of the exception message. */
    private static final String USERPOOLS_EXCEPTION_PREFIX = "(Service";

    private static final String USER_POOL_VIEW_NAME = "com.amazonaws.mobile.auth.userpools.UserPoolSignInView";
    
    /** The sign-in results adapter from the SignInManager. */
    private SignInProviderResultHandler resultsHandler;

    /** Forgot Password processing provided by the Cognito User Pools SDK. */
    private ForgotPasswordContinuation forgotPasswordContinuation;

    /** MFA processing provided by the Cognito User Pools SDK. */
    private MultiFactorAuthenticationContinuation multiFactorAuthenticationContinuation;

    /** Sign-in username. */
    private String username;

    /** Sign-in password. */
    private String password;

    /** Sign-in verification code. */
    private String verificationCode;

    /** The key for CognitoUserPools Login. */
    private String cognitoLoginKey;

    /** Active Cognito User Pool. */
    private CognitoUserPool cognitoUserPool;

    /** Active Cognito User Pools session. */
    private CognitoUserSession cognitoUserSession;

    /** AWSConfiguration object. */
    private AWSConfiguration awsConfiguration;

    /**
     * Handle callbacks from the Forgot Password flow.
     * @return 
     */
    protected ForgotPasswordHandler getForgotPasswordHandler() {
        return new ForgotPasswordHandler() {
            @Override
            public void onSuccess() {
                LOG.log(Level.FINE, "Password change succeeded.");
                GluonView.showProgressBar(false);
                ViewHelper.showDialog(GluonView.getString("title.activity.forgot.password"), 
                        GluonView.getString("password.change.success"));
            }

            @Override
            public void getResetCode(final ForgotPasswordContinuation continuation) {
                forgotPasswordContinuation = continuation;

                ForgotPasswordActivity forgot = new ForgotPasswordActivity(CognitoUserPoolsSignInProvider.this);
                forgot.show();
            }

            @Override
            public void onFailure(final Exception exception) {
                LOG.log(Level.WARNING, "Password change failed.", exception);
                GluonView.showProgressBar(false);
                final String message;
                if (exception instanceof InvalidParameterException) {
                    message = GluonView.getString("password.change.no.verification.failed");
                } else {
                    message = getErrorMessageFromException(exception);
                }
                ViewHelper.showDialog(GluonView.getString("title.activity.forgot.password"), 
                        GluonView.getString("password.change.failed") + " " + message);
            }
        };
    }
    
    /**
     * Start the SignUp Confirm Activity with the attribte keys.
     */
    private void startVerificationActivity() {
        SignUpConfirmActivity confirm = new SignUpConfirmActivity(CognitoUserPoolsSignInProvider.this);
        confirm.show(username);
    }

    /**
     * Handle callbacks from the Sign Up flow.
     */
    private final SignUpHandler signUpHandler = new SignUpHandler() {
        @Override
        public void onSuccess(final CognitoUser user, final boolean signUpConfirmationState,
                              final CognitoUserCodeDeliveryDetails cognitoUserCodeDeliveryDetails) {
            GluonView.showProgressBar(false);
            if (signUpConfirmationState) {
                LOG.log(Level.FINE, "Signed up. User ID = " + user.getUserId());
                ViewHelper.showDialog(GluonView.getString("title.activity.sign.up"), 
                        GluonView.getString("sign.up.success") + " " + user.getUserId());
            } else {
                LOG.log(Level.WARNING, "Additional confirmation for sign up.");
                startVerificationActivity();
            }
        }

        @Override
        public void onFailure(final Exception exception) {
            LOG.log(Level.WARNING, "Sign up failed.", exception);
            GluonView.showProgressBar(false);
            ViewHelper.showDialog(GluonView.getString("title.dialog.sign.up.failed"),
                exception.getLocalizedMessage() != null ? getErrorMessageFromException(exception)
                    : GluonView.getString("sign.up.failed"));
        }
    };

    /**
     * Handle callbacks from the Sign Up - Confirm Account flow.
     */
    private final GenericHandler signUpConfirmationHandler = new GenericHandler() {
        @Override
        public void onSuccess() {
            LOG.log(Level.INFO, "Confirmed.");
            GluonView.showProgressBar(false);
            ViewHelper.showDialog(GluonView.getString("title.activity.sign.up.confirm"), 
                    GluonView.getString("sign.up.confirm.success"));
        }

        @Override
        public void onFailure(final Exception exception) {
            LOG.log(Level.WARNING, "Failed to confirm user.", exception);
            GluonView.showProgressBar(false);
            ViewHelper.showDialog(GluonView.getString("title.activity.sign.up.confirm"), 
                    GluonView.getString("sign.up.confirm.failed") + " " + getErrorMessageFromException(exception));
        }
    };

    /**
     * Resent the confirmation code on MFA.
     */
    private void resendConfirmationCode() {
        final CognitoUser cognitoUser = cognitoUserPool.getUser(username);
        cognitoUser.resendConfirmationCodeInBackground(new VerificationHandler() {
            @Override
            public void onSuccess(final CognitoUserCodeDeliveryDetails verificationCodeDeliveryMedium) {
                GluonView.showProgressBar(false);
                startVerificationActivity();
            }

            @Override
            public void onFailure(final Exception exception) {
                GluonView.showProgressBar(false);
                if (null != resultsHandler) {
                    ViewHelper.showDialog(GluonView.getString("title.activity.sign.in"),
                        GluonView.getString("login.failed") + "\n" +
                        GluonView.getString("login.failed.text") + "\n" + getErrorMessageFromException(exception));

                    resultsHandler.onError(CognitoUserPoolsSignInProvider.this, exception);
                }
            }
        });
    }

    /**
     * Handle callbacks from the Authentication flow. Includes MFA handling.
     */
    private final AuthenticationHandler authenticationHandler = new AuthenticationHandler() {
        @Override
        public void onSuccess(final CognitoUserSession userSession, final CognitoDevice newDevice) {
            LOG.log(Level.INFO, "Logged in. " + userSession.getIdToken());
            GluonView.showProgressBar(false);
            cognitoUserSession = userSession;

            if (null != resultsHandler) {
                resultsHandler.onSuccess(CognitoUserPoolsSignInProvider.this);
            }
        }

        @Override
        public void getAuthenticationDetails(
                final AuthenticationContinuation authenticationContinuation, final String userId) {

            if (null != username && null != password) {
                final AuthenticationDetails authenticationDetails = new AuthenticationDetails(
                        username,
                        password,
                        null);

                authenticationContinuation.setAuthenticationDetails(authenticationDetails);
                authenticationContinuation.continueTask();
            }
        }

        @Override
        public void getMFACode(final MultiFactorAuthenticationContinuation continuation) {
            multiFactorAuthenticationContinuation = continuation;

            MFAActivity mfa = new MFAActivity(CognitoUserPoolsSignInProvider.this);
            mfa.show();
        }

        @Override
        public void authenticationChallenge(final ChallengeContinuation continuation) {
            throw new UnsupportedOperationException("Not supported in this sample.");
        }

        @Override
        public void onFailure(final Exception exception) {
            LOG.log(Level.WARNING, "Failed to login.", exception);
            GluonView.showProgressBar(false);
            final String message;

            // UserNotConfirmedException will only happen once in the sign-in flow in the case
            // that the user attempting to sign in had not confirmed their account by entering
            // the correct verification code. A different exception is thrown if the code
            // is invalid, so this will not create an continuous confirmation loop if the
            // user enters the wrong code.
            if (exception instanceof UserNotConfirmedException) {
                resendConfirmationCode();
                return;
            }

            if (exception instanceof UserNotFoundException) {
                message = GluonView.getString("user.does.not.exist");
            } else if (exception instanceof NotAuthorizedException) {
                message = GluonView.getString("incorrect.username.or.password");
            } else {
                message = getErrorMessageFromException(exception);
            }

            if (null != resultsHandler) {
                ViewHelper.showDialog(GluonView.getString("title.activity.sign.in"), 
                        GluonView.getString("login.failed") + " " + message);
                resultsHandler.onError(CognitoUserPoolsSignInProvider.this, exception);
            }
        }
    };

    /** {@inheritDoc} */
    @Override
    public void initialize(final AWSConfiguration awsConfiguration) {
        this.awsConfiguration = awsConfiguration;

        LOG.log(Level.FINE, "initalizing Cognito User Pools");

        final String regionString = getCognitoUserPoolRegion();
        final Regions region = Regions.fromName(regionString);

        this.cognitoUserPool = new CognitoUserPool(getCognitoUserPoolId(),
                                                   getCognitoUserPoolClientId(),
                                                   getCognitoUserPoolClientSecret(),
                                                   region);

        cognitoLoginKey = "cognito-idp." +  region.getName()
            + ".amazonaws.com/" + getCognitoUserPoolId();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isRequestCodeOurs(final int requestCode) {
        return REQUEST_CODES.contains(requestCode);
    }

    /** {@inheritDoc} */
    @Override
    public void handleActivityResult(final int requestCode,
                                     final int resultCode,
                                     final Object data) {

        if (resultCode == 0) {
            Map<String, String> map = (Map<String, String>) data;
            GluonView.showProgressBar(true);
            switch (requestCode) {
                case FORGOT_PASSWORD_REQUEST_CODE:
                    
                    password = map.get(CognitoUserPoolsSignInProvider.AttributeKeys.PASSWORD);
                    verificationCode = map.get(CognitoUserPoolsSignInProvider.AttributeKeys.VERIFICATION_CODE);

                    if (password.length() < PASSWORD_MIN_LENGTH) {
                        ViewHelper.showDialog(GluonView.getString("title.activity.forgot.password"),
                                    GluonView.getString("password.length.validation.failed"));
                        return;
                    } 

                    LOG.log(Level.FINE, "verificationCode = " + verificationCode);

                    forgotPasswordContinuation.setPassword(password);
                    forgotPasswordContinuation.setVerificationCode(verificationCode);
                    forgotPasswordContinuation.continueTask();
                    break;
                case SIGN_UP_REQUEST_CODE:
                    username = map.get(CognitoUserPoolsSignInProvider.AttributeKeys.USERNAME);
                    password = map.get(CognitoUserPoolsSignInProvider.AttributeKeys.PASSWORD);
                    final String givenName = map.get(CognitoUserPoolsSignInProvider.AttributeKeys.GIVEN_NAME);
                    final String email = map.get(CognitoUserPoolsSignInProvider.AttributeKeys.EMAIL_ADDRESS);
                    final String phone = map.get(CognitoUserPoolsSignInProvider.AttributeKeys.PHONE_NUMBER);

                    if (username.length() < 1) {
                        ViewHelper.showDialog(GluonView.getString("title.activity.sign.up"),
                                    GluonView.getString("sign.up.failed") + " " + GluonView.getString("sign.up.username.missing"));
                        return;
                    }

                    if (password.length() < PASSWORD_MIN_LENGTH) {
                        ViewHelper.showDialog(GluonView.getString("title.activity.sign.up"),
                                    GluonView.getString("sign.up.failed") + " " + GluonView.getString("password.length.validation.failed"));
                        return;
                    }

                    LOG.log(Level.FINE, "username = " + username);
                    LOG.log(Level.FINE, "given_name = " + givenName);
                    LOG.log(Level.FINE, "email = " + email);
                    LOG.log(Level.FINE, "phone = " + phone);

                    final CognitoUserAttributes userAttributes = new CognitoUserAttributes();
                    userAttributes.addAttribute(CognitoUserPoolsSignInProvider.AttributeKeys.GIVEN_NAME, givenName);
                    userAttributes.addAttribute(CognitoUserPoolsSignInProvider.AttributeKeys.EMAIL_ADDRESS, email);

                    if (null != phone && phone.length() > 0) {
                        userAttributes.addAttribute(CognitoUserPoolsSignInProvider.AttributeKeys.PHONE_NUMBER, phone);
                    }

                    cognitoUserPool.signUpInBackground(username, password, userAttributes,
                            null, signUpHandler);

                    break;
                case MFA_REQUEST_CODE:
                    verificationCode = map.get(CognitoUserPoolsSignInProvider.AttributeKeys.VERIFICATION_CODE);

                    if (verificationCode.length() < 1) {
                        ViewHelper.showDialog(GluonView.getString("title.activity.mfa"),
                                    GluonView.getString("mfa.failed") + " " + GluonView.getString("mfa.code.empty"));
                        return;
                    }

                    LOG.log(Level.FINE, "verificationCode = " + verificationCode);

                    multiFactorAuthenticationContinuation.setMfaCode(verificationCode);
                    multiFactorAuthenticationContinuation.continueTask();

                    break;
                case VERIFICATION_REQUEST_CODE:
                    username = map.get(CognitoUserPoolsSignInProvider.AttributeKeys.USERNAME);
                    verificationCode = map.get(CognitoUserPoolsSignInProvider.AttributeKeys.VERIFICATION_CODE);

                    if (username.length() < 1) {
                        ViewHelper.showDialog(GluonView.getString("title.activity.sign.up.confirm"),
                                    GluonView.getString("sign.up.confirm.title") + " " + GluonView.getString("sign.up.username.missing"));
                        return;
                    }

                    if (verificationCode.length() < 1) {
                        ViewHelper.showDialog(GluonView.getString("title.activity.sign.up.confirm"),
                                    GluonView.getString("sign.up.confirm.title") + " " + GluonView.getString("sign.up.confirm.code.missing"));
                        return;
                    }

                    LOG.log(Level.FINE, "username = " + username);
                    LOG.log(Level.FINE, "verificationCode = " + verificationCode);

                    final CognitoUser cognitoUser = cognitoUserPool.getUser(username);

                    cognitoUser.confirmSignUpInBackground(verificationCode, true, signUpConfirmationHandler);

                    break;
                case USER_POOL_SIGN_IN_REQUEST_CODE:
                    username = map.get(CognitoUserPoolsSignInProvider.AttributeKeys.USERNAME);
                    password = map.get(CognitoUserPoolsSignInProvider.AttributeKeys.PASSWORD);
                    LOG.log(Level.FINE, "username = " + username);

                    final CognitoUser cognitoUserSignIn = cognitoUserPool.getUser(username);
                    cognitoUserSignIn.getSessionInBackground(authenticationHandler);
                    break;
                default:
                    LOG.log(Level.WARNING, "Unknown Request Code sent.");
            }
        }
    }

    private UserPoolSignInView userPoolSignInView;
            
    @Override
    public void initializeSignInButton(final Button buttonView, final SignInProviderResultHandler resultsHandler) {
        this.resultsHandler = resultsHandler;
        
        buttonView.setOnAction(e -> {
            if (MobileApplication.getInstance() != null) {
                MobileApplication.getInstance().removeViewFactory(USER_POOL_VIEW_NAME);
                LOG.log(Level.FINE, "Creating UserPoolSignInView instance");
                MobileApplication.getInstance().addViewFactory(USER_POOL_VIEW_NAME, new Supplier<View>() {
                    @Override
                    public View get() {
                        userPoolSignInView = new UserPoolSignInView(CognitoUserPoolsSignInProvider.this);
                        return userPoolSignInView;
                    }
                });
                LOG.log(Level.FINE, "Switching to UserPoolSignInView");
                GluonView.switchView(USER_POOL_VIEW_NAME);
            } else {
                LOG.log(Level.WARNING, "Failed to create the UserPoolSignInView instance");
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public String getDisplayName() {
        return "Amazon Cognito Your User Pools";
    }

    /** {@inheritDoc} */
    @Override
    public String getCognitoLoginKey() {
        return cognitoLoginKey;
    }

    /**
     * Authentication handler for handling token refresh.
     */
    private static class RefreshSessionAuthenticationHandler implements AuthenticationHandler {
        private CognitoUserSession userSession = null;

        private CognitoUserSession getUserSession() {
            return userSession;
        }

        @Override
        public void onSuccess(final CognitoUserSession userSession, final CognitoDevice newDevice) {
            this.userSession = userSession;
        }

        @Override
        public void getAuthenticationDetails(final AuthenticationContinuation authenticationContinuation,
                                             final String UserId) {
            LOG.log(Level.FINE, "Can't refresh the session silently, due to authentication details needed.");
        }

        @Override
        public void getMFACode(final MultiFactorAuthenticationContinuation continuation) {
            LOG.log(Level.SEVERE, "Refresh flow can not trigger request for MFA code.");
        }

        @Override
        public void authenticationChallenge(final ChallengeContinuation continuation) {
            LOG.log(Level.SEVERE, "Refresh flow can not trigger request for authentication challenge.");
        }

        @Override
        public void onFailure(final Exception exception) {
            LOG.log(Level.WARNING, "Can't refresh session.", exception);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean refreshUserSignInState() {
        if (null != cognitoUserSession && cognitoUserSession.isValid()) {
            return true;
        }

        final RefreshSessionAuthenticationHandler refreshSessionAuthenticationHandler
            = new RefreshSessionAuthenticationHandler();

        cognitoUserPool.getCurrentUser().getSession(refreshSessionAuthenticationHandler);
        if (null != refreshSessionAuthenticationHandler.getUserSession()) {
            cognitoUserSession = refreshSessionAuthenticationHandler.getUserSession();
            LOG.log(Level.INFO, "refreshUserSignInState: Signed in with Cognito.");
            return true;
        }

        LOG.log(Level.INFO, "refreshUserSignInState: Not signed in with Cognito.");
        cognitoUserSession = null;
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String getToken() {
        return null == cognitoUserSession ? null : cognitoUserSession.getIdToken().getJWTToken();
    }

    /** {@inheritDoc} */
    @Override
    public String refreshToken() {
        // If there is a session, but the credentials are expired rendering the session not valid.
        if ((cognitoUserSession != null) && !cognitoUserSession.isValid()) {
            // Attempt to refresh the credentials.
            final RefreshSessionAuthenticationHandler refreshSessionAuthenticationHandler
                = new RefreshSessionAuthenticationHandler();

            // Cognito User Pools SDK will attempt to refresh the token upon calling getSession().
            cognitoUserPool.getCurrentUser().getSession(refreshSessionAuthenticationHandler);

            if (null != refreshSessionAuthenticationHandler.getUserSession()) {
                cognitoUserSession = refreshSessionAuthenticationHandler.getUserSession();
            } else {
                LOG.log(Level.WARNING, "Could not refresh the Cognito User Pool Token.");
            }
        }

        return getToken();
    }

    /** {@inheritDoc} */
    @Override
    public void signOut() {
        if (null != cognitoUserPool && null != cognitoUserPool.getCurrentUser()) {
            LOG.log(Level.INFO, "Signing out");
            cognitoUserPool.getCurrentUser().signOut();

            cognitoUserSession = null;
            username = null;
            password = null;
        }
    }

    /**
     * @return the Cognito User Pool.
     */
    public CognitoUserPool getCognitoUserPool() {
        return cognitoUserPool;
    }

     /**
     * Retrieve the Cognito UserPool Id from CognitoUserPool -> PoolId key
     *
     * @throws IllegalArgumentException
     * @return CognitoUserPoolId
     */
    private String getCognitoUserPoolId() throws IllegalArgumentException {
        try {
            return this.awsConfiguration.optJsonObject("CognitoUserPool").getString("PoolId");
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Cannot find the PoolId from the " + AWS_CONFIGURATION_FILE + " file.", exception);
        }
    }

    /**
     * Retrieve the Cognito UserPool Id from CognitoUserPool -> AppClientId key
     *
     * @return CognitoUserPoolId
     * @throws IllegalArgumentException
     */
    private String getCognitoUserPoolClientId() throws IllegalArgumentException {
        try {
            return this.awsConfiguration.optJsonObject("CognitoUserPool").getString("AppClientId");
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Cannot find the CognitoUserPool AppClientId from the " + AWS_CONFIGURATION_FILE + " file.", exception);
        }
    }

    /**
     * Retrieve the Cognito UserPool ClientSecret from CognitoUserPool -> AppClientSecret key
     *
     * @return CognitoUserPoolAppClientSecret
     * @throws IllegalArgumentException
     */
    private String getCognitoUserPoolClientSecret() throws IllegalArgumentException {
        try {
            return this.awsConfiguration.optJsonObject("CognitoUserPool").getString("AppClientSecret");
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Cannot find the CognitoUserPool AppClientSecret from the " + AWS_CONFIGURATION_FILE + " file.", exception);
        }
    }

    /**
     * Retrieve the Cognito Region from CognitoUserPool -> Region key
     *
     * @return CognitoUserPool Region
     * @throws IllegalArgumentException
     */
    private String getCognitoUserPoolRegion() throws IllegalArgumentException {
        try {
            return this.awsConfiguration.optJsonObject("CognitoUserPool").getString("Region");
        } catch (Exception exception) {
            throw new IllegalArgumentException("Cannot find the CognitoUserPool Region from the "
                    + AWS_CONFIGURATION_FILE + " file.", exception);
        }
    }

    /**
     * Extract the error message from the exception object.
     * @param exception The exception object thrown by Cognito IdentityProvider.
     * */
    private static String getErrorMessageFromException(final Exception exception) {
        final String message = exception.getLocalizedMessage();
        if (message == null) {
            return exception.getMessage();
        }
        final int index = message.indexOf(USERPOOLS_EXCEPTION_PREFIX);
        if (index == -1) {
            return message;
        } else {
            return message.substring(0, index);
        }
    }

}
