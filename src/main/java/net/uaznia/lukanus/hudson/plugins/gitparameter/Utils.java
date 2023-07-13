package net.uaznia.lukanus.hudson.plugins.gitparameter;

import hudson.model.Job;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.TopLevelItem;
import java.util.List;
import jenkins.model.Jenkins;

public class Utils {

    public static Job getParentJob(final GitParameterDefinition gitParameterDefinition) {
        return Jenkins.get().getAllItems(Job.class).stream()
                .filter(j -> j instanceof TopLevelItem)
                .filter(j -> j.getProperty(ParametersDefinitionProperty.class) != null)
                .filter(j -> haveThisGitParameterDefinition(j, gitParameterDefinition))
                .findFirst()
                .orElse(null);
    }

    private static boolean haveThisGitParameterDefinition(
            final Job job, final GitParameterDefinition gitParameterDefinition) {
        ParametersDefinitionProperty property =
                (ParametersDefinitionProperty) job.getProperty(ParametersDefinitionProperty.class);
        List<ParameterDefinition> parameterDefinitions = property.getParameterDefinitions();

        if (parameterDefinitions != null) {
            return parameterDefinitions.stream()
                    .filter(pd -> pd instanceof GitParameterDefinition)
                    .anyMatch(pd -> ((GitParameterDefinition) pd).compareTo(gitParameterDefinition) == 0);
        }
        return false;
    }
}
