package net.uaznia.lukanus.hudson.plugins.gitparameter;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractProject;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.TopLevelItem;
import hudson.plugins.git.Branch;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.UserRemoteConfig;
import hudson.scm.SCM;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.jenkinsci.plugins.gitclient.FetchCommand;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class GitParameterDefinition extends ParameterDefinition implements Comparable<GitParameterDefinition> {
    private static final long serialVersionUID = 9157832967140868122L;

    public static final String PARAMETER_TYPE_TAG = "PT_TAG";
    public static final String PARAMETER_TYPE_REVISION = "PT_REVISION";
    public static final String PARAMETER_TYPE_BRANCH = "PT_BRANCH";
    public static final String PARAMETER_TYPE_TAG_BRANCH = "PT_BRANCH_TAG";
    private static final Logger LOGGER = Logger.getLogger(GitParameterDefinition.class.getName());
    public static final String TEMPORARY_DIRECTORY_PREFIX = "git_parameter_";

    private static final String REPO_SCM_CLASS_NAME = "hudson.plugins.repo.RepoScm";
    private static final String REPO_SCM_NAME = "repo";
    private static final String REPO_MANIFESTS_DIR = ".repo/manifests";

    private final UUID uuid;
    private String type;
    private String branch;
    private String tagFilter;
    private String branchFilter;
    private SortMode sortMode;
    private String defaultValue;
    private SelectedValue selectedValue;
    private Boolean quickFilterEnabled;

    @DataBoundConstructor
    public GitParameterDefinition(String name, String type, String defaultValue, String description, String branch,
                                  String branchFilter, String tagFilter, SortMode sortMode, SelectedValue selectedValue,
                                  Boolean quickFilterEnabled) {
        super(name, description);
        this.defaultValue = defaultValue;
        this.branch = branch;
        this.uuid = UUID.randomUUID();
        this.sortMode = sortMode;
        this.selectedValue = selectedValue;
        this.quickFilterEnabled = quickFilterEnabled;

        setType(type);
        setTagFilter(tagFilter);
        setBranchFilter(branchFilter);
    }

    @Override
    public ParameterValue createValue(StaplerRequest request) {
        String value[] = request.getParameterValues(getName());
        if (value == null || value.length == 0 || StringUtils.isBlank(value[0])) {
            return getDefaultParameterValue();
        }
        return new GitParameterValue(getName(), value[0]);
    }

    @Override
    public ParameterValue createValue(StaplerRequest request, JSONObject jO) {
        Object value = jO.get("value");
        String strValue = "";
        if (value instanceof String) {
            strValue = (String) value;
        } else if (value instanceof JSONArray) {
            JSONArray jsonValues = (JSONArray) value;
            for (int i = 0; i < jsonValues.size(); i++) {
                strValue += jsonValues.getString(i);
                if (i < jsonValues.size() - 1) {
                    strValue += ",";
                }
            }
        }

        if ("".equals(strValue)) {
            strValue = defaultValue;
        }

        GitParameterValue gitParameterValue = new GitParameterValue(jO.getString("name"), strValue);
        return gitParameterValue;
    }

    @Override
    public ParameterValue getDefaultParameterValue() {
        //If 'Default Value' is set has high priority!
        String defValue = getDefaultValue();
        if (!StringUtils.isBlank(defValue)) {
            return new GitParameterValue(getName(), defValue);
        }

        switch (getSelectedValue()) {
            case TOP:
                try {
                    ListBoxModel valueItems = getDescriptor().doFillValueItems(getParentProject(), getName());
                    if (valueItems.size() > 0) {
                        return new GitParameterValue(getName(), valueItems.get(0).value);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Unexpected error!", e);
                }
        }
        return super.getDefaultParameterValue();
    }

    @Override
    public String getType() {
        return type;
    }

    public void setType(String type) {
        if (isParameterTypeCorrect(type)) {
            this.type = type;
        } else {
            this.type = PARAMETER_TYPE_BRANCH;
        }
    }

    private boolean isParameterTypeCorrect(String type) {
        return type.equals(PARAMETER_TYPE_TAG) || type.equals(PARAMETER_TYPE_REVISION)
                || type.equals(PARAMETER_TYPE_BRANCH) || type.equals(PARAMETER_TYPE_TAG_BRANCH);
    }

    public String getBranch() {
        return this.branch;
    }

    public void setBranch(String nameOfBranch) {
        this.branch = nameOfBranch;
    }

    public SortMode getSortMode() {
        return this.sortMode;
    }

    public void setSortMode(SortMode sortMode) {
        this.sortMode = sortMode;
    }

    public String getTagFilter() {
        return this.tagFilter;
    }

    public void setTagFilter(String tagFilter) {
        if (StringUtils.isEmpty(StringUtils.trim(tagFilter))) {
            tagFilter = "*";
        }
        this.tagFilter = tagFilter;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getBranchFilter() {
        return branchFilter;
    }

    public void setBranchFilter(String branchFilter) {
        if (StringUtils.isEmpty(StringUtils.trim(branchFilter))) {
            branchFilter = ".*";
        }

        this.branchFilter = branchFilter;
    }

    public SelectedValue getSelectedValue() {
        return selectedValue == null ? SelectedValue.TOP : selectedValue;
    }

    public Boolean getQuickFilterEnabled() {
        return quickFilterEnabled;
    }

    public AbstractProject<?, ?> getParentProject() {
        AbstractProject<?, ?> context = null;
        List<AbstractProject> jobs = Jenkins.getInstance().getAllItems(AbstractProject.class);

        for (AbstractProject<?, ?> project : jobs) {
            if (!(project instanceof TopLevelItem)) continue;

            ParametersDefinitionProperty property = project.getProperty(ParametersDefinitionProperty.class);

            if (property != null) {
                List<ParameterDefinition> parameterDefinitions = property.getParameterDefinitions();

                if (parameterDefinitions != null) {
                    for (ParameterDefinition pd : parameterDefinitions) {
                        if (pd instanceof GitParameterDefinition && ((GitParameterDefinition) pd).compareTo(this) == 0) {
                            context = project;
                            break;
                        }
                    }
                }
            }
        }

        return context;
    }

    public int compareTo(GitParameterDefinition pd) {
        return pd.uuid.equals(uuid) ? 0 : -1;
    }

    public Map<String, String> generateContents(AbstractProject<?, ?> project, GitSCM git) {

        Map<String, String> paramList = new LinkedHashMap<String, String>();
        try {
            EnvVars environment = getEnvironment(project);
            for (RemoteConfig repository : git.getRepositories()) {
                boolean isRepoScm = REPO_SCM_NAME.equals(repository.getName());
                synchronized (GitParameterDefinition.class) {
                    FilePathWrapper workspace = getWorkspace(project, isRepoScm);
                    GitClient gitClient = getGitClient(project, workspace, git, environment);
                    for (URIish remoteURL : repository.getURIs()) {
                        initWorkspace(workspace, gitClient, remoteURL);

                        FetchCommand fetch = gitClient.fetch_().prune().from(remoteURL, repository.getFetchRefSpecs());
                        fetch.execute();

                        if (type.equalsIgnoreCase(PARAMETER_TYPE_REVISION)) {
                            getRevision(paramList, gitClient);
                        }
                        if (type.equalsIgnoreCase(PARAMETER_TYPE_TAG) || type.equalsIgnoreCase(PARAMETER_TYPE_TAG_BRANCH)) {
                            Set<String> tagSet = gitClient.getTagNames(tagFilter);
                            sortAndPutToParam(tagSet, paramList);
                        }
                        if (type.equalsIgnoreCase(PARAMETER_TYPE_BRANCH) || type.equalsIgnoreCase(PARAMETER_TYPE_TAG_BRANCH)) {
                            Set<String> branchSet = getBranch(gitClient);
                            sortAndPutToParam(branchSet, paramList);
                        }
                    }
                    workspace.delete();
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error!", e);
            String message = e.getMessage() + " Pleas look to log!";
            paramList.clear();
            paramList.put(message, message);
        }
        return paramList;
    }

    private Set<String> getBranch(GitClient gitClient) throws InterruptedException {
        Set<String> branchSet = new HashSet<String>();
        Pattern branchFilterPattern;
        try {
            branchFilterPattern = Pattern.compile(branchFilter);
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Specified branchFilter is not a valid regex. Setting to '.*'", e.getMessage());
            branchFilterPattern = Pattern.compile(".*");
        }
        for (Branch branch : gitClient.getRemoteBranches()) {
            String branchName = branch.getName();
            Matcher matcher = branchFilterPattern.matcher(branchName);
            if (matcher.matches()) {
                if (matcher.groupCount() == 1) {
                    branchSet.add(matcher.group(1));
                } else {
                    branchSet.add(branchName);
                }
            }
        }
        return branchSet;
    }

    private void getRevision(Map<String, String> paramList, GitClient gitClient) throws InterruptedException {
        RevisionInfoFactory revisionInfoFactory = new RevisionInfoFactory(gitClient, branch);
        List<RevisionInfo> revisions = revisionInfoFactory.getRevisions();

        for (RevisionInfo revision : revisions) {
            paramList.put(revision.getSha1(), revision.getRevisionInfo());
        }
    }

    private void sortAndPutToParam(Set<String> setElement, Map<String, String> paramList) {
        List<String> sorted = sort(setElement);

        for (String element : sorted) {
            paramList.put(element, element);
        }
    }

    private ArrayList<String> sort(Set<String> toSort) {
        ArrayList<String> sorted;

        if (this.getSortMode().getIsSorting()) {
            sorted = sortByName(toSort);
            if (this.getSortMode().getIsDescending()) {
                Collections.reverse(sorted);
            }
        } else {
            sorted = new ArrayList<String>(toSort);
        }
        return sorted;
    }

    private FilePathWrapper getWorkspace(AbstractProject<?, ?> project, boolean isRepoScm) throws IOException, InterruptedException {
        FilePathWrapper someWorkspace = new FilePathWrapper(project.getSomeWorkspace());
        if (isRepoScm) {
            FilePath repoDir = new FilePath(someWorkspace.getFilePath(), REPO_MANIFESTS_DIR);
            if (repoDir.exists()) {
                someWorkspace = new FilePathWrapper(repoDir);
            } else {
                someWorkspace = getTemporaryWorkspace();
            }
        } else if (someWorkspace.getFilePath() == null) {
            someWorkspace = getTemporaryWorkspace();
        }
        someWorkspace.getFilePath().mkdirs();
        //Must by not null and exist
        return someWorkspace;
    }

    private FilePathWrapper getTemporaryWorkspace() throws IOException {
        Path temporaryWorkspacePath = Files.createTempDirectory(TEMPORARY_DIRECTORY_PREFIX);
        FilePath filePath = new FilePath(temporaryWorkspacePath.toFile());
        FilePathWrapper filePathWrapper = new FilePathWrapper(filePath);
        filePathWrapper.setThatTemporary();
        return filePathWrapper;
    }

    private EnvVars getEnvironment(AbstractProject<?, ?> project) throws IOException, InterruptedException {
        if (project.getSomeBuildWithWorkspace() != null) {
            return project.getSomeBuildWithWorkspace().getEnvironment(TaskListener.NULL);
        } else {
            return project.getEnvironment(null, TaskListener.NULL);
        }

    }

    private void initWorkspace(FilePathWrapper workspace, GitClient gitClient, URIish remoteURL) throws IOException, InterruptedException {
        if (isEmptyWorkspace(workspace.getFilePath())) {
            gitClient.init();
            gitClient.clone(remoteURL.toASCIIString(), "origin", false, null);
            LOGGER.log(Level.INFO, "generateContents clone done");
        }
    }

    private boolean isEmptyWorkspace(FilePath workspaceDir) throws IOException, InterruptedException {
        return workspaceDir.list().size() == 0;
    }

    private GitClient getGitClient(final AbstractProject<?, ?> project, FilePathWrapper workspace, GitSCM git, EnvVars environment) throws IOException, InterruptedException {
        int nextBuildNumber = project.getNextBuildNumber();

        GitClient gitClient = git.createClient(TaskListener.NULL, environment, new Run(project) {
        }, workspace.getFilePath());

        project.updateNextBuildNumber(nextBuildNumber);
        return gitClient;
    }

    public ArrayList<String> sortByName(Set<String> set) {

        ArrayList<String> tags = new ArrayList<String>(set);

        if (sortMode.getIsUsingSmartSort()) {
            Collections.sort(tags, new SmartNumberStringComparer());
        } else {
            Collections.sort(tags);
        }

        return tags;
    }

    public String getDivUUID() {
        StringBuilder randomSelectName = new StringBuilder();
        randomSelectName.append(getName()).append("-").append(uuid);
        return randomSelectName.toString();
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static class DescriptorImpl extends ParameterDescriptor {
        private GitSCM scm;

        @Override
        public String getDisplayName() {
            return "Git Parameter";
        }

        public ListBoxModel doFillValueItems(@AncestorInPath AbstractProject<?, ?> project, @QueryParameter String param)
                throws IOException, InterruptedException {
            ListBoxModel items = new ListBoxModel();

            scm = getProjectSCM(project);
            if (scm == null) {
                items.add("!No Git repository configured in SCM configuration");
                return items;
            }
            ParametersDefinitionProperty prop = project.getProperty(ParametersDefinitionProperty.class);
            if (prop != null) {
                ParameterDefinition def = prop.getParameterDefinition(param);
                if (def instanceof GitParameterDefinition) {
                    GitParameterDefinition paramDef = (GitParameterDefinition) def;
                    Map<String, String> paramList = paramDef.generateContents(project, scm);

                    for (Map.Entry<String, String> entry : paramList.entrySet()) {
                        items.add(entry.getValue(), entry.getKey());
                    }
                }
            }
            return items;
        }

        public GitSCM getProjectSCM(AbstractProject<?, ?> project) {
            SCM projectScm = null;
            if (project != null) {
                projectScm = project.getScm();
            }

            if (projectScm instanceof GitSCM) {
                return (GitSCM) projectScm;
            }

            if (isRepoScm(projectScm)) {
                return getGitSCMFromRepoScm(projectScm);
            }

            return null;
        }

        private static boolean isRepoScm(SCM projectScm) {
            return projectScm != null && REPO_SCM_CLASS_NAME.equals(projectScm.getClass().getName());
        }

        private static GitSCM getGitSCMFromRepoScm(SCM projectScm) {
            try {
                Class<?> clazz = projectScm.getClass();
                Method method = clazz.getDeclaredMethod("getManifestRepositoryUrl");
                String repositoryUrl = (String) method.invoke(projectScm);
                UserRemoteConfig config = new UserRemoteConfig(repositoryUrl, REPO_SCM_NAME, null, null);
                List<UserRemoteConfig> configs = new ArrayList<UserRemoteConfig>();
                configs.add(config);
                return new GitSCM(configs, null, false, null, null, null, null);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "get repo scm failed", e);
            }
            return null;
        }

        public FormValidation doCheckBranchFilter(@QueryParameter String value) {
            try {
                Pattern.compile(value); // Validate we've got a valid regex.
            } catch (PatternSyntaxException e) {
                return FormValidation.error("The pattern '" + value + "' does not appear to be valid: " + e.getMessage());
            }
            return FormValidation.ok();
        }
    }
}
