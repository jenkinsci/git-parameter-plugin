package net.uaznia.lukanus.hudson.plugins.gitparameter.jobs;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Job;
import hudson.model.Node;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.TaskListener;
import hudson.scm.SCM;
import java.io.IOException;
import java.util.List;

public interface JobWrapper {
    Job getJob();

    ParametersDefinitionProperty getProperty(Class<ParametersDefinitionProperty> parametersDefinitionPropertyClass);

    List<SCM> getScms();

    FilePath getSomeWorkspace() throws IOException, InterruptedException;

    EnvVars getEnvironment(Node node, TaskListener taskListener) throws IOException, InterruptedException;

    EnvVars getSomeBuildEnvironments();

    String getJobName();

    String getCustomJobName();
}
