package net.uaznia.lukanus.hudson.plugins.gitparameter;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.ParameterValue;
import hudson.model.TaskListener;
import hudson.model.TopLevelItem;
import hudson.model.AbstractProject;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Run;
import hudson.plugins.git.Branch;
import hudson.plugins.git.GitException;
import hudson.plugins.git.Revision;
import hudson.plugins.git.GitSCM;
import hudson.scm.SCM;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.lib.ObjectId;
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

    private final UUID uuid;
    private String type;
    private String branch;
    private String tagFilter;
    private String branchfilter;
    private SortMode sortMode;
    private String errorMessage;
    private String defaultValue;
    private Boolean quickFilterEnabled;

    @DataBoundConstructor
    public GitParameterDefinition(String name, String type, String defaultValue, String description, String branch,
                                  String branchFilter, String tagFilter, SortMode sortMode, Boolean quickFilterEnabled) {

        super(name, description);
        this.type = type;
        this.defaultValue = defaultValue;
        this.branch = branch;
        this.uuid = UUID.randomUUID();
        this.sortMode = sortMode;
        this.quickFilterEnabled = quickFilterEnabled;

        setTagFilter(tagFilter);
        setBranchfilter(branchFilter);
    }

    @Override
    public ParameterValue createValue(StaplerRequest request) {
        String value[] = request.getParameterValues(getName());
        if (value == null) {
            return getDefaultParameterValue();
        }
        return null;
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
        String defValue = getDefaultValue();
        if (!StringUtils.isBlank(defValue)) {
            return new GitParameterValue(getName(), defValue);
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
            this.errorMessage = "wrongType";
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

    public String getBranchfilter() {
        return branchfilter;
    }

    public void setBranchfilter(String branchFilter) {
        if (StringUtils.isEmpty(StringUtils.trim(branchFilter))) {
            branchFilter = "*";
        }

        if (!"*".equals(branchFilter)) {
            try {
                Pattern.compile(branchFilter);
            } catch (PatternSyntaxException e) {
                LOGGER.log(Level.FINE, "Specified branchFilter is not a valid regex. Setting to '*'", e);
            }
        }
        this.branchfilter = branchFilter;
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
        if (pd.uuid.equals(uuid)) {
            return 0;
        }

        return -1;
    }

    public Map<String, String> generateContents(AbstractProject<?, ?> project, GitSCM git) throws IOException, InterruptedException {

        Map<String, String> paramList = new LinkedHashMap<String, String>();
        // for (AbstractProject<?,?> project :
        // Hudson.getInstance().getItems(AbstractProject.class)) {
        if (project.getSomeWorkspace() == null) {
            this.errorMessage = "noWorkspace";
        }

        EnvVars environment = null;

        try {
            environment = project.getSomeBuildWithWorkspace().getEnvironment(TaskListener.NULL);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error ", e);
        }

        for (RemoteConfig repository : git.getRepositories()) {
            LOGGER.log(Level.INFO, "generateContents contenttype " + type + " RemoteConfig " + repository.getURIs());
            for (URIish remoteURL : repository.getURIs()) {
                GitClient newgit = git.createClient(TaskListener.NULL, environment, new Run(project) {
                }, project.getSomeWorkspace());
                FilePath wsDir = null;
                if (project.getSomeBuildWithWorkspace() != null) {
                    wsDir = project.getSomeBuildWithWorkspace().getWorkspace();
                    if (wsDir == null || !wsDir.exists()) {
                        LOGGER.log(Level.WARNING, "generateContents create wsDir " + wsDir + " for " + remoteURL);
                        wsDir.mkdirs();
                        if (!wsDir.exists()) {
                            LOGGER.log(Level.SEVERE, "generateContents wsDir.mkdirs() failed.");
                            String errMsg = "!Failed To Create Workspace";
                            return Collections.singletonMap(errMsg, errMsg);
                        }
                        newgit.init();
                        newgit.clone(remoteURL.toASCIIString(), "origin", false, null);
                        LOGGER.log(Level.INFO, "generateContents clone done");
                    }
                } else {
                    // probably our first build. We cannot yet fill in any
                    // values.
                    LOGGER.log(Level.INFO, "getSomeBuildWithWorkspace is null");
                    String errMsg = "!No workspace. Please build the project at least once";
                    return Collections.singletonMap(errMsg, errMsg);
                }

                long time = -System.currentTimeMillis();

                FetchCommand fetch = newgit.fetch_().prune().from(remoteURL, repository.getFetchRefSpecs());
                fetch.execute();

                LOGGER.finest("Took " + (time + System.currentTimeMillis()) + "ms to fetch");
                if (type.equalsIgnoreCase(PARAMETER_TYPE_REVISION)) {
                    RevisionInfoFactory revisionInfoFactory = new RevisionInfoFactory(newgit,branch);
                    List<RevisionInfo> revisions = revisionInfoFactory.getRevisions();

                    for (RevisionInfo revision : revisions) {
                        paramList.put(revision.getSha1(), revision.getRevisionInfo());
                    }
                }
                if (type.equalsIgnoreCase(PARAMETER_TYPE_TAG) || type.equalsIgnoreCase(PARAMETER_TYPE_TAG_BRANCH)) {

                    Set<String> tagSet = newgit.getTagNames(tagFilter);
                    ArrayList<String> orderedTagNames;

                    if (this.getSortMode().getIsSorting()) {
                        orderedTagNames = sortByName(tagSet);
                        if (this.getSortMode().getIsDescending())
                            Collections.reverse(orderedTagNames);
                    } else {
                        orderedTagNames = new ArrayList<String>(tagSet);
                    }

                    for (String tagName : orderedTagNames) {
                        paramList.put(tagName, tagName);
                    }
                }
                if (type.equalsIgnoreCase(PARAMETER_TYPE_BRANCH) || type.equalsIgnoreCase(PARAMETER_TYPE_TAG_BRANCH)) {
                    time = -System.currentTimeMillis();
                    Set<String> branchSet = new HashSet<String>();
                    final boolean wildcard = "*".equals(branchfilter);
                    for (Branch branch : newgit.getRemoteBranches()) {
                        // It'd be nice if we could filter on remote branches via the GitClient,
                        // but that's not an option.
                        final String branchName = branch.getName();
                        if (wildcard || branchName.matches(branchfilter)) {
                            branchSet.add(branchName);
                        }
                    }
                    LOGGER.finest("Took " + (time + System.currentTimeMillis()) + "ms to fetch branches");

                    time = -System.currentTimeMillis();
                    List<String> orderedBranchNames;
                    if (this.getSortMode().getIsSorting()) {
                        orderedBranchNames = sortByName(branchSet);
                        if (this.getSortMode().getIsDescending())
                            Collections.reverse(orderedBranchNames);
                    } else {
                        orderedBranchNames = new ArrayList<String>(branchSet);
                    }

                    for (String branchName : orderedBranchNames) {
                        paramList.put(branchName, branchName);
                    }
                    LOGGER.finest("Took " + (time + System.currentTimeMillis()) + "ms to sort and add to param list.");
                }
            }
            break;
        }
        return paramList;
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

    public String getErrorMessage() {
        return errorMessage;
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
            return null;
        }

        public FormValidation doCheckBranchfilter(@QueryParameter String value) {
            if (!"*".equals(value)) {
                try {
                    Pattern.compile(value); // Validate we've got a valid regex.
                } catch (PatternSyntaxException e) {
                    return FormValidation.error("The pattern '" + value + "' does not appear to be valid: " + e.getMessage());
                }
            }
            return FormValidation.ok();
        }
    }
}
