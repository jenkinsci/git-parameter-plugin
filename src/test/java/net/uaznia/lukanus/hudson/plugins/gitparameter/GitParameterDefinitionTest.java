package net.uaznia.lukanus.hudson.plugins.gitparameter;

import static net.uaznia.lukanus.hudson.plugins.gitparameter.Constants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hudson.EnvVars;
import hudson.Functions;
import hudson.cli.CLICommand;
import hudson.cli.ConsoleCommand;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Failure;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.ParameterValue;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Result;
import hudson.model.StringParameterDefinition;
import hudson.model.queue.QueueTaskFuture;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.UserRemoteConfig;
import hudson.plugins.git.extensions.impl.RelativeTargetDirectory;
import hudson.plugins.templateproject.ProxySCM;
import hudson.scm.SCM;
import hudson.tasks.BatchFile;
import hudson.tasks.Shell;
import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.sf.json.JSONObject;
import net.uaznia.lukanus.hudson.plugins.gitparameter.GitParameterDefinition.DescriptorImpl;
import net.uaznia.lukanus.hudson.plugins.gitparameter.jobs.JobWrapper;
import net.uaznia.lukanus.hudson.plugins.gitparameter.jobs.JobWrapperFactory;
import net.uaznia.lukanus.hudson.plugins.gitparameter.model.ItemsErrorModel;
import net.uaznia.lukanus.hudson.plugins.gitparameter.scms.SCMFactory;
import org.jenkinsci.plugins.multiplescms.MultiSCM;
import org.jenkinsci.plugins.structs.SymbolLookup;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockFolder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.StaplerRequest2;

/**
 * @author lukanus
 */
@WithJenkins
class GitParameterDefinitionTest {

    // Test Descriptor.getProjectSCM()
    @Test
    void testGetProjectSCM(JenkinsRule jenkins) throws Exception {
        FreeStyleProject testJob = jenkins.createFreeStyleProject("testGetProjectSCM");
        GitSCM git = new GitSCM(GIT_PARAMETER_REPOSITORY_URL);
        GitParameterDefinition def = new GitParameterDefinition(
                "testName",
                PT_REVISION,
                "testDefaultValue",
                "testDescription",
                "testBranch",
                ".*",
                "*",
                SortMode.NONE,
                SelectedValue.NONE,
                null,
                false);
        testJob.addProperty(new ParametersDefinitionProperty(def));
        JobWrapper jobWrapper = JobWrapperFactory.createJobWrapper(testJob);
        assertTrue(SCMFactory.getGitSCMs(jobWrapper, null).isEmpty());

        testJob.setScm(git);
        assertEquals(git, SCMFactory.getGitSCMs(jobWrapper, null).get(0));
    }

    @Test
    void testDoFillValueItems_withoutSCM(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("testListTags");
        GitParameterDefinition def = new GitParameterDefinition(
                "testName",
                "PT_TAG",
                "testDefaultValue",
                "testDescription",
                "testBranch",
                ".*",
                "*",
                SortMode.NONE,
                SelectedValue.NONE,
                null,
                false);
        project.addProperty(new ParametersDefinitionProperty(def));

        ItemsErrorModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertEquals(3, items.getErrors().size());
        assertEquals("The default value has been returned", items.getErrors().get(0));
        assertEquals(
                "No Git repository configured in SCM configuration or plugin is configured wrong",
                items.getErrors().get(1));
        assertEquals("Please check the configuration", items.getErrors().get(2));
        assertTrue(isListBoxItem(items, def.getDefaultValue()));
    }

