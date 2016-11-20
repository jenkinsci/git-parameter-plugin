package net.uaznia.lukanus.hudson.plugins.gitparameter.jobs;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.model.TaskListener;
import hudson.scm.SCM;

public class AbstractProjectJobWrapper extends AbstractJobWrapper {
    private static final Logger LOGGER = Logger.getLogger(AbstractProjectJobWrapper.class.getName());

    public AbstractProjectJobWrapper(Job job) {
        super(job);
    }

    @Override
    public List<SCM> getScms() {
        return Arrays.asList(((AbstractProject) getJob()).getScm());
    }

    @Override
    public FilePath getSomeWorkspace() {
        return ((AbstractProject) getJob()).getSomeWorkspace();
    }

    @Override
    public EnvVars getSomeBuildEnvironments() {
        try {
            AbstractBuild someBuildWithWorkspace = ((AbstractProject) getJob()).getSomeBuildWithWorkspace();
            if (someBuildWithWorkspace != null) {
                return someBuildWithWorkspace.getEnvironment(TaskListener.NULL);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, Messages.AbstractProjectJobWrapper_GetEnvironmentsFromAbstractProject(), e);
        }
        return null;
    }
}
