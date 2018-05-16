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

package com.amazonaws.mobile.auth.core.internal.util;

import java.util.Optional;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/**
 * Utilities for Views.
 *
 */
public final class ViewHelper {
    /**
     * Displays a modal dialog with an OK button.
     *
     * @param title title to display for the dialog
     * @param body content of the dialog
     */
    public static void showDialog(final String title, final String body) {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(body);
        alert.show();
    }

    /**
     * Displays a modal dialog.
     *
     * @param title title to display for the dialog
     * @param body content of the dialog
     * @param positiveButton String for positive button
     * @param negativeButton String for negative button
     * @param negativeButtonListener  the listener which should be invoked when a negative button is pressed
     * @param positiveButtonListener  the listener which should be invoked when a positive button is pressed
     */
    public static void showDialog(final String title,
                                  final String body,
                                  final String positiveButton, 
                                  final Runnable positiveButtonListener,
                                  final String negativeButton,
                                  final Runnable negativeButtonListener) {

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setContentText(body);
        ButtonType yesButton = new ButtonType("Yes");
        ButtonType noButton = new ButtonType("No");
        alert.getButtonTypes().setAll(yesButton, noButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == yesButton) {
            positiveButtonListener.run();
        } else {
            negativeButtonListener.run();
        }
    }
}
