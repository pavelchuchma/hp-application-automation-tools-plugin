/*
 * Certain versions of software and/or documents ("Material") accessible here may contain branding from
 * Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 * the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 * and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 * marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * (c) Copyright 2012-2021 Micro Focus or one of its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * ___________________________________________________________________
 */

package com.microfocus.application.automation.tools.sv.runner;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microfocus.application.automation.tools.sv.model.AbstractSvRunModel;
import com.microfocus.application.automation.tools.model.SvServerSettingsModel;
import com.microfocus.application.automation.tools.model.SvServiceSelectionModel;
import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;

public abstract class AbstractSvRunBuilder<T extends AbstractSvRunModel> extends Builder implements SimpleBuildStep {
    private static final Logger LOG = Logger.getLogger(AbstractSvRunBuilder.class.getName());

    protected final T model;

    protected AbstractSvRunBuilder(T model) {
        this.model = model;
    }

    protected static void verifyNotNull(Object value, String errorMessage) throws IllegalArgumentException {
        if (value == null) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    public T getModel() {
        return model;
    }

    public SvServiceSelectionModel getServiceSelection() {
        return model.getServiceSelection();
    }

    protected SvServerSettingsModel getSelectedServerSettings() throws IllegalArgumentException {
        SvServerSettingsModel[] servers = ((AbstractSvRunDescriptor) getDescriptor()).getServers();
        if (servers != null) {
            for (SvServerSettingsModel serverSettings : servers) {
                if (model.getServerName() != null && model.getServerName().equals(serverSettings.getName())) {
                    return serverSettings;
                }
            }
        }
        throw new IllegalArgumentException("Selected server configuration '" + model.getServerName() + "' does not exist.");
    }

    protected abstract AbstractSvRemoteRunner<T> getRemoteRunner(@Nonnull FilePath workspace, TaskListener listener, SvServerSettingsModel server);

        @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        PrintStream logger = listener.getLogger();
        Date startDate = new Date();
        try {
            SvServerSettingsModel serverModel = getSelectedServerSettings();

            logger.printf("%nStarting %s for SV Server '%s' (%s as %s, ignoreSslErrors=%s) on %s%n", getDescriptor().getDisplayName(),
                    serverModel.getName(), serverModel.getUrlObject(), serverModel.getUsername(), serverModel.isTrustEveryone(), startDate);
            logConfig(logger, "    ");
            validateServiceSelection();

            SvServerSettingsModel server = getSelectedServerSettings();
            AbstractSvRemoteRunner<T> runner = getRemoteRunner(workspace, listener, server);
            launcher.getChannel().call(runner);

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Build failed: " + e.getMessage(), e);
            throw new AbortException(e.getMessage());
        } finally {
            double duration = (new Date().getTime() - startDate.getTime()) / 1000.;
            logger.printf("Finished: %s in %.3f seconds%n%n", getDescriptor().getDisplayName(), duration);
        }
    }

    protected void logConfig(PrintStream logger, String prefix) {
        SvServiceSelectionModel ss = model.getServiceSelection();
        switch (ss.getSelectionType()) {
            case SERVICE:
                logger.println(prefix + "Service name or id: " + ss.getService());
                break;
            case PROJECT:
                logger.println(prefix + "Project path: " + ss.getProjectPath());
                logger.println(prefix + "Project password: " + ((StringUtils.isNotBlank(ss.getProjectPassword())) ? "*****" : null));
                break;
            case ALL_DEPLOYED:
                logger.println(prefix + "All deployed services");
                break;
            case DEPLOY:
                logger.println(prefix + "Project path: " + ss.getProjectPath());
                logger.println(prefix + "Project password: " + ((StringUtils.isNotBlank(ss.getProjectPassword())) ? "*****" : null));
                logger.println(prefix + "Service name or id: " + ss.getService());
                break;
        }
        logger.println(prefix + "Force: " + model.isForce());
    }

    protected void validateServiceSelection() throws IllegalArgumentException {
        SvServiceSelectionModel s = getServiceSelection();
        switch (s.getSelectionType()) {
            case SERVICE:
                verifyNotNull(s.getService(), "Service name or id must not be empty if service selection by name or id set");
                break;
            case PROJECT:
                verifyNotNull(s.getProjectPath(), "Project path must not be empty if service selection by project is set");
                break;
            case ALL_DEPLOYED:
                break;
            case DEPLOY:
                verifyNotNull(s.getProjectPath(), "Project path must not be empty for deployment");
                break;
        }
    }
}
