package net.uaznia.lukanus.hudson.plugins.gitparameter.jobs;

import java.io.IOException;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Job;
import hudson.model.Node;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.TaskListener;
import hudson.scm.SCM;

public interface JobWrapper {
    Job getJob();

    ParametersDefinitionProperty getProperty(Class<ParametersDefinitionProperty> parametersDefinitionPropertyClass);

    SCM getScm();

    FilePath getSomeWorkspace() throws IOException, InterruptedException;

    EnvVars getEnvironment(Node node, TaskListener taskListener) throws IOException, InterruptedException;

    AbstractBuild getSomeBuildWithWorkspace();

    int getNextBuildNumber();

    void updateNextBuildNumber(int nextBuildNumber) throws IOException;
}
