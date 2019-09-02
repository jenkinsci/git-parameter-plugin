package net.uaznia.lukanus.hudson.plugins.gitparameter;

import static hudson.util.FormValidation.error;
import static hudson.util.FormValidation.ok;
import static hudson.util.FormValidation.warning;
import static net.uaznia.lukanus.hudson.plugins.gitparameter.Consts.DEFAULT_LIST_SIZE;
import static net.uaznia.lukanus.hudson.plugins.gitparameter.Consts.DEFAULT_REMOTE;
import static net.uaznia.lukanus.hudson.plugins.gitparameter.Consts.EMPTY_JOB_NAME;
import static net.uaznia.lukanus.hudson.plugins.gitparameter.Consts.PARAMETER_TYPE_BRANCH;
import static net.uaznia.lukanus.hudson.plugins.gitparameter.Consts.PULL_REQUEST_REFS_PATTERN;
import static net.uaznia.lukanus.hudson.plugins.gitparameter.Consts.REFS_TAGS_PATTERN;
import static net.uaznia.lukanus.hudson.plugins.gitparameter.Consts.TEMPORARY_DIRECTORY_PREFIX;
import static net.uaznia.lukanus.hudson.plugins.gitparameter.Consts.isAnnotatedTagType;
import static net.uaznia.lukanus.hudson.plugins.gitparameter.Consts.isBranchType;
import static net.uaznia.lukanus.hudson.plugins.gitparameter.Consts.isParameterTypeCorrect;
import static net.uaznia.lukanus.hudson.plugins.gitparameter.Consts.isPullRequestType;
import static net.uaznia.lukanus.hudson.plugins.gitparameter.Consts.isRevisionType;
import static net.uaznia.lukanus.hudson.plugins.gitparameter.Consts.isTagType;
import static net.uaznia.lukanus.hudson.plugins.gitparameter.Messages.GitParameterDefinition_checkConfiguration;
import static net.uaznia.lukanus.hudson.plugins.gitparameter.Messages.GitParameterDefinition_error;
import static net.uaznia.lukanus.hudson.plugins.gitparameter.Messages.GitParameterDefinition_lookAtLog;
import static net.uaznia.lukanus.hudson.plugins.gitparameter.Messages.GitParameterDefinition_noRepositoryConfigured;
import static net.uaznia.lukanus.hudson.plugins.gitparameter.Messages.GitParameterDefinition_returnDefaultValue;
import static net.uaznia.lukanus.hudson.plugins.gitparameter.scms.SCMFactory.getGitSCMs;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.apache.commons.lang.StringUtils.trim;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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

import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.gitclient.FetchCommand;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.cli.CLICommand;
import hudson.model.ChoiceParameterDefinition;
import hudson.model.Job;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Run;
import hudson.model.StringParameterDefinition;
import hudson.model.TaskListener;
import hudson.model.TopLevelItem;
import hudson.plugins.git.GitException;
import hudson.plugins.git.GitSCM;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.uaznia.lukanus.hudson.plugins.gitparameter.jobs.JobWrapper;
import net.uaznia.lukanus.hudson.plugins.gitparameter.jobs.JobWrapperFactory;
import net.uaznia.lukanus.hudson.plugins.gitparameter.model.ItemsErrorModel;
import net.uaznia.lukanus.hudson.plugins.gitparameter.scms.RepoSCM;

public class GitParameterDefinition extends ParameterDefinition implements Comparable<GitParameterDefinition> {
    private static final long serialVersionUID = 9157832967140868122L;
    private static final Logger LOGGER = Logger.getLogger(GitParameterDefinition.class.getName());

    private static final int MAX_COMMIT_MESSAGE_LENGTH = 150;
    private static final String TAG_MESSAGE_SEPARATOR = " || ";

    private final UUID uuid;
    private String type;
    private String branch;
    private String tagFilter;
    private String branchFilter;
    private SortMode sortMode;
    private String defaultValue;
    private SelectedValue selectedValue;
    private String useRepository;
    private Boolean quickFilterEnabled;
    private String listSize;

