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

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import com.microfocus.application.automation.tools.sv.model.AbstractSvRunModel;
import com.microfocus.application.automation.tools.model.SvServerSettingsModel;
import com.microfocus.application.automation.tools.model.SvServiceSelectionModel;
import com.microfocus.sv.svconfigurator.build.ProjectBuilder;
import com.microfocus.sv.svconfigurator.core.IProject;
import com.microfocus.sv.svconfigurator.core.IService;
import com.microfocus.sv.svconfigurator.core.impl.exception.CommandExecutorException;
import com.microfocus.sv.svconfigurator.core.impl.exception.CommunicatorException;
import com.microfocus.sv.svconfigurator.core.impl.exception.ProjectBuilderException;
import com.microfocus.sv.svconfigurator.core.impl.jaxb.atom.ServiceListAtom;
import com.microfocus.sv.svconfigurator.serverclient.ICommandExecutor;
import com.microfocus.sv.svconfigurator.serverclient.impl.CommandExecutorFactory;
import hudson.FilePath;
import hudson.model.TaskListener;
import jenkins.security.MasterToSlaveCallable;

public abstract class AbstractSvRemoteRunner<T extends AbstractSvRunModel> extends MasterToSlaveCallable<String, Exception> {
    protected T model;
    protected FilePath workspace;
    protected TaskListener listener;
    protected SvServerSettingsModel server;

    public AbstractSvRemoteRunner(TaskListener listener, T model, FilePath workspace, SvServerSettingsModel server) {
        this.listener = listener;
        this.model = model;
        this.workspace = workspace;
        this.server = server;
    }

    protected List<ServiceInfo> getServiceList(boolean ignoreMissingServices, PrintStream logger, FilePath workspace) throws Exception {
        SvServiceSelectionModel s = getServiceSelection();
        ICommandExecutor exec = createCommandExecutor();

        ArrayList<ServiceInfo> res = new ArrayList<>();

        switch (s.getSelectionType()) {
            case SERVICE:
                addServiceIfDeployed(s.getService(), res, ignoreMissingServices, exec, logger);
                break;
            case PROJECT:
                IProject project = loadProject(workspace);
                for (IService svc : project.getServices()) {
                    addServiceIfDeployed(svc.getId(), res, ignoreMissingServices, exec, logger);
                }
                break;
            case ALL_DEPLOYED:
                for (ServiceListAtom.ServiceEntry entry : exec.getServiceList(null).getEntries()) {
                    res.add(new ServiceInfo(entry.getId(), entry.getTitle()));
                }
                break;
            case DEPLOY:
                break;
            default:
                throw new IllegalArgumentException();
        }
        return res;
    }

    protected IProject loadProject(FilePath workspace) throws ProjectBuilderException {
        SvServiceSelectionModel s = getServiceSelection();
        FilePath projectPath = workspace.child(s.getProjectPath());
        return new ProjectBuilder().buildProject(new File(projectPath.getRemote()), s.getProjectPassword());
    }

    public SvServiceSelectionModel getServiceSelection() {
        return model.getServiceSelection();
    }

    private void addServiceIfDeployed(String service, ArrayList<ServiceInfo> results, boolean ignoreMissingServices,
                                  ICommandExecutor exec, PrintStream logger) throws CommunicatorException, CommandExecutorException {
        try {
            IService svc = exec.findService(service, null);
            results.add(new ServiceInfo(svc.getId(), svc.getName()));
        } catch (CommandExecutorException e) {
            if (!ignoreMissingServices) {
                throw e;
            }
            logger.printf("Service '%s' is not deployed, ignoring%n", service);
        }
    }

    protected ICommandExecutor createCommandExecutor() throws Exception {
        return new CommandExecutorFactory()
                .createCommandExecutor(server.getUrlObject(), server.isTrustEveryone(), server.getCredentials());
    }
}
