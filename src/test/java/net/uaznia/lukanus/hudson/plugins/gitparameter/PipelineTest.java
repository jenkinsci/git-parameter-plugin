package net.uaznia.lukanus.hudson.plugins.gitparameter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jenkins.model.Jenkins;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.pipeline.modeldefinition.actions.DeclarativeJobPropertyTrackerAction;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class PipelineTest {
    private JenkinsRule r;

    @BeforeEach
    void initializeJenkinsRule(JenkinsRule rule) {
        // Make the rule available to all tests in the class
        r = rule;
    }

    private WorkflowJob defineProject(String defaultValue) throws Exception {
        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
        r.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.ADMINISTER)
                .everywhere()
                .to("git-parameter-admin"));
        WorkflowJob project = r.createProject(WorkflowJob.class);
        String pipeline = """
                pipeline {
                  agent any
                  parameters {
                    gitParameter type: 'PT_TAG',
                                 name: 'A_TAG',
                                 defaultValue: '%s',
                                 description: 'Choose a tag to checkout',
                                 selectedValue: 'TOP',
                                 sortMode: 'DESCENDING_SMART',
                                 tagFilter: 'git-client-6*'
                  }
                  stages {
                    stage('checkout') {
                      steps {
                        checkout scmGit(branches: [[name: params.A_TAG]],
                                        extensions: [cloneOption(depth: 1)],
                                        userRemoteConfigs: [[url: 'https://github.com/jenkinsci/git-client-plugin.git']])
                      }
                    }
                  }
                }
                """;
        project.setDefinition(new CpsFlowDefinition(pipeline.formatted(defaultValue), true));
        return project;
    }

    @Test
    void createPipelineProject() throws Exception {
        WorkflowJob project = defineProject("git-client-6.0.0"); // valid default value

        // Project has not run, parameters not yet defined
        List<DeclarativeJobPropertyTrackerAction> actions =
                project.getActions(DeclarativeJobPropertyTrackerAction.class);
        assertThat(actions, is(empty()));

        // Run the project and confirm default value is used for the branch
        WorkflowRun b1 = r.buildAndAssertSuccess(project);
        r.assertLogContains("git-client-6.0.0", b1);

        // Project has run, parameters are defined
        actions = project.getActions(DeclarativeJobPropertyTrackerAction.class);
        assertThat(actions, is(not(empty())));
        assertThat(actions.get(0).getParameters(), hasItem("A_TAG"));

        // Use Jenkins command line to start job with valid parameter value
        runJobWithParameter(project, "A_TAG", "git-client-6.1.1", "");
        WorkflowRun b2 = project.getLastBuild();
        assertThat("Second build did not run", b2.number, is(2)); // Build ran as expected
        r.assertLogContains("git-client-6.1.1", b2);

        // Use Jenkins command line to attempt to start job with invalid parameter value
        runJobWithParameter(project, "A_TAG", "bad-tag", "Parameter A_TAG value 'bad-tag' is invalid");
        assertThat("Build ran unexpectedly", project.getLastBuild().number, is(2)); // Build did not run
    }

    @Test
    void createPipelineProjectWithInvalidDefaultValue() throws Exception {
        WorkflowJob project = defineProject("git-client-4.0.0"); // invalid default value

        // Project has not run, parameters not yet defined
        List<DeclarativeJobPropertyTrackerAction> actions =
                project.getActions(DeclarativeJobPropertyTrackerAction.class);
        assertThat(actions, is(empty()));

        // Run the project and confirm default value is used for the branch
        WorkflowRun b1 = r.buildAndAssertSuccess(project);
        r.assertLogContains("git-client-4.0.0", b1); // Passes even though it is an invalid default value

        // Use Jenkins command line to start job with valid parameter value
        runJobWithParameter(project, "A_TAG", "git-client-6.1.1", "");
        WorkflowRun b2 = project.getLastBuild();
        assertThat("Second build did not run", b2.number, is(2)); // Build ran as expected
        r.assertLogContains("git-client-6.1.1", b2);

        // Use Jenkins command line to attempt to start job with invalid parameter value
        runJobWithParameter(project, "A_TAG", "bad-tag", "Parameter A_TAG value 'bad-tag' is invalid");
        assertThat("Build ran unexpectedly", project.getLastBuild().number, is(2)); // Build did not run
    }

    private void runJobWithParameter(WorkflowJob project, String name, String value, String expectedOutput)
            throws Exception {
        File jar = downloadCommandLineJar();
        File output = Files.createTempFile("cli-output", ".txt").toFile();
        List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList(
                "java", "-jar", jar.getAbsolutePath(), "-s", r.getURL().toString()));
        args.addAll(Arrays.asList("-auth", "git-parameter-admin:git-parameter-admin"));
        args.addAll(Arrays.asList("build", project.getName(), "-p", name + "=" + value));
        ProcessBuilder pb = new ProcessBuilder(args);
        pb.redirectErrorStream(true);
        pb.redirectOutput(output);
        Process process = pb.start();
        int exitCode = process.waitFor();
        String content = Files.readString(output.toPath());
        Files.delete(jar.toPath()); // Delete before assertions
        Files.delete(output.toPath());
        if (expectedOutput != null && !expectedOutput.isEmpty()) {
            assertThat(content, containsString(expectedOutput));
        }
        if (exitCode == 0) {
            r.waitForCompletion(project.getLastBuild());
        }
    }

    private File downloadCommandLineJar() throws Exception {
        File jar = Files.createTempFile("jenkins-cli", ".jar").toFile();
        FileUtils.copyURLToFile(r.jenkins.getJnlpJars("jenkins-cli.jar").getURL(), jar);
        return jar;
    }
}