    @DataBoundConstructor
    public GitParameterDefinition(String name, String type, String defaultValue, String description, String branch,
                                  String branchFilter, String tagFilter, SortMode sortMode, SelectedValue selectedValue,
                                  String useRepository, Boolean quickFilterEnabled) {
        super(name, description);
        this.defaultValue = defaultValue;
        this.branch = branch;
        this.uuid = UUID.randomUUID();
        this.sortMode = sortMode;
        this.selectedValue = selectedValue;
        this.quickFilterEnabled = quickFilterEnabled;
        this.listSize = DEFAULT_LIST_SIZE;

        setUseRepository(useRepository);
        setType(type);
        setTagFilter(tagFilter);
        setBranchFilter(branchFilter);
    }

    @Override
    public ParameterValue createValue(StaplerRequest request) {
        String value[] = request.getParameterValues(getName());
        if (value == null || value.length == 0 || isBlank(value[0])) {
            return getDefaultParameterValue();
        }
        return new GitParameterValue(getName(), value[0]);
    }

    @Override
    public ParameterValue createValue(StaplerRequest request, JSONObject jO) {
        Object value = jO.get("value");
        StringBuilder strValue = new StringBuilder();
        if (value instanceof String) {
            strValue.append(value);
        } else if (value instanceof JSONArray) {
            JSONArray jsonValues = (JSONArray) value;
            for (int i = 0; i < jsonValues.size(); i++) {
                strValue.append(jsonValues.getString(i));
                if (i < jsonValues.size() - 1) {
                    strValue.append(",");
                }
            }
        }

        if (strValue.length() == 0) {
            strValue.append(defaultValue);
        }

        GitParameterValue gitParameterValue = new GitParameterValue(jO.getString("name"), strValue.toString());
        return gitParameterValue;
    }

    @Override
    public ParameterValue createValue(CLICommand command, String value) throws IOException, InterruptedException {
        if (isNotEmpty(value)) {
            return new GitParameterValue(getName(), value);
        }
        return getDefaultParameterValue();
    }

