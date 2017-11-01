package net.uaznia.lukanus.hudson.plugins.gitparameter.jobs;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Job;
import hudson.model.TaskListener;
import hudson.model.TopLevelItem;
import hudson.scm.SCM;
import jenkins.model.Jenkins;

public class WorkflowJobWrapper extends AbstractJobWrapper {
    private static final Logger LOGGER = Logger.getLogger(WorkflowJobWrapper.class.getName());

    public WorkflowJobWrapper(Job job) {
        super(job);
    }

    @Override
    public List<SCM> getScms() {
        List<SCM> scms = new ArrayList<>();

        SCM scmFromDefinition = getSCMFromDefinition();
        if (scmFromDefinition != null) {
            scms.add(scmFromDefinition);
        }

        Collection<? extends SCM> scmsFromLastBuild = getSCMsFromLastBuild();
        if (scmsFromLastBuild != null && !scmsFromLastBuild.isEmpty()) {
            scms.addAll(scmsFromLastBuild);
        }
        return scms;
    }

    private Collection<? extends SCM> getSCMsFromLastBuild() {
        try {
            Object scms = invokeGetMethodFromJob("getSCMs");
            if (scms != null && scms instanceof Collection) {
                return (Collection<? extends SCM>) scms;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, getCustomJobName() + " " + Messages.WorkflowJobWrapper_GetWorkflowRepoScmFail(), e);
        }
        return null;
    }


    private SCM getSCMFromDefinition() {
        try {
            Object definition = invokeGetMethodFromJob("getDefinition");

            Class<?> cpsScmFlowDefinitionClazz = definition.getClass();
            if (isNotCpsScmFlowDefinitionClass(cpsScmFlowDefinitionClazz)) {
                return null;
            }

            Method getScmMethod = cpsScmFlowDefinitionClazz.getMethod("getScm");
            Object scm = getScmMethod.invoke(definition);
            if (scm instanceof SCM) {
                return (SCM) scm;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, getCustomJobName() + " " + Messages.WorkflowJobWrapper_GetWorkflowRepoScmFail(), e);
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
    public EnvVars getSomeBuildEnvironments() {
        try {
            Object lastBuild = invokeGetMethodFromJob("getLastBuild");
            if (lastBuild != null) {
                Class<?> workflowRunClazz = lastBuild.getClass();

                Method getEnvironmentMethod = workflowRunClazz.getMethod("getEnvironment", TaskListener.class);
                Object envVars = getEnvironmentMethod.invoke(lastBuild, TaskListener.NULL);
                if (envVars instanceof EnvVars) {
                    return (EnvVars) envVars;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, getCustomJobName() + Messages.WorkflowJobWrapper_GetEnvironmentsFromWorkflowrun(), e);
        }
        return null;
    }

    private boolean isNotCpsScmFlowDefinitionClass(Class<?> clazz) {
        return !"org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition".equals(clazz.getName());
    }

    private Object invokeGetMethodFromJob(String methodInvoke) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Class<?> workflowJobClazz = getJob().getClass();
        Method getDefinitionMethod = workflowJobClazz.getDeclaredMethod(methodInvoke);
        return getDefinitionMethod.invoke(getJob());
    }
}
