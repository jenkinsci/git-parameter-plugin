package net.uaznia.lukanus.hudson.plugins.gitparameter.jobs;


import java.io.IOException;

import hudson.EnvVars;
import hudson.model.Job;
import hudson.model.Node;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.TaskListener;

public abstract class AbstractJobWrapper implements JobWrapper {
    private Job job;

    public AbstractJobWrapper(Job job) {
        this.job = job;
    }

    @Override
    public Job getJob() {
        return job;
    }

    @Override
    public ParametersDefinitionProperty getProperty(Class<ParametersDefinitionProperty> propertyClass) {
        return (ParametersDefinitionProperty) job.getProperty(propertyClass);
    }

    @Override
    public EnvVars getEnvironment(Node node, TaskListener taskListener) throws IOException, InterruptedException {
        return job.getEnvironment(node, taskListener);
    }

    @Override
    public int getNextBuildNumber() {
        return job.getNextBuildNumber();
    }

    @Override
    public void updateNextBuildNumber(int nextBuildNumber) throws IOException {
        job.updateNextBuildNumber(nextBuildNumber);
    }

    @Override
    public String getJobName() {
        return job.getFullName();
    }

    @Override
    public String getCustomJobName() {
        return "[ " + getJobName() + " ] ";
    }
}