    @Override
    public ParameterValue getDefaultParameterValue() {
        //If 'Default Value' is set has high priority!
        String defValue = getDefaultValue();
        if (!isBlank(defValue)) {
            return new GitParameterValue(getName(), defValue);
        }

        switch (getSelectedValue()) {
            case TOP:
                try {
                    ItemsErrorModel valueItems = getDescriptor().doFillValueItems(getParentJob(), getName());
                    if (valueItems.size() > 0) {
                        return new GitParameterValue(getName(), valueItems.get(0).value);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, getCustomeJobName() + " " + Messages.GitParameterDefinition_unexpectedError(), e);
                }
                break;
            case DEFAULT:
            case NONE:
            default:
                return super.getDefaultParameterValue();
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

    public String getBranch() {
        return this.branch;
    }

    public void setBranch(String nameOfBranch) {
        this.branch = nameOfBranch;
    }

    public SortMode getSortMode() {
        return this.sortMode == null ? SortMode.NONE : this.sortMode;
    }

    public void setSortMode(SortMode sortMode) {
        this.sortMode = sortMode;
    }

    public String getTagFilter() {
        return this.tagFilter;
    }

    public void setTagFilter(String tagFilter) {
        if (isEmpty(trim(tagFilter))) {
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
        if (isEmpty(trim(branchFilter))) {
            branchFilter = ".*";
        }

        this.branchFilter = branchFilter;
    }

    public String getListSize() {
        return listSize == null ? DEFAULT_LIST_SIZE : listSize;
    }

    @DataBoundSetter
    public void setListSize(String listSize) {
        this.listSize = listSize;
    }

    public SelectedValue getSelectedValue() {
        return selectedValue == null ? SelectedValue.TOP : selectedValue;
    }

    public Boolean getQuickFilterEnabled() {
        return quickFilterEnabled;
    }

    public Job getParentJob() {
        Job context = null;
        List<Job> jobs = Jenkins.getInstance().getAllItems(Job.class);

        for (Job job : jobs) {
            if (!(job instanceof TopLevelItem)) continue;

            ParametersDefinitionProperty property = (ParametersDefinitionProperty) job.getProperty(ParametersDefinitionProperty.class);

            if (property != null) {
                List<ParameterDefinition> parameterDefinitions = property.getParameterDefinitions();

                if (parameterDefinitions != null) {
                    for (ParameterDefinition pd : parameterDefinitions) {
                        if (pd instanceof GitParameterDefinition && ((GitParameterDefinition) pd).compareTo(this) == 0) {
                            context = job;
                            break;
                        }
                    }
                }
            }
        }

        return context;
    }

    @SuppressFBWarnings(value="EQ_COMPARETO_USE_OBJECT_EQUALS")
    public int compareTo(GitParameterDefinition pd) {
        return pd.uuid.equals(uuid) ? 0 : -1;
    }

    public ItemsErrorModel generateContents(JobWrapper jobWrapper, List<GitSCM> scms) {
        try {
            Map<String, String> paramList = new LinkedHashMap<String, String>();
            EnvVars environment = getEnvironment(jobWrapper);
            Set<String> usedRepository = new HashSet<>();
            outForLoops:
            for (GitSCM git : scms) {
                for (RemoteConfig repository : git.getRepositories()) {
                    GitClient gitClient = getGitClient(jobWrapper, null, git, environment);
                    for (URIish remoteURL : repository.getURIs()) {

                        String gitUrl = Util.replaceMacro(remoteURL.toPrivateASCIIString(), environment);
                        if (notMatchUseRepository(gitUrl) || usedRepository.contains(gitUrl)) {
                            continue;
                        }

                        if (isTagType(type)) {
                            Set<String> tagSet = getTag(gitClient, gitUrl);
                            sortAndPutToParam(tagSet, paramList);
                        }

                        if (isAnnotatedTagType(type)) {
                            synchronized (GitParameterDefinition.class) {
	                            Map<String, String> tagSet = getAnnotatedTag(jobWrapper, git, paramList, environment, repository, remoteURL);
	                            sortAnnoTagsAndPutToParam(tagSet, paramList);
                            }
                        }

                        if (isBranchType(type)) {
                            Set<String> branchSet = getBranch(gitClient, gitUrl, repository.getName());
                            sortAndPutToParam(branchSet, paramList);
                        }


                        if (isPullRequestType(type)) {
                            Set<String> pullRequestSet = getPullRequest(gitClient, gitUrl);
                            sortAndPutToParam(pullRequestSet, paramList);
                        }

                        if (isRevisionType(type)) {
                            synchronized (GitParameterDefinition.class) {
                                getRevision(jobWrapper, git, paramList, environment, repository, remoteURL);
                            }
                        }

                        if (isBlank(useRepository)) {
                            break outForLoops;
                        }
                        usedRepository.add(gitUrl);
                    }
                }
            }
            return convertMapToListBox(paramList);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, getCustomeJobName() + " " + Messages.GitParameterDefinition_unexpectedError(), e);
            return ItemsErrorModel.create(getDefaultValue(), GitParameterDefinition_returnDefaultValue(), GitParameterDefinition_error(), e.getMessage(), GitParameterDefinition_lookAtLog(), GitParameterDefinition_checkConfiguration());
        }
    }

    private ItemsErrorModel convertMapToListBox(Map<String, String> paramList) {
        ItemsErrorModel items = new ItemsErrorModel();
        for (Map.Entry<String, String> entry : paramList.entrySet()) {
            items.add(entry.getKey(), entry.getValue());
        }
        return items;
    }

    private boolean notMatchUseRepository(String gitUrl) {
        if (isBlank(useRepository)) {
            return false;
        }
        Pattern repositoryNamePattern;
        try {
            repositoryNamePattern = Pattern.compile(useRepository);
        } catch (Exception e) {
            LOGGER.log(Level.INFO, Messages.GitParameterDefinition_invalidUseRepositoryPattern(useRepository), e.getMessage());
            return false;
        }
        return !repositoryNamePattern.matcher(gitUrl).find();
    }

    private Set<String> getTag(GitClient gitClient, String gitUrl) throws InterruptedException {
        Set<String> tagSet = new HashSet<String>();
        try {
            Map<String, ObjectId> tags = gitClient.getRemoteReferences(gitUrl, tagFilter, false, true);
            for (String tagName : tags.keySet()) {
                tagSet.add(tagName.replaceFirst(REFS_TAGS_PATTERN, ""));
            }
        } catch (GitException e) {
            LOGGER.log(Level.WARNING, getCustomeJobName() + " " + Messages.GitParameterDefinition_getTag(), e);
        }
        return tagSet;
    }
    

    private Map<String, String> getAnnotatedTag(JobWrapper jobWrapper, GitSCM git, Map<String, String> paramList, EnvVars environment, RemoteConfig repository, URIish remoteURL) throws IOException, InterruptedException {
    	boolean isRepoScm = RepoSCM.isRepoSCM(repository.getName());
        FilePathWrapper workspace = getWorkspace(jobWrapper, isRepoScm);

        GitClient gitClient = getGitClient(jobWrapper, workspace, git, environment);
        initWorkspace(workspace, gitClient, remoteURL);
        FetchCommand fetch = gitClient.fetch_().prune().from(remoteURL, repository.getFetchRefSpecs());
        fetch.execute();
        
    	Map<String, String>  tagMap = new HashMap<String, String>();
        try {
            Set<String> tags = gitClient.getTagNames(tagFilter);
            for (String tagName : tags) {
        		tagName = tagName.replaceFirst(REFS_TAGS_PATTERN, "");
        		String tagMessage = getTagMessage(gitClient, tagName);
        		tagMessage = trimMessage(tagMessage);
        		if(tagMessage.isEmpty()) {
        			tagMessage = tagName;
        		} else {
        			tagMessage = tagName.concat(TAG_MESSAGE_SEPARATOR).concat(tagMessage);
        		}
        		tagMap.put(tagMessage, tagName);
            }
        } catch (GitException e) {
            LOGGER.log(Level.WARNING, getCustomeJobName() + " " + Messages.GitParameterDefinition_getTag(), e);
        }
        
        return tagMap;
    }
    private String getTagMessage(GitClient gitClient, String tagName) throws GitException, InterruptedException {
    	return gitClient.getTagMessage(tagName).trim();
    }
    
    private String trimMessage(String commitMessage) {
        if (commitMessage.length() > MAX_COMMIT_MESSAGE_LENGTH) {
            int lastSpace = commitMessage.lastIndexOf(" ", MAX_COMMIT_MESSAGE_LENGTH);
            return commitMessage.substring(0, lastSpace) + " ...";
        }
        return commitMessage;
    }
    
    
    private Set<String> getBranch(GitClient gitClient, String gitUrl, String remoteName) throws Exception {
        Set<String> branchSet = new HashSet<>();
        Pattern branchFilterPattern = compileBranchFilterPattern();

        Map<String, ObjectId> branches = gitClient.getRemoteReferences(gitUrl, null, true, false);
        Iterator<String> remoteBranchesName = branches.keySet().iterator();
        while (remoteBranchesName.hasNext()) {
            String branchName = strip(remoteBranchesName.next(), remoteName);
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

    private Set<String> getPullRequest(GitClient gitClient, String gitUrl) throws Exception {
        Set<String> pullRequestSet = new HashSet<>();
        Map<String, ObjectId> remoteReferences = gitClient.getRemoteReferences(gitUrl, null, false, false);
        for (String remoteReference : remoteReferences.keySet()) {
            Matcher matcher = PULL_REQUEST_REFS_PATTERN.matcher(remoteReference);
            if (matcher.find()) {
                pullRequestSet.add(matcher.group(1));
            }
        }
        return pullRequestSet;
    }

    private Pattern compileBranchFilterPattern() {
        Pattern branchFilterPattern;
        try {
            branchFilterPattern = Pattern.compile(branchFilter);
        } catch (Exception e) {
            LOGGER.log(Level.INFO, getCustomeJobName() + " " + Messages.GitParameterDefinition_branchFilterNotValid(), e.getMessage());
            branchFilterPattern = Pattern.compile(".*");
        }
        return branchFilterPattern;
    }

    //hudson.plugins.git.Branch.strip
    private String strip(String name, String remote) {
        return remote + "/" + name.substring(name.indexOf('/', 5) + 1);
    }

    /**
     * Unfortunately, to get the revisions should do fetch
     */
    private void getRevision(JobWrapper jobWrapper, GitSCM git, Map<String, String> paramList, EnvVars environment, RemoteConfig repository, URIish remoteURL) throws IOException, InterruptedException {
        boolean isRepoScm = RepoSCM.isRepoSCM(repository.getName());
        FilePathWrapper workspace = getWorkspace(jobWrapper, isRepoScm);

        GitClient gitClient = getGitClient(jobWrapper, workspace, git, environment);
        initWorkspace(workspace, gitClient, remoteURL);
        FetchCommand fetch = gitClient.fetch_().prune().from(remoteURL, repository.getFetchRefSpecs());
        fetch.execute();

        RevisionInfoFactory revisionInfoFactory = new RevisionInfoFactory(gitClient, branch);
        List<RevisionInfo> revisions = revisionInfoFactory.getRevisions();

        for (RevisionInfo revision : revisions) {
            paramList.put(revision.getSha1(), revision.getRevisionInfo());
        }

        workspace.delete();
    }


    private void sortAndPutToParam(Set<String> setElement, Map<String, String> paramList) {
        List<String> sorted = sort(setElement);

        for (String element : sorted) {
            paramList.put(element, element);
        }
    }


    private void sortAnnoTagsAndPutToParam(Map<String, String> tags, Map<String, String> paramList) {
    	Set<String> setElement = tags.keySet();
        List<String> sorted = sort(setElement);

        for (String element : sorted) {
            paramList.put(element, tags.get(element));
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
            sorted = new ArrayList<>(toSort);
        }
        return sorted;
    }

    private FilePathWrapper getWorkspace(JobWrapper jobWrapper, boolean isRepoScm) throws IOException, InterruptedException {
        FilePathWrapper someWorkspace = new FilePathWrapper(jobWrapper.getSomeWorkspace());
        if (isRepoScm) {
            FilePath repoDir = new FilePath(someWorkspace.getFilePath(), RepoSCM.getRepoMainfestsDir());
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

    @SuppressFBWarnings(value="NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification="Jenkins.get() is not null")
    private EnvVars getEnvironment(JobWrapper jobWrapper) throws IOException, InterruptedException {
        EnvVars environment = jobWrapper.getEnvironment(Jenkins.get().toComputer().getNode(), TaskListener.NULL);

        EnvVars buildEnvironment = jobWrapper.getSomeBuildEnvironments();
        addEnvironmentIfNotExists(environment, buildEnvironment);

        EnvVars jobDefautEnvironment = getJobDefaultEnvironment(jobWrapper);
        addEnvironmentIfNotExists(environment, jobDefautEnvironment);

        EnvVars.resolve(environment);
        return environment;
    }

    private void addEnvironmentIfNotExists(EnvVars environment, EnvVars otherEnvironment) {
        if (otherEnvironment == null) {
            return;
        }

        for (Map.Entry<String, String> entry : otherEnvironment.entrySet()) {
            String key = entry.getKey();
            if (!environment.containsKey(key)) {
                environment.put(key, entry.getValue());
            }
        }
    }

    private EnvVars getJobDefaultEnvironment(JobWrapper jobWrapper) {
        EnvVars environment = new EnvVars();
        ParametersDefinitionProperty property = (ParametersDefinitionProperty) jobWrapper.getJob().getProperty(ParametersDefinitionProperty.class);
        if (property != null) {
            for (ParameterDefinition parameterDefinition : property.getParameterDefinitions()) {
                if (parameterDefinition != null && isAcceptedParameterClass(parameterDefinition)) {
                    checkAndAddDefaultParameterValue(parameterDefinition, environment);
                }
            }
        }
        return environment;
    }

    private boolean isAcceptedParameterClass(ParameterDefinition parameterDefinition) {
        return parameterDefinition instanceof StringParameterDefinition
                || parameterDefinition instanceof ChoiceParameterDefinition;
    }

    private void checkAndAddDefaultParameterValue(ParameterDefinition parameterDefinition, EnvVars environment) {
        ParameterValue defaultParameterValue = parameterDefinition.getDefaultParameterValue();
        if (defaultParameterValue != null && defaultParameterValue.getValue() != null && defaultParameterValue.getValue() instanceof String) {
            environment.put(parameterDefinition.getName(), (String) defaultParameterValue.getValue());
        }
    }

    private void initWorkspace(FilePathWrapper workspace, GitClient gitClient, URIish remoteURL) throws IOException, InterruptedException {
        if (isEmptyWorkspace(workspace.getFilePath())) {
            gitClient.init();
            gitClient.clone(remoteURL.toASCIIString(), DEFAULT_REMOTE, false, null);
            LOGGER.log(Level.INFO, getCustomeJobName() + " " + Messages.GitParameterDefinition_genContentsCloneDone());
        }
    }

    private boolean isEmptyWorkspace(FilePath workspaceDir) throws IOException, InterruptedException {
        return workspaceDir.list().size() == 0;
    }

    private GitClient getGitClient(final JobWrapper jobWrapper, FilePathWrapper workspace, GitSCM git, EnvVars environment) throws IOException, InterruptedException {
        Run build = new Run(jobWrapper.getJob(), System.currentTimeMillis()) {};

        return git.createClient(TaskListener.NULL, environment, build, workspace != null ? workspace.getFilePath() : null);
    }

    public ArrayList<String> sortByName(Set<String> set) {

        ArrayList<String> tags = new ArrayList<String>(set);

        if (getSortMode().getIsUsingSmartSort()) {
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

    public String getUseRepository() {
        return useRepository;
    }

    public void setUseRepository(String useRepository) {
        this.useRepository = isBlank(useRepository) ? null : useRepository;
    }

    public String getCustomeJobName() {
        Job job = getParentJob();
        String fullName = job != null ? job.getFullName() : EMPTY_JOB_NAME;
        return "[ " + fullName + " ] ";
    }

    @Symbol("gitParameter")
    @Extension
    public static class DescriptorImpl extends ParameterDescriptor {
        private boolean showNeedToCloneInformation = true;

        public DescriptorImpl() {
            load();
        }

        @Override
        public String getDisplayName() {
            return Messages.GitParameterDefinition_DisplayName();
        }

        public ItemsErrorModel doFillValueItems(@AncestorInPath Job job, @QueryParameter String param) {
            JobWrapper jobWrapper = JobWrapperFactory.createJobWrapper(job);

            ParametersDefinitionProperty prop = jobWrapper.getProperty(ParametersDefinitionProperty.class);
            if (prop != null) {
                ParameterDefinition def = prop.getParameterDefinition(param);
                if (def instanceof GitParameterDefinition) {
                    GitParameterDefinition paramDef = (GitParameterDefinition) def;

                    String repositoryName = paramDef.getUseRepository();
                    List<GitSCM> scms = getGitSCMs(jobWrapper, repositoryName);
                    if (scms == null || scms.isEmpty()) {
                        String useRepositoryMessage = getUseRepositoryMessage(repositoryName);
                        return ItemsErrorModel.create(paramDef.getDefaultValue(), GitParameterDefinition_returnDefaultValue(), GitParameterDefinition_noRepositoryConfigured(), useRepositoryMessage, GitParameterDefinition_checkConfiguration());
                    }

                    return paramDef.generateContents(jobWrapper, scms);
                }
            }
            return ItemsErrorModel.EMPTY;
        }

        private String getUseRepositoryMessage(String repositoryName) {
            return isNotBlank(repositoryName) ? Messages.GitParameterDefinition_useRepositoryMessage(repositoryName): EMPTY;
        }

        public FormValidation doCheckDefaultValue(@QueryParameter String defaultValue) {
            return isBlank(defaultValue) ? warning(Messages.GitParameterDefinition_requiredDefaultValue()): ok();
        }

        public FormValidation doCheckBranchFilter(@QueryParameter String value) {
            String errorMessage = Messages.GitParameterDefinition_invalidBranchPattern(value);
            return validationRegularExpression(value, errorMessage);
        }

        public FormValidation doCheckUseRepository(@QueryParameter String value) {
            String errorMessage = Messages.GitParameterDefinition_invalidUseRepositoryPattern(value);
            return validationRegularExpression(value, errorMessage);
        }

        private FormValidation validationRegularExpression(String value, String errorMessage) {
            try {
                Pattern.compile(value); // Validate we've got a valid regex.
            } catch (PatternSyntaxException e) {
                LOGGER.log(Level.WARNING, errorMessage, e);
                return error(errorMessage);
            }
            return ok();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            showNeedToCloneInformation = json.getBoolean("showNeedToCloneInformation");
            save();
            return super.configure(req, json);
        }

        public boolean getShowNeedToCloneInformation() {
            return showNeedToCloneInformation;
        }
    }
}
