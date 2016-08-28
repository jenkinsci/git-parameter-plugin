package net.uaznia.lukanus.hudson.plugins.gitparameter.jobs;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Job;
import hudson.model.TopLevelItem;
import hudson.scm.SCM;
import jenkins.model.Jenkins;
import net.uaznia.lukanus.hudson.plugins.gitparameter.GitParameterDefinition;

public class WorkflowJobWrapper extends AbstractJobWrapper {
    private static final Logger LOGGER = Logger.getLogger(WorkflowJobWrapper.class.getName());

    public WorkflowJobWrapper(Job job) {
        super(job);
    }

    @Override
    public SCM getScm() {
        try {
            Class<?> workflowJobClazz = getJob().getClass();
            Method getDefinitionMethod = workflowJobClazz.getDeclaredMethod("getDefinition");
            Object definition = getDefinitionMethod.invoke(getJob());

            Class<?> cpsScmFlowDefinitionClazz = definition.getClass();
            Method getScmMethod = cpsScmFlowDefinitionClazz.getMethod("getScm");

            Object scm = getScmMethod.invoke(definition);
            if (scm instanceof SCM) {
                return (SCM) scm;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, Messages.WorkflowJobWrapper_GetWorkflowRepoScmFail(), e);
        }
        return null;
    }

    @Override
    public FilePath getSomeWorkspace() throws IOException, InterruptedException {
        FilePath workspaceForWorkflow = Jenkins.getInstance().getWorkspaceFor((TopLevelItem) getJob()).withSuffix("@script");
        if (workspaceForWorkflow.exists()) {
            return workspaceForWorkflow;
        }
        return null;
    }

    @Override
    public AbstractBuild getSomeBuildWithWorkspace() {
        return null; //TODO Add implementation, perhaps is not necessary
    }
}