    @Test
    void testDoFillValueItems_listTags(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("testListTags");
        project.getBuildersList().add(Functions.isWindows() ? new BatchFile("echo test") : new Shell("echo test"));
        setupGit(project);

        GitParameterDefinition def = new GitParameterDefinition(
                "testName",
                "PT_TAG",
                "testDefaultValue",
                "testDescription",
                "testBranch",
                ".*",
                "*",
                SortMode.NONE,
                SelectedValue.NONE,
                null,
                false);
        project.addProperty(new ParametersDefinitionProperty(def));

        // Run the build once to get the workspace
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        assertNotNull(build);
        ItemsErrorModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "git-parameter-0.2"));
    }

    @Test
    void testGetListBranchNoBuildProject(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("testListTags");
        project.getBuildersList().add(Functions.isWindows() ? new BatchFile("echo test") : new Shell("echo test"));
        setupGit(project);

        GitParameterDefinition def = new GitParameterDefinition(
                "testName",
                "PT_BRANCH",
                "testDefaultValue",
                "testDescription",
                "testBranch",
                ".*",
                "*",
                SortMode.NONE,
                SelectedValue.NONE,
                null,
                false);
        project.addProperty(new ParametersDefinitionProperty(def));

        ItemsErrorModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "master"));
    }

    @Test
    void testGetListBranchAfterWipeOutWorkspace(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("testListTags");
        project.getBuildersList().add(Functions.isWindows() ? new BatchFile("echo test") : new Shell("echo test"));
        setupGit(project);

        GitParameterDefinition def = new GitParameterDefinition(
                "testName",
                "PT_BRANCH",
                null,
                "testDescription",
                "testBranch",
                ".*",
                "*",
                SortMode.NONE,
                SelectedValue.NONE,
                null,
                false);
        project.addProperty(new ParametersDefinitionProperty(def));
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        assertNotNull(build);
        // build.run();
        project.doDoWipeOutWorkspace();
        ItemsErrorModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "master"));
    }

    @Test
    void testDoFillValueItems_listBranches(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("testListTags");
        project.getBuildersList().add(Functions.isWindows() ? new BatchFile("echo test") : new Shell("echo test"));
        setupGit(project);

        GitParameterDefinition def = new GitParameterDefinition(
                "testName",
                "PT_BRANCH",
                "testDefaultValue",
                "testDescription",
                "testBranch",
                ".*",
                "*",
                SortMode.NONE,
                SelectedValue.NONE,
                null,
                false);
        project.addProperty(new ParametersDefinitionProperty(def));

        // Run the build once to get the workspace
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        assertNotNull(build);
        ItemsErrorModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "master"));
    }

    @Test
    void tesListBranchesWrongBranchFilter(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("testListTags");
        project.getBuildersList().add(Functions.isWindows() ? new BatchFile("echo test") : new Shell("echo test"));
        setupGit(project);

        GitParameterDefinition def = new GitParameterDefinition(
                "testName",
                "PT_BRANCH",
                "testDefaultValue",
                "testDescription",
                "testBranch",
                "[*",
                "*",
                SortMode.NONE,
                SelectedValue.NONE,
                null,
                false);
        project.addProperty(new ParametersDefinitionProperty(def));

        ItemsErrorModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "master"));
    }

    @Test
    void testDoFillValueItems_listBranches_withRegexGroup(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("testListTags");
        project.getBuildersList().add(Functions.isWindows() ? new BatchFile("echo test") : new Shell("echo test"));
        setupGit(project);

        GitParameterDefinition def = new GitParameterDefinition(
                "testName",
                "PT_BRANCH",
                "testDefaultValue",
                "testDescription",
                "testBranch",
                "origin/(.*)",
                "*",
                SortMode.NONE,
                SelectedValue.NONE,
                null,
                false);
        project.addProperty(new ParametersDefinitionProperty(def));

        // Run the build once to get the workspace
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        assertNotNull(build);
        ItemsErrorModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "master"));
        assertFalse(isListBoxItem(items, "origin/master"));
    }

    @Test
    void testSortAscendingTag(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("testListTags");
        project.getBuildersList().add(Functions.isWindows() ? new BatchFile("echo test") : new Shell("echo test"));
        setupGit(project);

        GitParameterDefinition def = new GitParameterDefinition(
                "testName",
                "PT_TAG",
                "testDefaultValue",
                "testDescription",
                null,
                ".*",
                "*",
                SortMode.ASCENDING,
                SelectedValue.NONE,
                null,
                false);
        project.addProperty(new ParametersDefinitionProperty(def));

        ItemsErrorModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertEquals("0.1", items.get(0).value);
    }

    @Test
    void testWrongTagFilter(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("testListTags");
        project.getBuildersList().add(Functions.isWindows() ? new BatchFile("echo test") : new Shell("echo test"));
        setupGit(project);

        GitParameterDefinition def = new GitParameterDefinition(
                "testName",
                "PT_TAG",
                "testDefaultValue",
                "testDescription",
                null,
                ".*",
                "wrongTagFilter",
                SortMode.ASCENDING,
                SelectedValue.NONE,
                null,
                false);
        project.addProperty(new ParametersDefinitionProperty(def));

        ItemsErrorModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(items.isEmpty());
    }

    @Test
    void testSortDescendingTag(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("testListTags");
        project.getBuildersList().add(Functions.isWindows() ? new BatchFile("echo test") : new Shell("echo test"));
        setupGit(project);

        GitParameterDefinition def = new GitParameterDefinition(
                "testName",
                "PT_TAG",
                "testDefaultValue",
                "testDescription",
                null,
                ".*",
                "*",
                SortMode.DESCENDING,
                SelectedValue.NONE,
                null,
                false);
        project.addProperty(new ParametersDefinitionProperty(def));

        ItemsErrorModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertEquals("0.1", items.get(items.size() - 1).value);
    }

    @Test
    void testDoFillValueItems_listTagsAndBranches(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("testListTags");
        project.getBuildersList().add(Functions.isWindows() ? new BatchFile("echo test") : new Shell("echo test"));
        setupGit(project);

        GitParameterDefinition def = new GitParameterDefinition(
                "testName",
                "PT_BRANCH_TAG",
                "testDefaultValue",
                "testDescription",
                "testBranch",
                ".*",
                "*",
                SortMode.NONE,
                SelectedValue.NONE,
                null,
                false);
        project.addProperty(new ParametersDefinitionProperty(def));

        // Run the build once to get the workspace
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        assertNotNull(build);
        ItemsErrorModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "master"));
        assertTrue(isListBoxItem(items, "git-parameter-0.2"));
    }

    @Test
    void testDoFillValueItems_listRevisions(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("testListRevisions");
        project.getBuildersList().add(Functions.isWindows() ? new BatchFile("echo test") : new Shell("echo test"));
        setupGit(project);

        GitParameterDefinition def = new GitParameterDefinition(
                "testName",
                PT_REVISION,
                "testDefaultValue",
                "testDescription",
                null,
                ".*",
                "*",
                SortMode.NONE,
                SelectedValue.NONE,
                null,
                false);

        project.addProperty(new ParametersDefinitionProperty(def));

        // Run the build once to get the workspace
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        assertNotNull(build);
        ItemsErrorModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "00a8385cba1e4e32cf823775e2b3dbe5eb27931d"));
        assertTrue(isListBoxItemName(
                items, "00a8385c 2011-10-30 17:11 Łukasz Miłkowski <lukanus@uaznia.net> initial readme"));
    }

    @Test
    void testDoFillValueItems_listRevisionsWithBranch(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("testListRevisions");
        project.getBuildersList().add(Functions.isWindows() ? new BatchFile("echo test") : new Shell("echo test"));
        setupGit(project);
        GitParameterDefinition def = new GitParameterDefinition(
                "testName",
                PT_REVISION,
                "testDefaultValue",
                "testDescription",
                "origin/test/my-branch",
                ".*",
                "*",
                SortMode.NONE,
                SelectedValue.NONE,
                null,
                false);

        project.addProperty(new ParametersDefinitionProperty(def));

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        assertNotNull(build);
        ItemsErrorModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "00a8385c"));
    }

    @Test
    void testDoFillValueItems_listPullRequests(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("testListPullRequests");
        project.getBuildersList().add(Functions.isWindows() ? new BatchFile("echo test") : new Shell("echo test"));
        setupGit(project);
        GitParameterDefinition def = new GitParameterDefinition(
                "testName",
                Consts.PARAMETER_TYPE_PULL_REQUEST,
                "master",
                "testDescription",
                "",
                ".*",
                "*",
                SortMode.NONE,
                SelectedValue.NONE,
                null,
                false);

        project.addProperty(new ParametersDefinitionProperty(def));

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        assertNotNull(build);
        ItemsErrorModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "41"));
        assertTrue(isListBoxItem(items, "44"));
    }

    @Test
    void testSearchInFolders(JenkinsRule jenkins) throws Exception {
        MockFolder folder = jenkins.createFolder("folder");
        FreeStyleProject job1 = folder.createProject(FreeStyleProject.class, "job1");

        GitParameterDefinition gitParameterDefinition = new GitParameterDefinition(
                NAME,
                "asdf",
                "other",
                "description",
                "branch",
                ".*",
                "*",
                SortMode.NONE,
                SelectedValue.NONE,
                null,
                false);
        job1.addProperty(new ParametersDefinitionProperty(gitParameterDefinition));
        assertEquals("folder/job1", Utils.getParentJob(gitParameterDefinition).getFullName());
    }

    @Test
    void testBranchFilterValidation(JenkinsRule jenkins) {
        final DescriptorImpl descriptor = new DescriptorImpl();
        final FormValidation okWildcard = descriptor.doCheckBranchFilter(".*");
        final FormValidation badWildcard = descriptor.doCheckBranchFilter(".**");

        assertSame(Kind.OK, okWildcard.kind);
        assertSame(Kind.ERROR, badWildcard.kind);
    }

    @Test
    void testUseRepositoryValidation(JenkinsRule jenkins) {
        final DescriptorImpl descriptor = new DescriptorImpl();
        final FormValidation okWildcard = descriptor.doCheckUseRepository(".*");
        final FormValidation badWildcard = descriptor.doCheckUseRepository(".**");

        assertSame(Kind.OK, okWildcard.kind);
        assertSame(Kind.ERROR, badWildcard.kind);
    }

    @Test
    void testDefaultValueIsRequired(JenkinsRule jenkins) {
        final DescriptorImpl descriptor = new DescriptorImpl();
        final FormValidation okDefaultValue = descriptor.doCheckDefaultValue("origin/master", false);
        final FormValidation badDefaultValue = descriptor.doCheckDefaultValue(null, false);
        final FormValidation badDefaultValue_2 = descriptor.doCheckDefaultValue("  ", false);

        assertSame(Kind.OK, okDefaultValue.kind);
        assertSame(Kind.WARNING, badDefaultValue.kind);
        assertSame(Kind.WARNING, badDefaultValue_2.kind);
    }

    @Test
    void testDefaultValueIsNotRequired(JenkinsRule jenkins) {
        final DescriptorImpl descriptor = new DescriptorImpl();
        final FormValidation okDefaultValue = descriptor.doCheckDefaultValue("origin/master", true);
        final FormValidation badDefaultValue = descriptor.doCheckDefaultValue(null, true);
        final FormValidation badDefaultValue_2 = descriptor.doCheckDefaultValue("  ", true);

        assertSame(Kind.WARNING, okDefaultValue.kind);
        assertSame(Kind.OK, badDefaultValue.kind);
        assertSame(Kind.OK, badDefaultValue_2.kind);
    }

    @Test
    void testGetDefaultValueWhenDefaultValueIsSet(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("testDefaultValue");
        project.getBuildersList().add(Functions.isWindows() ? new BatchFile("echo test") : new Shell("echo test"));
        setupGit(project);
        String testDefaultValue = "testDefaultValue";
        GitParameterDefinition def = new GitParameterDefinition(
                "testName",
                Consts.PARAMETER_TYPE_TAG,
                testDefaultValue,
                "testDescription",
                null,
                ".*",
                "*",
                SortMode.ASCENDING,
                SelectedValue.TOP,
                null,
                false);

        project.addProperty(new ParametersDefinitionProperty(def));
        assertNotNull(def.getDefaultParameterValue());
        assertEquals(testDefaultValue, def.getDefaultParameterValue().getValue());
    }

    @Test
    void testGetDefaultValueAsTop(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("testDefaultValueAsTOP");
        project.getBuildersList().add(Functions.isWindows() ? new BatchFile("echo test") : new Shell("echo test"));
        setupGit(project);
        GitParameterDefinition def = new GitParameterDefinition(
                "testName",
                Consts.PARAMETER_TYPE_TAG,
                null,
                "testDescription",
                null,
                ".*",
                "*",
                SortMode.ASCENDING,
                SelectedValue.TOP,
                null,
                false);

        project.addProperty(new ParametersDefinitionProperty(def));
        assertNotNull(def.getDefaultParameterValue());
        assertEquals("0.1", def.getDefaultParameterValue().getValue());
    }

    @Test
    void testGlobalVariableRepositoryUrl(JenkinsRule jenkins) throws Exception {
        EnvVars.masterEnvVars.put("GIT_REPO_URL", GIT_PARAMETER_REPOSITORY_URL);
        FreeStyleProject project = jenkins.createFreeStyleProject("testGlobalValue");
        project.getBuildersList().add(Functions.isWindows() ? new BatchFile("echo test") : new Shell("echo test"));
        setupGit(project, "$GIT_REPO_URL");
        GitParameterDefinition def = new GitParameterDefinition(
                "testName",
                Consts.PARAMETER_TYPE_BRANCH,
                null,
                "testDescription",
                null,
                ".*master.*",
                "*",
                SortMode.ASCENDING,
                SelectedValue.NONE,
                null,
                false);

        project.addProperty(new ParametersDefinitionProperty(def));
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        assertEquals(Result.SUCCESS, build.getResult());
        ItemsErrorModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "origin/master"));
    }

    @Test
    void testRequiredParameterStaplerFail(JenkinsRule jenkins) {
        GitParameterDefinition def = new GitParameterDefinition(
                "testName",
                Consts.PARAMETER_TYPE_BRANCH,
                null,
                "testDescription",
                null,
                ".*master.*",
                "*",
                SortMode.ASCENDING,
                SelectedValue.NONE,
                null,
                false);
        def.setRequiredParameter(true);
        StaplerRequest2 request = mock(StaplerRequest2.class);
        String[] result = new String[] {""};
        when(request.getParameterValues("testName")).thenReturn(result);
        assertThrows(Failure.class, () -> def.createValue(request));
    }

    @Test
    void testRequiredParameterStaplerPass(JenkinsRule jenkins) {
        GitParameterDefinition def = new GitParameterDefinition(
                "testName",
                Consts.PARAMETER_TYPE_BRANCH,
                null,
                "testDescription",
                null,
                ".*master.*",
                "*",
                SortMode.ASCENDING,
                SelectedValue.NONE,
                null,
                false);
        def.setRequiredParameter(true);
        StaplerRequest2 request = mock(StaplerRequest2.class);
        String[] result = new String[] {"master"};
        when(request.getParameterValues("testName")).thenReturn(result);
        def.createValue(request);
    }

    @Test
    void testRequiredParameterJSONFail(JenkinsRule jenkins) {
        GitParameterDefinition def = new GitParameterDefinition(
                "testName",
                Consts.PARAMETER_TYPE_BRANCH,
                null,
                "testDescription",
                null,
                ".*master.*",
                "*",
                SortMode.ASCENDING,
                SelectedValue.NONE,
                null,
                false);
        def.setRequiredParameter(true);
        JSONObject o = new JSONObject();
        o.put("value", "");
        assertThrows(Failure.class, () -> def.createValue((StaplerRequest2) null, o));
    }

    @Test
    void testRequiredParameterJSONPass(JenkinsRule jenkins) {
        GitParameterDefinition def = new GitParameterDefinition(
                "testName",
                Consts.PARAMETER_TYPE_BRANCH,
                null,
                "testDescription",
                null,
                ".*master.*",
                "*",
                SortMode.ASCENDING,
                SelectedValue.NONE,
                null,
                false);
        def.setRequiredParameter(true);
        JSONObject o = new JSONObject();
        o.put("value", "master");
        o.put("name", "testName");
        def.createValue((StaplerRequest2) null, o);
    }

    @Test
    void testWorkflowJobWithCpsScmFlowDefinition(JenkinsRule jenkins) throws Exception {
        WorkflowJob p = jenkins.createProject(WorkflowJob.class, "wfj");
        p.setDefinition(new CpsScmFlowDefinition(getGitSCM(), "jenkinsfile"));

        GitParameterDefinition def = new GitParameterDefinition(
                "testName",
                Consts.PARAMETER_TYPE_TAG,
                null,
                "testDescription",
                null,
                ".*",
                "*",
                SortMode.ASCENDING,
                SelectedValue.TOP,
                null,
                false);

        p.addProperty(new ParametersDefinitionProperty(def));
        ItemsErrorModel items = def.getDescriptor().doFillValueItems(p, def.getName());
        assertTrue(isListBoxItem(items, "git-parameter-0.2"));
    }

    @Test
    void testWorkflowJobWithCpsFlowDefinition(JenkinsRule jenkins) throws Exception {
        WorkflowJob p = jenkins.createProject(WorkflowJob.class, "wfj");
        String script =
                "node {\n" + " git url: '" + GIT_PARAMETER_REPOSITORY_URL + "' \n" + " echo 'Some message'\n" + "}";

        p.setDefinition(new CpsFlowDefinition(script, false));
        GitParameterDefinition def = new GitParameterDefinition(
                "testName",
                Consts.PARAMETER_TYPE_TAG,
                null,
                "testDescription",
                null,
                ".*",
                "*",
                SortMode.ASCENDING,
                SelectedValue.TOP,
                null,
                false);

        p.addProperty(new ParametersDefinitionProperty(def));
        ItemsErrorModel items = def.getDescriptor().doFillValueItems(p, def.getName());
        // First build is fake build! And should return no Repository configured
        assertEquals(items.getErrors().get(1), Messages.GitParameterDefinition_noRepositoryConfigured());

        QueueTaskFuture<WorkflowRun> workflowRunQueueTaskFuture = p.scheduleBuild2(0);
        assertNotNull(workflowRunQueueTaskFuture);

        WorkflowRun workflowRun = workflowRunQueueTaskFuture.get();
        assertNotNull(workflowRun);

        items = def.getDescriptor().doFillValueItems(p, def.getName());
        assertTrue(isListBoxItem(items, "git-parameter-0.2"));
    }

    @Test
    void testProxySCM(JenkinsRule jenkins) throws Exception {
        FreeStyleProject anotherProject = jenkins.createFreeStyleProject("AnotherProject");
        anotherProject
                .getBuildersList()
                .add(Functions.isWindows() ? new BatchFile("echo test") : new Shell("echo test"));
        anotherProject.setScm(getGitSCM());

        FreeStyleProject project = jenkins.createFreeStyleProject("projectHaveProxySCM");
        project.setScm(new ProxySCM("AnotherProject"));

        GitParameterDefinition def = new GitParameterDefinition(
                "testName",
                Consts.PARAMETER_TYPE_TAG,
                null,
                "testDescription",
                null,
                ".*",
                "*",
                SortMode.ASCENDING,
                SelectedValue.TOP,
                null,
                false);

        project.addProperty(new ParametersDefinitionProperty(def));
        ItemsErrorModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "git-parameter-0.2"));
    }

    @Test
    void testParameterDefinedRepositoryUrl(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("testLocalValue");
        project.getBuildersList().add(Functions.isWindows() ? new BatchFile("echo test") : new Shell("echo test"));

        StringParameterDefinition stringParameterDef =
                new StringParameterDefinition("GIT_REPO_URL", GIT_PARAMETER_REPOSITORY_URL, "Description");
        GitParameterDefinition def = new GitParameterDefinition(
                "testName",
                Consts.PARAMETER_TYPE_BRANCH,
                null,
                "testDescription",
                null,
                ".*master.*",
                "*",
                SortMode.ASCENDING,
                SelectedValue.NONE,
                null,
                false);

        ParametersDefinitionProperty jobProp = new ParametersDefinitionProperty(stringParameterDef, def);
        project.addProperty(jobProp);
        setupGit(project, "${GIT_REPO_URL}");

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        assertEquals(Result.SUCCESS, build.getResult());
        ItemsErrorModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "origin/master"));
    }

    @Test
    void testMultiRepositoryInOneSCM(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("projectHaveMultiRepositoryInOneSCM");
        project.getBuildersList().add(Functions.isWindows() ? new BatchFile("echo test") : new Shell("echo test"));
        SCM gitSCM = getGitSCM(EXAMPLE_REPOSITORY_A_URL, EXAMPLE_REPOSITORY_B_URL);
        project.setScm(gitSCM);

        GitParameterDefinition def = new GitParameterDefinition(
                "name_git_parameter",
                Consts.PARAMETER_TYPE_BRANCH,
                null,
                "testDescription",
                null,
                ".*",
                "*",
                SortMode.ASCENDING,
                SelectedValue.TOP,
                null,
                false);

        project.addProperty(new ParametersDefinitionProperty(def));
        ItemsErrorModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "exA_branch_1"));
        assertFalse(isListBoxItem(items, "exB_branch_1"));

        def.setUseRepository(".*exampleB.git");
        items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertFalse(isListBoxItem(items, "exA_branch_1"));
        assertTrue(isListBoxItem(items, "exB_branch_1"));
    }

    @Test
    void testMultiSCM(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("projectHaveMultiSCM");
        project.getBuildersList().add(Functions.isWindows() ? new BatchFile("echo test") : new Shell("echo test"));
        MultiSCM multiSCM =
                new MultiSCM(Arrays.asList(getGitSCM(EXAMPLE_REPOSITORY_A_URL), getGitSCM(EXAMPLE_REPOSITORY_B_URL)));
        project.setScm(multiSCM);

        GitParameterDefinition def = new GitParameterDefinition(
                "testName",
                Consts.PARAMETER_TYPE_BRANCH,
                null,
                "testDescription",
                null,
                ".*",
                "*",
                SortMode.ASCENDING,
                SelectedValue.TOP,
                null,
                false);

        project.addProperty(new ParametersDefinitionProperty(def));
        ItemsErrorModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "origin/exA_branch_1"));
        assertFalse(isListBoxItem(items, "origin/exB_branch_1"));

        def.setUseRepository(".*exampleB.git");
        items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertFalse(isListBoxItem(items, "origin/exA_branch_1"));
        assertTrue(isListBoxItem(items, "origin/exB_branch_1"));
    }

    @Test
    void testMultiSCM_forSubdirectoryForRepo(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("projectHaveMultiSCM");
        project.getBuildersList().add(Functions.isWindows() ? new BatchFile("echo test") : new Shell("echo test"));
        GitSCM gitSCM = (GitSCM) getGitSCM(GIT_CLIENT_REPOSITORY_URL);
        gitSCM.getExtensions().add(new RelativeTargetDirectory("subDirectory"));
        MultiSCM multiSCM = new MultiSCM(Arrays.asList(getGitSCM(), gitSCM));
        project.setScm(multiSCM);

        GitParameterDefinition def = new GitParameterDefinition(
                "testName",
                Consts.PARAMETER_TYPE_BRANCH,
                null,
                "testDescription",
                null,
                ".*",
                "*",
                SortMode.ASCENDING,
                SelectedValue.TOP,
                null,
                false);

        project.addProperty(new ParametersDefinitionProperty(def));
        ItemsErrorModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "origin/master"));

        def.setUseRepository(".*git-client-plugin.git");
        items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "origin/master"));
    }

    @Test
    void testMultiSCM_forSubdirectoryForTheSomeRepo(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("projectHaveMultiSCM");
        project.getBuildersList().add(Functions.isWindows() ? new BatchFile("echo test") : new Shell("echo test"));
        GitSCM gitSCM = (GitSCM) getGitSCM(GIT_CLIENT_REPOSITORY_URL);
        gitSCM.getExtensions().add(new RelativeTargetDirectory("subDirectory"));
        MultiSCM multiSCM = new MultiSCM(Arrays.asList(getGitSCM(GIT_CLIENT_REPOSITORY_URL), gitSCM));
        project.setScm(multiSCM);

        GitParameterDefinition def = new GitParameterDefinition(
                "testName",
                Consts.PARAMETER_TYPE_BRANCH,
                null,
                "testDescription",
                null,
                ".*",
                "*",
                SortMode.ASCENDING,
                SelectedValue.TOP,
                null,
                false);

        project.addProperty(new ParametersDefinitionProperty(def));
        ItemsErrorModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "origin/master"));
        int expected = items.size();

        def.setUseRepository(".*git-client-plugin.git");
        items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "origin/master"));
        assertEquals(expected, items.size());
    }

    @Test
    void testMultiSCM_repositoryUrlIsNotSet(JenkinsRule jenkins) throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("projectHaveMultiSCM");
        project.getBuildersList().add(Functions.isWindows() ? new BatchFile("echo test") : new Shell("echo test"));
        GitSCM gitSCM = (GitSCM) getGitSCM(GIT_CLIENT_REPOSITORY_URL);
        gitSCM.getExtensions().add(new RelativeTargetDirectory("subDirectory"));
        MultiSCM multiSCM = new MultiSCM(Arrays.asList(getGitSCM(""), gitSCM));
        project.setScm(multiSCM);

        GitParameterDefinition def = new GitParameterDefinition(
                "testName",
                Consts.PARAMETER_TYPE_BRANCH,
                null,
                "testDescription",
                null,
                ".*",
                "*",
                SortMode.ASCENDING,
                SelectedValue.TOP,
                null,
                false);
        def.setUseRepository(".*git-client-plugin.git");

        project.addProperty(new ParametersDefinitionProperty(def));
        ItemsErrorModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "origin/master"));
    }

    @Test
    void symbolPipelineTest(JenkinsRule jenkins) {
        Descriptor<?> gitParameter = SymbolLookup.get().findDescriptor(Describable.class, "gitParameter");
        assertNotNull(gitParameter);
    }

    @Test
    void testCreateValue_CLICommand(JenkinsRule jenkins) throws Exception {
        CLICommand cliCommand = new ConsoleCommand();
        GitParameterDefinition instance = new GitParameterDefinition(
                NAME,
                PT_REVISION,
                DEFAULT_VALUE,
                "description",
                "branch",
                ".*",
                "*",
                SortMode.NONE,
                SelectedValue.NONE,
                null,
                false);

        String value = "test";
        ParameterValue result = instance.createValue(cliCommand, value);
        assertEquals(new GitParameterValue(NAME, value), result);
    }

    @Test
    void testCreateRequiredValueFail_CLICommand(JenkinsRule jenkins) {
        CLICommand cliCommand = new ConsoleCommand();
        GitParameterDefinition instance = new GitParameterDefinition(
                NAME,
                PT_REVISION,
                "",
                "description",
                "branch",
                ".*",
                "*",
                SortMode.NONE,
                SelectedValue.NONE,
                null,
                false);
        instance.setRequiredParameter(true);
        assertThrows(Failure.class, () -> instance.createValue(cliCommand, ""));
    }

    @Test
    void testCreateRequiredValuePass_CLICommand(JenkinsRule jenkins) throws Exception {
        CLICommand cliCommand = new ConsoleCommand();
        GitParameterDefinition instance = new GitParameterDefinition(
                NAME,
                PT_REVISION,
                DEFAULT_VALUE,
                "description",
                "branch",
                ".*",
                "*",
                SortMode.NONE,
                SelectedValue.NONE,
                null,
                false);
        instance.setRequiredParameter(true);
        String value = "test";
        ParameterValue result = instance.createValue(cliCommand, value);
        assertEquals(new GitParameterValue(NAME, value), result);
    }

    @Test
    void testCreateValue_CLICommand_EmptyValue(JenkinsRule jenkins) throws Exception {
        CLICommand cliCommand = new ConsoleCommand();
        GitParameterDefinition instance = new GitParameterDefinition(
                NAME,
                PT_REVISION,
                DEFAULT_VALUE,
                "description",
                "branch",
                ".*",
                "*",
                SortMode.NONE,
                SelectedValue.NONE,
                null,
                false);

        ParameterValue result = instance.createValue(cliCommand, null);
        assertEquals(new GitParameterValue(NAME, DEFAULT_VALUE), result);
    }

    private void setupGit(FreeStyleProject project) throws Exception {
        setupGit(project, GIT_PARAMETER_REPOSITORY_URL);
    }

    private void setupGit(FreeStyleProject project, String url) throws Exception {
        SCM git = getGitSCM(url);
        project.setScm(git);
    }

    private SCM getGitSCM() {
        return getGitSCM(GIT_PARAMETER_REPOSITORY_URL);
    }

    private SCM getGitSCM(String... urls) {
        List<UserRemoteConfig> configs = new ArrayList<>();
        for (String url : urls) {
            UserRemoteConfig config = new UserRemoteConfig(url, null, null, null);
            configs.add(config);
        }
        return new GitSCM(configs, null, null, null, null);
    }

    private boolean isListBoxItem(ItemsErrorModel items, String item) {
        boolean itemExists = false;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).value.contains(item)) {
                itemExists = true;
            }
        }
        return itemExists;
    }

    private boolean isListBoxItemName(ItemsErrorModel items, String item) {
        boolean itemExists = false;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).name.contains(item)) {
                itemExists = true;
            }
        }
        return itemExists;
    }
}
