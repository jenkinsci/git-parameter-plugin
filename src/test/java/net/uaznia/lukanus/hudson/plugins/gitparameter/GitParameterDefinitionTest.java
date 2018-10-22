package net.uaznia.lukanus.hudson.plugins.gitparameter;

import hudson.EnvVars;
import hudson.model.*;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.UserRemoteConfig;
import hudson.plugins.git.extensions.impl.RelativeTargetDirectory;
import hudson.plugins.templateproject.ProxySCM;
import hudson.scm.SCM;
import hudson.tasks.Shell;
import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;
import hudson.util.ListBoxModel;
import net.uaznia.lukanus.hudson.plugins.gitparameter.GitParameterDefinition.DescriptorImpl;
import net.uaznia.lukanus.hudson.plugins.gitparameter.jobs.JobWrapper;
import net.uaznia.lukanus.hudson.plugins.gitparameter.jobs.JobWrapperFactory;
import org.jenkinsci.plugins.multiplescms.MultiSCM;
import org.jenkinsci.plugins.structs.SymbolLookup;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockFolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static net.uaznia.lukanus.hudson.plugins.gitparameter.Constants.*;
import static net.uaznia.lukanus.hudson.plugins.gitparameter.scms.SCMFactory.getGitSCMs;
import static org.junit.Assert.*;

/**
 * @author lukanus
 */
public class GitParameterDefinitionTest {
    private FreeStyleProject project;

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    // Test Descriptor.getProjectSCM()
    @Test
    public void testGetProjectSCM() throws Exception {
        FreeStyleProject testJob = jenkins.createFreeStyleProject("testGetProjectSCM");
        GitSCM git = new GitSCM(GIT_PARAMETER_REPOSITORY_URL);
        GitParameterDefinition def = new GitParameterDefinition("testName",
                PT_REVISION,
                "testDefaultValue",
                "testDescription",
                "testBranch",
                ".*",
                "*",
                SortMode.NONE, SelectedValue.NONE, null, false);
        testJob.addProperty(new ParametersDefinitionProperty(def));
        JobWrapper IJobWrapper = JobWrapperFactory.createJobWrapper(testJob);
        assertTrue(getGitSCMs(IJobWrapper, null).isEmpty());

        testJob.setScm(git);
        assertTrue(git.equals(getGitSCMs(IJobWrapper, null).get(0)));
    }

    @Test
    public void testDoFillValueItems_withoutSCM() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject("testListTags");
        GitParameterDefinition def = new GitParameterDefinition("testName",
                "PT_TAG",
                "testDefaultValue",
                "testDescription",
                "testBranch",
                ".*",
                "*",
                SortMode.NONE, SelectedValue.NONE, null, false);
        project.addProperty(new ParametersDefinitionProperty(def));

        ListBoxModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "No Git repository configured in SCM configuration"));
    }

    @Test
    public void testDoFillValueItems_listTags() throws Exception {
        project = jenkins.createFreeStyleProject("testListTags");
        project.getBuildersList().add(new Shell("echo test"));
        setupGit();

        GitParameterDefinition def = new GitParameterDefinition("testName",
                "PT_TAG",
                "testDefaultValue",
                "testDescription",
                "testBranch",
                ".*",
                "*",
                SortMode.NONE, SelectedValue.NONE, null, false);
        project.addProperty(new ParametersDefinitionProperty(def));

        // Run the build once to get the workspace
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        ListBoxModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "git-parameter-0.2"));
    }

    @Test
    public void testGetListBranchNoBuildProject() throws Exception {
        project = jenkins.createFreeStyleProject("testListTags");
        project.getBuildersList().add(new Shell("echo test"));
        setupGit();

        GitParameterDefinition def = new GitParameterDefinition("testName",
                "PT_BRANCH",
                "testDefaultValue",
                "testDescription",
                "testBranch",
                ".*",
                "*",
                SortMode.NONE, SelectedValue.NONE, null, false);
        project.addProperty(new ParametersDefinitionProperty(def));

        ListBoxModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "master"));
    }

    @Test
    public void testGetListBranchAfterWipeOutWorkspace() throws Exception {
        project = jenkins.createFreeStyleProject("testListTags");
        project.getBuildersList().add(new Shell("echo test"));
        setupGit();

        GitParameterDefinition def = new GitParameterDefinition("testName",
                "PT_BRANCH",
                null,
                "testDescription",
                "testBranch",
                ".*",
                "*",
                SortMode.NONE, SelectedValue.NONE, null, false);
        project.addProperty(new ParametersDefinitionProperty(def));
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        // build.run();
        project.doDoWipeOutWorkspace();
        ListBoxModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "master"));
    }

    @Test
    public void testDoFillValueItems_listBranches() throws Exception {
        project = jenkins.createFreeStyleProject("testListTags");
        project.getBuildersList().add(new Shell("echo test"));
        setupGit();

        GitParameterDefinition def = new GitParameterDefinition("testName",
                "PT_BRANCH",
                "testDefaultValue",
                "testDescription",
                "testBranch",
                ".*",
                "*",
                SortMode.NONE, SelectedValue.NONE, null, false);
        project.addProperty(new ParametersDefinitionProperty(def));

        // Run the build once to get the workspace
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        ListBoxModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "master"));
    }

    @Test
    public void tesListBranchesWrongBranchFilter() throws Exception {
        project = jenkins.createFreeStyleProject("testListTags");
        project.getBuildersList().add(new Shell("echo test"));
        setupGit();

        GitParameterDefinition def = new GitParameterDefinition("testName",
                "PT_BRANCH",
                "testDefaultValue",
                "testDescription",
                "testBranch",
                "[*",
                "*",
                SortMode.NONE, SelectedValue.NONE, null, false);
        project.addProperty(new ParametersDefinitionProperty(def));

        ListBoxModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "master"));
    }

    @Test
    public void testDoFillValueItems_listBranches_withRegexGroup() throws Exception {
        project = jenkins.createFreeStyleProject("testListTags");
        project.getBuildersList().add(new Shell("echo test"));
        setupGit();

        GitParameterDefinition def = new GitParameterDefinition("testName",
                "PT_BRANCH",
                "testDefaultValue",
                "testDescription",
                "testBranch",
                "origin/(.*)",
                "*",
                SortMode.NONE, SelectedValue.NONE, null, false);
        project.addProperty(new ParametersDefinitionProperty(def));

        // Run the build once to get the workspace
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        ListBoxModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "master"));
        assertFalse(isListBoxItem(items, "origin/master"));
    }

    @Test
    public void testSortAscendingTag() throws Exception {
        project = jenkins.createFreeStyleProject("testListTags");
        project.getBuildersList().add(new Shell("echo test"));
        setupGit();

        GitParameterDefinition def = new GitParameterDefinition("testName",
                "PT_TAG",
                "testDefaultValue",
                "testDescription",
                null,
                ".*",
                "*",
                SortMode.ASCENDING, SelectedValue.NONE, null, false);
        project.addProperty(new ParametersDefinitionProperty(def));

        ListBoxModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertEquals("0.1", items.get(0).value);
    }

    @Test
    public void testWrongTagFilter() throws Exception {
        project = jenkins.createFreeStyleProject("testListTags");
        project.getBuildersList().add(new Shell("echo test"));
        setupGit();

        GitParameterDefinition def = new GitParameterDefinition("testName",
                "PT_TAG",
                "testDefaultValue",
                "testDescription",
                null,
                ".*",
                "wrongTagFilter",
                SortMode.ASCENDING, SelectedValue.NONE, null, false);
        project.addProperty(new ParametersDefinitionProperty(def));

        ListBoxModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(items.isEmpty());
    }

    @Test
    public void testSortDescendingTag() throws Exception {
        project = jenkins.createFreeStyleProject("testListTags");
        project.getBuildersList().add(new Shell("echo test"));
        setupGit();

        GitParameterDefinition def = new GitParameterDefinition("testName",
                "PT_TAG",
                "testDefaultValue",
                "testDescription",
                null,
                ".*",
                "*",
                SortMode.DESCENDING, SelectedValue.NONE, null, false);
        project.addProperty(new ParametersDefinitionProperty(def));

        ListBoxModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertEquals("0.1", items.get(items.size() - 1).value);
    }

    @Test
    public void testDoFillValueItems_listTagsAndBranches() throws Exception {
        project = jenkins.createFreeStyleProject("testListTags");
        project.getBuildersList().add(new Shell("echo test"));
        setupGit();

        GitParameterDefinition def = new GitParameterDefinition("testName",
                "PT_BRANCH_TAG",
                "testDefaultValue",
                "testDescription",
                "testBranch",
                ".*",
                "*",
                SortMode.NONE, SelectedValue.NONE, null, false);
        project.addProperty(new ParametersDefinitionProperty(def));

        // Run the build once to get the workspace
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        ListBoxModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "master"));
        assertTrue(isListBoxItem(items, "git-parameter-0.2"));
    }

    @Test
    public void testDoFillValueItems_listRevisions() throws Exception {
        project = jenkins.createFreeStyleProject("testListRevisions");
        project.getBuildersList().add(new Shell("echo test"));
        setupGit();

        GitParameterDefinition def = new GitParameterDefinition("testName",
                PT_REVISION,
                "testDefaultValue",
                "testDescription",
                null,
                ".*",
                "*",
                SortMode.NONE, SelectedValue.NONE, null, false);

        project.addProperty(new ParametersDefinitionProperty(def));

        // Run the build once to get the workspace
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        ListBoxModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "00a8385c"));
    }

    @Test
    public void testDoFillValueItems_listRevisionsWithBranch() throws Exception {
        project = jenkins.createFreeStyleProject("testListRevisions");
        project.getBuildersList().add(new Shell("echo test"));
        setupGit();
        GitParameterDefinition def = new GitParameterDefinition("testName",
                PT_REVISION,
                "testDefaultValue",
                "testDescription",
                "origin/preview_0_3",
                ".*",
                "*",
                SortMode.NONE, SelectedValue.NONE, null, false);

        project.addProperty(new ParametersDefinitionProperty(def));

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        ListBoxModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "00a8385c"));
    }

    @Test
    public void testDoFillValueItems_listPullRequests() throws Exception {
        project = jenkins.createFreeStyleProject("testListPullRequests");
        project.getBuildersList().add(new Shell("echo test"));
        setupGit();
        GitParameterDefinition def = new GitParameterDefinition("testName",
                Consts.PARAMETER_TYPE_PULL_REQUEST,
                "master",
                "testDescription",
                "",
                ".*",
                "*",
                SortMode.NONE, SelectedValue.NONE, null, false);

        project.addProperty(new ParametersDefinitionProperty(def));

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        ListBoxModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "41"));
        assertTrue(isListBoxItem(items, "44"));
    }

    @Test
    public void testSearchInFolders() throws Exception {
        MockFolder folder = jenkins.createFolder("folder");
        FreeStyleProject job1 = folder.createProject(FreeStyleProject.class, "job1");

        GitParameterDefinition gitParameterDefinition = new GitParameterDefinition(NAME,
                "asdf",
                "other",
                "description",
                "branch",
                ".*",
                "*",
                SortMode.NONE, SelectedValue.NONE, null, false);
        job1.addProperty(new ParametersDefinitionProperty(gitParameterDefinition));
        assertEquals("folder/job1", gitParameterDefinition.getParentJob().getFullName());
    }

    @Test
    public void testBranchFilterValidation() {
        final DescriptorImpl descriptor = new DescriptorImpl();
        final FormValidation okWildcard = descriptor.doCheckBranchFilter(".*");
        final FormValidation badWildcard = descriptor.doCheckBranchFilter(".**");

        assertTrue(okWildcard.kind == Kind.OK);
        assertTrue(badWildcard.kind == Kind.ERROR);
    }

    @Test
    public void testUseRepositoryValidation() {
        final DescriptorImpl descriptor = new DescriptorImpl();
        final FormValidation okWildcard = descriptor.doCheckUseRepository(".*");
        final FormValidation badWildcard = descriptor.doCheckUseRepository(".**");

        assertTrue(okWildcard.kind == Kind.OK);
        assertTrue(badWildcard.kind == Kind.ERROR);
    }

    @Test
    public void testGetDefaultValueWhenDefaultValueIsSet() throws Exception {
        project = jenkins.createFreeStyleProject("testDefaultValue");
        project.getBuildersList().add(new Shell("echo test"));
        setupGit();
        String testDefaultValue = "testDefaultValue";
        GitParameterDefinition def = new GitParameterDefinition("testName",
                Consts.PARAMETER_TYPE_TAG,
                testDefaultValue,
                "testDescription",
                null,
                ".*",
                "*",
                SortMode.ASCENDING, SelectedValue.TOP, null, false);

        project.addProperty(new ParametersDefinitionProperty(def));

        assertTrue(testDefaultValue.equals(def.getDefaultParameterValue().getValue()));
    }

    @Test
    public void testGetDefaultValueAsTop() throws Exception {
        project = jenkins.createFreeStyleProject("testDefaultValueAsTOP");
        project.getBuildersList().add(new Shell("echo test"));
        setupGit();
        GitParameterDefinition def = new GitParameterDefinition("testName",
                Consts.PARAMETER_TYPE_TAG,
                null,
                "testDescription",
                null,
                ".*",
                "*",
                SortMode.ASCENDING, SelectedValue.TOP, null, false);

        project.addProperty(new ParametersDefinitionProperty(def));
        assertEquals("0.1", def.getDefaultParameterValue().getValue());
    }

    @Test
    public void testGlobalVariableRepositoryUrl() throws Exception {
        EnvVars.masterEnvVars.put("GIT_REPO_URL", GIT_PARAMETER_REPOSITORY_URL);
        project = jenkins.createFreeStyleProject("testGlobalValue");
        project.getBuildersList().add(new Shell("echo test"));
        setupGit("$GIT_REPO_URL");
        GitParameterDefinition def = new GitParameterDefinition("testName",
                Consts.PARAMETER_TYPE_BRANCH,
                null,
                "testDescription",
                null,
                ".*master.*",
                "*",
                SortMode.ASCENDING, SelectedValue.NONE, null, false);

        project.addProperty(new ParametersDefinitionProperty(def));
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        assertEquals(build.getResult(), Result.SUCCESS);
        ListBoxModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "origin/master"));
    }

    @Test
    public void testWorkflowJobWithCpsScmFlowDefinition() throws IOException, InterruptedException {
        WorkflowJob p = jenkins.createProject(WorkflowJob.class, "wfj");
        p.setDefinition(new CpsScmFlowDefinition(getGitSCM(), "jenkinsfile"));

        GitParameterDefinition def = new GitParameterDefinition("testName",
                Consts.PARAMETER_TYPE_TAG,
                null,
                "testDescription",
                null,
                ".*",
                "*",
                SortMode.ASCENDING, SelectedValue.TOP, null, false);

        p.addProperty(new ParametersDefinitionProperty(def));
        ListBoxModel items = def.getDescriptor().doFillValueItems(p, def.getName());
        assertTrue(isListBoxItem(items, "git-parameter-0.2"));
    }

    @Test
    public void testWorkflowJobWithCpsFlowDefinition() throws IOException, InterruptedException, ExecutionException {
        WorkflowJob p = jenkins.createProject(WorkflowJob.class, "wfj");
        String script = "node {\n" +
                " git url: '" + GIT_PARAMETER_REPOSITORY_URL + "' \n" +
                " echo 'Some message'\n" +
                "}";


        p.setDefinition(new CpsFlowDefinition(script, false));
        GitParameterDefinition def = new GitParameterDefinition("testName",
                Consts.PARAMETER_TYPE_TAG,
                null,
                "testDescription",
                null,
                ".*",
                "*",
                SortMode.ASCENDING, SelectedValue.TOP, null, false);

        p.addProperty(new ParametersDefinitionProperty(def));
        ListBoxModel items = def.getDescriptor().doFillValueItems(p, def.getName());
        //First build is fake build! And should return no Repository configured
        assertTrue(isListBoxItem(items, Messages.GitParameterDefinition_noRepositoryConfigured()));

        p.scheduleBuild2(0).get();
        items = def.getDescriptor().doFillValueItems(p, def.getName());
        assertTrue(isListBoxItem(items, "git-parameter-0.2"));
    }

    @Test
    public void testProxySCM() throws IOException, InterruptedException {
        FreeStyleProject anotherProject = jenkins.createFreeStyleProject("AnotherProject");
        anotherProject.getBuildersList().add(new Shell("echo test"));
        anotherProject.setScm(getGitSCM());

        project = jenkins.createFreeStyleProject("projectHaveProxySCM");
        project.setScm(new ProxySCM("AnotherProject"));

        GitParameterDefinition def = new GitParameterDefinition("testName",
                Consts.PARAMETER_TYPE_TAG,
                null,
                "testDescription",
                null,
                ".*",
                "*",
                SortMode.ASCENDING, SelectedValue.TOP, null, false);

        project.addProperty(new ParametersDefinitionProperty(def));
        ListBoxModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "git-parameter-0.2"));
    }

    @Test
    public void testParameterDefinedRepositoryUrl() throws Exception {
        project = jenkins.createFreeStyleProject("testLocalValue");
        project.getBuildersList().add(new Shell("echo test"));

        StringParameterDefinition stringParameterDef = new StringParameterDefinition("GIT_REPO_URL", GIT_PARAMETER_REPOSITORY_URL, "Description");
        GitParameterDefinition def = new GitParameterDefinition("testName",
                Consts.PARAMETER_TYPE_BRANCH,
                null,
                "testDescription",
                null,
                ".*master.*",
                "*",
                SortMode.ASCENDING, SelectedValue.NONE, null, false);

        ParametersDefinitionProperty jobProp = new ParametersDefinitionProperty(stringParameterDef, def);
        project.addProperty(jobProp);
        setupGit("${GIT_REPO_URL}");

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        assertEquals(build.getResult(), Result.SUCCESS);
        ListBoxModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "origin/master"));
    }

    @Test
    public void testMultiRepositoryInOneSCM() throws IOException, InterruptedException {
        project = jenkins.createFreeStyleProject("projectHaveMultiRepositoryInOneSCM");
        project.getBuildersList().add(new Shell("echo test"));
        SCM gitSCM = getGitSCM(EXAMPLE_REPOSITORY_A_URL, EXAMPLE_REPOSITORY_B_URL);
        project.setScm(gitSCM);

        GitParameterDefinition def = new GitParameterDefinition("name_git_parameter",
                Consts.PARAMETER_TYPE_BRANCH,
                null,
                "testDescription",
                null,
                ".*",
                "*",
                SortMode.ASCENDING, SelectedValue.TOP, null, false);


        project.addProperty(new ParametersDefinitionProperty(def));
        ListBoxModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "exA_branch_1"));
        assertFalse(isListBoxItem(items, "exB_branch_1"));

        def.setUseRepository(".*exampleB.git");
        items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertFalse(isListBoxItem(items, "exA_branch_1"));
        assertTrue(isListBoxItem(items, "exB_branch_1"));
    }

    @Test
    public void testMultiSCM() throws IOException, InterruptedException {
        project = jenkins.createFreeStyleProject("projectHaveMultiSCM");
        project.getBuildersList().add(new Shell("echo test"));
        MultiSCM multiSCM = new MultiSCM(Arrays.asList(getGitSCM(EXAMPLE_REPOSITORY_A_URL), getGitSCM(EXAMPLE_REPOSITORY_B_URL)));
        project.setScm(multiSCM);

        GitParameterDefinition def = new GitParameterDefinition("testName",
                Consts.PARAMETER_TYPE_BRANCH,
                null,
                "testDescription",
                null,
                ".*",
                "*",
                SortMode.ASCENDING, SelectedValue.TOP, null, false);

        project.addProperty(new ParametersDefinitionProperty(def));
        ListBoxModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "origin/exA_branch_1"));
        assertFalse(isListBoxItem(items, "origin/exB_branch_1"));

        def.setUseRepository(".*exampleB.git");
        items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertFalse(isListBoxItem(items, "origin/exA_branch_1"));
        assertTrue(isListBoxItem(items, "origin/exB_branch_1"));
    }

    @Test
    public void testMultiSCM_forSubdirectoryForRepo() throws IOException, InterruptedException {
        project = jenkins.createFreeStyleProject("projectHaveMultiSCM");
        project.getBuildersList().add(new Shell("echo test"));
        GitSCM gitSCM = (GitSCM) getGitSCM(GIT_CLIENT_REPOSITORY_URL);
        gitSCM.getExtensions().add(new RelativeTargetDirectory("subDirectory"));
        MultiSCM multiSCM = new MultiSCM(Arrays.asList(getGitSCM(), gitSCM));
        project.setScm(multiSCM);

        GitParameterDefinition def = new GitParameterDefinition("testName",
                Consts.PARAMETER_TYPE_BRANCH,
                null,
                "testDescription",
                null,
                ".*",
                "*",
                SortMode.ASCENDING, SelectedValue.TOP, null, false);

        project.addProperty(new ParametersDefinitionProperty(def));
        ListBoxModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "origin/master"));

        def.setUseRepository(".*git-client-plugin.git");
        items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "origin/master"));
    }

    @Test
    public void testMultiSCM_forSubdirectoryForTheSomeRepo() throws IOException, InterruptedException {
        project = jenkins.createFreeStyleProject("projectHaveMultiSCM");
        project.getBuildersList().add(new Shell("echo test"));
        GitSCM gitSCM = (GitSCM) getGitSCM(GIT_CLIENT_REPOSITORY_URL);
        gitSCM.getExtensions().add(new RelativeTargetDirectory("subDirectory"));
        MultiSCM multiSCM = new MultiSCM(Arrays.asList(getGitSCM(GIT_CLIENT_REPOSITORY_URL), gitSCM));
        project.setScm(multiSCM);

        GitParameterDefinition def = new GitParameterDefinition("testName",
                Consts.PARAMETER_TYPE_BRANCH,
                null,
                "testDescription",
                null,
                ".*",
                "*",
                SortMode.ASCENDING, SelectedValue.TOP, null, false);

        project.addProperty(new ParametersDefinitionProperty(def));
        ListBoxModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "origin/master"));
        int expected = items.size();

        def.setUseRepository(".*git-client-plugin.git");
        items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "origin/master"));
        assertEquals(expected, items.size());
    }

    @Test
    public void symbolPipelineTest() {
        Descriptor<? extends Describable> gitParameter = SymbolLookup.get().findDescriptor(Describable.class, "gitParameter");
        assertNotNull(gitParameter);
    }

    private void setupGit() throws IOException {
        setupGit(GIT_PARAMETER_REPOSITORY_URL);
    }

    private void setupGit(String url) throws IOException {
        SCM git = getGitSCM(url);
        project.setScm(git);
    }

    private SCM getGitSCM() {
        return getGitSCM(GIT_PARAMETER_REPOSITORY_URL);
    }

    private SCM getGitSCM(String... urls) {
        List<UserRemoteConfig> configs = new ArrayList<UserRemoteConfig>();
        for (String url : urls) {
            UserRemoteConfig config = new UserRemoteConfig(url, null, null, null);
            configs.add(config);
        }
        return new GitSCM(configs, null, false, null, null, null, null);
    }

    private boolean isListBoxItem(ListBoxModel items, String item) {
        boolean itemExists = false;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).value.contains(item)) {
                itemExists = true;
            }
        }
        return itemExists;
    }
}
