package com.amazonaws.mobileconnectors.cognitoidentityprovider.util;

import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.SettingsService;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Handles shared user context with Amazon Pinpoint.
 */
public class CognitoPinpointSharedContext {
    private static final Log LOGGER = LogFactory.getLog(CognitoPinpointSharedContext.class);

    /**
     * Key to access Pinpoint endpoint id in {@link android.content.SharedPreferences}.
     */
    private static final String UNIQUE_ID_KEY = "UniqueId";

    /**
     * Pinpoint SharedPreferences file.
     */
    private static final String PREFERENCES_AND_FILE_MANAGER_SUFFIX
            = "515d6767-01b7-49e5-8273-c8d11b0f331d";

    /**
     * Returns the pinpoint endpoint id for the provided Pinpoint App Id.
     * @param pinpointAppId Required, the pinpoint appId.
     * @return The pinpoint endpoint id as a string.
     */
    public static String getPinpointEndpoint(String pinpointAppId) {
        return getPinpointEndpoint(pinpointAppId, UNIQUE_ID_KEY);
    }

    /**
     * Returns the pinpoint endpoint id for the provided Pinpoint App Id and user.
     * <p>
     *     Returns the pinpoint endpoint id for a the {@code pinpointAppId} and {@code pinpointEndpointIdentifier}.
     *     Generates and stores a new pinpoint endpoint id if a pinpoint endpoint id is not available for this
     *     combination.
     * </p>
     * @param pinpointAppId Required, the pinpoint appId.
     * @param pinpointEndpointIdentifier Required, the pinpoint user identifier.
     * @return The pinpoint endpoint id as a string.
     */
    public static String getPinpointEndpoint(String pinpointAppId,
                                             String pinpointEndpointIdentifier) {
        if (pinpointAppId == null || pinpointEndpointIdentifier == null) {
            return null;
        }

        try {
            SettingsService pinpointPreferences = Services.get(SettingsService.class)
                .orElseThrow(() -> {
                    throw new RuntimeException("Error accessing Settings Service");
            });
            String pinpointEndpointId = pinpointPreferences.retrieve(pinpointAppId + "." + 
                    PREFERENCES_AND_FILE_MANAGER_SUFFIX + "." + pinpointEndpointIdentifier);
            if (pinpointEndpointId == null) {
                pinpointEndpointId = UUID.randomUUID().toString();
                pinpointPreferences.store(pinpointAppId + "." + 
                    PREFERENCES_AND_FILE_MANAGER_SUFFIX + "." + pinpointEndpointIdentifier, pinpointEndpointId);
            }
            return pinpointEndpointId;
        } catch (Throwable e) {
            LOGGER.error("Error while reading from SharedPreferences", e);
            return null;
        }
    }
}
