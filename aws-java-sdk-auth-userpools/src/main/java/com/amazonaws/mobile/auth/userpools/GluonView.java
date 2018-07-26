/**
 * Copyright (c) 2018 Gluon
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of Gluon, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL GLUON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.amazonaws.mobile.auth.userpools;

import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.application.ViewStackPolicy;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import java.util.Collection;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

public class GluonView extends View {

    static final Logger LOG = Logger.getLogger(GluonView.class.getName());

    private static final ResourceBundle BUNDLE;
    static {
        BUNDLE = ResourceBundle.getBundle("com/amazonaws/mobile/auth/userpools/userpools");
    }
    
    private final VBox center;
    
    public GluonView() {
        getStyleClass().add("aws");
        
        center = new VBox();
        center.getStyleClass().add("center");
        setCenter(center);
        
        setOnShowing(e -> showProgressBar(false));
    }

    final void addNodes(Collection<Node> nodes) {
        center.getChildren().addAll(nodes);
    }
    
    final void addNodes(Node... nodes) {
        center.getChildren().addAll(nodes);
    }

    @Override
    protected void updateAppBar(AppBar appBar) {
        appBar.setNavIcon(MaterialDesignIcon.CHEVRON_LEFT.button(e -> {
            getApplication().switchToPreviousView();
        }));
    }

    
    @Override
    public String getUserAgentStylesheet() {
        return GluonView.class.getResource("aws-style.css").toExternalForm();
    }
    
    static String getString(String value) {
        try {
            return BUNDLE.getString(value);
        } catch (MissingResourceException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        return "";
    }
    
    static void switchView(final String viewName) {
        if (Platform.isFxApplicationThread()) {
            MobileApplication.getInstance().switchView(viewName, ViewStackPolicy.SKIP);
        } else {
            Platform.runLater(() -> {
                MobileApplication.getInstance().switchView(viewName, ViewStackPolicy.SKIP);
            });
        }
    }
    
    static void showProgressBar(boolean value) {
        final AppBar appBar = MobileApplication.getInstance().getAppBar();
        if (Platform.isFxApplicationThread()) {
            appBar.setProgress(value ? ProgressBar.INDETERMINATE_PROGRESS : 0);
            appBar.setProgressBarVisible(value);
        } else {
            Platform.runLater(() -> {
                appBar.setProgress(value ? ProgressBar.INDETERMINATE_PROGRESS : 0);
                appBar.setProgressBarVisible(value);
            });
        }
    }
}
