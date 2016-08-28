package net.uaznia.lukanus.hudson.plugins.gitparameter.jobs;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.scm.SCM;

public class AbstractProjectJobWrapper extends AbstractJobWrapper {
    public AbstractProjectJobWrapper(Job job) {
        super(job);
    }

    @Override
    public SCM getScm() {
        return ((AbstractProject) getJob()).getScm();
    }

    @Override
    public FilePath getSomeWorkspace() {
        return ((AbstractProject) getJob()).getSomeWorkspace();
    }

    @Override
    public AbstractBuild getSomeBuildWithWorkspace() {
        return ((AbstractProject) getJob()).getSomeBuildWithWorkspace();
    }
}
