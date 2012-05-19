package net.uaznia.lukanus.hudson.plugins.gitparameter;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.TaskListener;
import hudson.plugins.git.GitAPI;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.IGitAPI;
import hudson.plugins.git.Revision;
import hudson.scm.SCM;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class GitParameterDefinition
        extends ParameterDefinition
        implements Comparable<GitParameterDefinition> {
    private static final long serialVersionUID = 9157832967140868122L;

    public static final String PARAMETER_TYPE_TAG      = "PT_TAG";
    public static final String PARAMETER_TYPE_REVISION = "PT_REVISION";
    public static final String PARAMETER_TYPE_BRANCH   = "PT_BRANCH";

    private final UUID uuid;

    @Extension
    public static class DescriptorImpl extends ParameterDescriptor {
        @Override
        public String getDisplayName() {
            return "Git Parameter";
        }
    }

    private String type;
    private String branch;

    private String errorMessage;
    private String defaultValue;

    private Map<String, String> revisionMap;
    private Map<String, String> tagMap;
    private Map<String, String> branchMap;

    @DataBoundConstructor
    public GitParameterDefinition(String name,
                                  String type, String defaultValue,
                                  String description, String branch) {
        super(name, description);
        this.type = type;
        this.defaultValue = defaultValue;
        this.branch = branch;
        this.uuid = UUID.randomUUID();
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
            strValue = (String)value;
        }
        else if (value instanceof JSONArray) {
            JSONArray jsonValues = (JSONArray)value;

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
        if (type.equals(PARAMETER_TYPE_TAG) ||
                type.equals(PARAMETER_TYPE_REVISION) ||
                type.equals(PARAMETER_TYPE_BRANCH)) {
            this.type = type;
        }
        else {
            this.errorMessage = "wrongType";
        }
    }

    public String getBranch() {
        return this.branch;
    }

    public void setBranch(String nameOfBranch) {
        this.branch = nameOfBranch;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public AbstractProject<?,?> getParentProject() {
        AbstractProject<?,?> context = null;
        List<AbstractProject> jobs = Hudson.getInstance().getItems(AbstractProject.class);

        for (AbstractProject<?,?> project : jobs) {
            ParametersDefinitionProperty property = project.getProperty(ParametersDefinitionProperty.class);

            if (property != null) {
                List<ParameterDefinition> parameterDefinitions = property.getParameterDefinitions();

                if (parameterDefinitions != null) {
                    for (ParameterDefinition pd : parameterDefinitions) {
                        if (pd instanceof GitParameterDefinition &&
                                ((GitParameterDefinition) pd).compareTo(this) == 0)
                        {
                            context = project;
                            break;
                        }
                    }
                }
            }
        }

        return context;
    }

    @Override
    public int compareTo(GitParameterDefinition pd) {
        if (pd.uuid.equals(uuid)) {
            return 0;
        }
        return -1;
    }

    public void generateContents(String contentType) {
        AbstractProject<?,?> project = getParentProject();

        if (project.getSomeWorkspace() == null) {
            this.errorMessage = "noWorkspace";
        }

        SCM scm = project.getScm();

        if (scm instanceof GitSCM) {
            this.errorMessage = "notGit";
        }

        GitSCM git = (GitSCM)scm;

        String defaultGitExe = File.separatorChar != '/' ? "git.exe" : "git";

        GitSCM.DescriptorImpl desc = (GitSCM.DescriptorImpl) git.getDescriptor();
        if (desc.getOldGitExe() != null) {
            defaultGitExe = desc.getOldGitExe();
        }

        EnvVars environment = null;

        try {
            environment = project.getSomeBuildWithWorkspace().getEnvironment(TaskListener.NULL);
        } catch(Exception e)
        {}

        for (RemoteConfig repository : git.getRepositories()) {
            for (URIish remoteURL : repository.getURIs()) {
                IGitAPI newgit = new GitAPI(defaultGitExe,
                        project.getSomeWorkspace(), TaskListener.NULL,
                        environment, new String());

                newgit.fetch();

                if (type.equalsIgnoreCase(PARAMETER_TYPE_REVISION)) {
                    revisionMap = new HashMap<String, String>();

                    List<ObjectId> oid;

                    if (this.branch != null && !this.branch.isEmpty()) {
                        oid = newgit.revListBranch(this.branch);
                    }
                    else {
                        oid = newgit.revListAll();
                    }

                    for (ObjectId noid: oid) {
                        Revision r = new Revision(noid);
                        List<String> test3 = newgit.showRevision(r);
                        String[] authorDate = test3.get(3).split(">");
                        String author = authorDate[0].replaceFirst("author ", "").replaceFirst("committer ", "") + ">";
                        String goodDate = null;

                        try {
                            String totmp = authorDate[1].trim().split("\\+")[0].trim();
                            long timestamp = Long.parseLong(totmp,10) * 1000;
                            Date date = new Date();
                            date.setTime(timestamp);

                            goodDate = new SimpleDateFormat("yyyy:mm:dd").format(date);
                        }
                        catch (Exception e) {
                            e.toString();
                        }
                        revisionMap.put(r.getSha1String(), r.getSha1String() + " " + author + " " + goodDate);
                    }
                }
                else if(type.equalsIgnoreCase(PARAMETER_TYPE_TAG)) {
                    tagMap = new HashMap<String, String>();

                    for(String tagName : newgit.getTagNames("*")) {
                        tagMap.put(tagName, tagName);
                    }
                }
                else if (type.equalsIgnoreCase(PARAMETER_TYPE_BRANCH)) {
                    branchMap = new TreeMap<String, String>();

                    for (hudson.plugins.git.Branch gitBranch : newgit.getBranches()) {
                        String branchName = gitBranch.getName();

                        if ("master".equals(branchName)) {
                            continue;
                        }

                        branchMap.put(branchName, branchName);
                    }
                }
            }
        }
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Map<String, String> getRevisionMap() {
        if (revisionMap == null || revisionMap.isEmpty()) {
            generateContents(PARAMETER_TYPE_REVISION);
        }
        return revisionMap;
    }

    public Map<String, String> getTagMap() {
        if (tagMap == null || tagMap.isEmpty()) {
            generateContents(PARAMETER_TYPE_TAG);
        }
        return tagMap;
    }

    public Map<String, String> getBranchMap() {
        if (branchMap == null || branchMap.isEmpty()) {
            generateContents(PARAMETER_TYPE_BRANCH);
        }
        return branchMap;
    }
}
