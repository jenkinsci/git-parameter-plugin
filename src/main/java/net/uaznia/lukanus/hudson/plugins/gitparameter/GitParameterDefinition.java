package net.uaznia.lukanus.hudson.plugins.gitparameter;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.TopLevelItem;
import hudson.plugins.git.GitException;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.Revision;
import hudson.scm.SCM;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class GitParameterDefinition extends ParameterDefinition implements
		Comparable<GitParameterDefinition> {
	private static final long serialVersionUID = 9157832967140868122L;

	public static final String PARAMETER_TYPE_TAG = "PT_TAG";
	public static final String PARAMETER_TYPE_REVISION = "PT_REVISION";
	public static final String PARAMETER_TYPE_BRANCH = "PT_BRANCH";
	public static final String PARAMETER_TYPE_TAG_BRANCH = "PT_BRANCH_TAG";
	public static final String REMOTE_ORIGIN = "origin";

	private final UUID uuid;
	private static final Logger LOGGER = Logger
			.getLogger(GitParameterDefinition.class.getName());

	private String type;
	private String branch;
	private String filter;

	private SortMode sortMode;

	private String errorMessage;
	private String defaultValue;

	@DataBoundConstructor
	public GitParameterDefinition(String name, String type,
			String defaultValue, String description, String branch,
			String filter, SortMode sortMode) {
		super(name, description);
		this.type = type;
		this.defaultValue = defaultValue;
		this.branch = branch;
		this.uuid = UUID.randomUUID();
		this.sortMode = sortMode;

		setFilter(filter);
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

		GitParameterValue gitParameterValue = new GitParameterValue(
				jO.getString("name"), strValue);
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
		if (type.equals(PARAMETER_TYPE_TAG)
				|| type.equals(PARAMETER_TYPE_REVISION)
				|| type.equals(PARAMETER_TYPE_BRANCH)
				|| type.equals(PARAMETER_TYPE_TAG_BRANCH)) {
			this.type = type;
		} else {
			this.errorMessage = "wrongType";
		}
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

	public String getFilter() {
		return this.filter;
	}

	public void setFilter(String filter) {
		filter = validateFilter(filter);
		if (isNullOrWhitespace(filter)) {
			filter = "*";
		}
		this.filter = filter;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public AbstractProject<?, ?> getParentProject() {
		AbstractProject<?, ?> context = null;
		List<AbstractProject> jobs = Jenkins.getInstance().getAllItems(AbstractProject.class);

		for (AbstractProject<?, ?> project : jobs) {
			if (!(project instanceof TopLevelItem)) continue;

			ParametersDefinitionProperty property = project
					.getProperty(ParametersDefinitionProperty.class);

			if (property != null) {
				List<ParameterDefinition> parameterDefinitions = property
						.getParameterDefinitions();

				if (parameterDefinitions != null) {
					for (ParameterDefinition pd : parameterDefinitions) {

						if (pd instanceof GitParameterDefinition
								&& ((GitParameterDefinition) pd)
										.compareTo(this) == 0) {

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

	public String prettyRevisionInfo(GitClient newgit, Revision r) {
		List<String> test3 = null;
		try {
			test3 = newgit.showRevision(r.getSha1());
		} catch (GitException e1) {
			return "";
		} catch (InterruptedException e1) {
			return "";
		}
		String[] authorDate = test3.get(3).split(">");
		String author = authorDate[0].replaceFirst("author ", "").replaceFirst(
				"committer ", "")
				+ ">";
		String goodDate = null;
		try {
			String totmp = authorDate[1].trim().split("\\+")[0].trim();
			long timestamp = Long.parseLong(totmp, 10) * 1000;
			Date date = new Date();
			date.setTime(timestamp);

			goodDate = new SimpleDateFormat("yyyy:MM:dd HH:mm").format(date);

		} catch (Exception e) {
			e.toString();
		}
		return r.getSha1String().substring(0, 8) + " " + goodDate + " "
				+ author;
	}

	public Map<String, String> generateContents(AbstractProject<?, ?> project,
			GitSCM git) throws IOException, InterruptedException {

		Map<String, String> paramList = new LinkedHashMap<String, String>();

		EnvVars environment = project.getEnvironment(null, TaskListener.NULL);

		try {
			environment = project.getSomeBuildWithWorkspace().getEnvironment(
					TaskListener.NULL);
		} catch (Exception e) {
		}

		for (RemoteConfig repository : git.getRepositories()) {
			LOGGER.log(Level.INFO, "generateContents contenttype " + type
					+ " RemoteConfig " + repository.getURIs());
			for (URIish remoteURL : repository.getURIs()) {
				GitClient newgit = git.createClient(TaskListener.NULL, environment, new Run(project) {}, null);

				if (type.equalsIgnoreCase(PARAMETER_TYPE_REVISION)) {
					// If we use the GitClient#getRemoteReferences function, we don't
					// need to do a fetch to get tags or branches.
					FetchCommand fetch = newgit.fetch_().from(remoteURL,
							repository.getFetchRefSpecs());
					fetch.execute();
					List<ObjectId> oid;

					if (this.branch != null && !this.branch.isEmpty()) {
						oid = newgit.revList(this.branch);
					} else {
						oid = newgit.revListAll();
					}

					for (ObjectId noid : oid) {
						Revision r = new Revision(noid);
						paramList.put(r.getSha1String(),
								prettyRevisionInfo(newgit, r));
					}
				}
				if (type.equalsIgnoreCase(PARAMETER_TYPE_TAG)
						|| type.equalsIgnoreCase(PARAMETER_TYPE_BRANCH)
						|| type.equalsIgnoreCase(PARAMETER_TYPE_TAG_BRANCH)) {
					// We can't rely on the filter paramter, so it's null.
					// Different implementations support different filters. At least one only glob, at least one both.
					// We'll filter things out ourselves below.
					final Set<String> refSet =
							newgit.getRemoteReferences(
									remoteURL.toString(),
									null,
									type.equalsIgnoreCase(PARAMETER_TYPE_BRANCH) || type.equalsIgnoreCase(PARAMETER_TYPE_TAG_BRANCH),
									type.equalsIgnoreCase(PARAMETER_TYPE_TAG) || type.equalsIgnoreCase(PARAMETER_TYPE_TAG_BRANCH)).keySet();

					final ArrayList<String> orderedRefNames;
					if (this.getSortMode().getIsSorting()) {
						orderedRefNames = sortByName(refSet);
						if (this.getSortMode().getIsDescending())
							Collections.reverse(orderedRefNames);
					} else {
						orderedRefNames = new ArrayList<String>(refSet);
					}

					boolean isWildcard = filter == null || "*".equals(filter);
					for (String refName : orderedRefNames) {
						if (isWildcard || refName.matches(filter)) {
							if (refName.startsWith("refs/heads/")) {
								refName = refName.substring("refs/heads/".length());
							} else if (refName.startsWith("refs/tags/")) {
								refName = refName.substring("refs/tags/".length());
							}
							paramList.put(refName, refName);
						}
					}
				}
			}
			break;
		}
		return paramList;
	}

	public ArrayList<String> sortByName(Set<String> set) {

		ArrayList<String> tags = new ArrayList<String>(set);

		if (!this.getSortMode().getIsUsingSmartSort()) {
			Collections.sort(tags);
		} else {
			Collections.sort(tags, new SmartNumberStringComparer());
		}

		return tags;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	private static boolean isNullOrWhitespace(String s) {
		return s == null || isWhitespace(s);

	}

	private static boolean isWhitespace(String s) {
		int length = s.length();
		for (int i = 0; i < length; i++) {
			if (!Character.isWhitespace(s.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	enum SortMode {
		NONE, ASCENDING_SMART, DESCENDING_SMART, ASCENDING, DESCENDING;

		public boolean getIsUsingSmartSort() {
			return this == SortMode.ASCENDING_SMART
					|| this == SortMode.DESCENDING_SMART;
		}

		public boolean getIsDescending() {
			return this == SortMode.DESCENDING
					|| this == SortMode.DESCENDING_SMART;
		}

		public boolean getIsSorting() {
			return this != SortMode.NONE;
		}
	}

	/**
	 * Compares strings but treats a sequence of digits as a single character.
	 */
	static class SmartNumberStringComparer implements Comparator<String> {

		/**
		 * Gets the token starting at the given index. It will return the first
		 * char if it is not a digit, otherwise it will return all consecutive
		 * digits starting at index.
		 *
		 * @param str
		 *            The string to extract token from
		 * @param index
		 *            The start location
		 */
		private String getToken(String str, int index) {
			char nextChar = str.charAt(index++);
			String token = String.valueOf(nextChar);

			// if the first char wasn't a digit then we're already done
			if (!Character.isDigit(nextChar))
				return token;

			// the first char was a digit so continue until end of string or non
			// digit
			while (index < str.length()) {
				nextChar = str.charAt(index++);

				if (!Character.isDigit(nextChar))
					break;

				token += nextChar;
			}

			return token;
		}

		/**
		 * True if the string only contains digits
		 */
		private boolean stringContainsInteger(String str) {
			for (int charIndex = 0; charIndex < str.length(); charIndex++) {
				if (!Character.isDigit(str.charAt(charIndex)))
					return false;
			}
			return true;
		}

		public int compare(String a, String b) {

			int aIndex = 0;
			int bIndex = 0;

			while (aIndex < a.length() && bIndex < b.length()) {
				String aToken = getToken(a, aIndex);
				String bToken = getToken(b, bIndex);
				int difference;

				if (stringContainsInteger(aToken)
						&& stringContainsInteger(bToken)) {
					int aInt = Integer.parseInt(aToken);
					int bInt = Integer.parseInt(bToken);
					difference = aInt - bInt;
				} else {
					difference = aToken.compareTo(bToken);
				}

				if (difference != 0)
					return difference;

				aIndex += aToken.length();
				bIndex += bToken.length();
			}

			return new Integer(a.length()).compareTo(new Integer(b.length()));
		}

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

		public ListBoxModel doFillValueItems(
				@AncestorInPath AbstractProject<?, ?> project,
				@QueryParameter String param) throws IOException,
				InterruptedException {
			ListBoxModel items = new ListBoxModel();

			scm = getProjectSCM(project);
			if (scm == null) {
				items.add("!No Git repository configured in SCM configuration");
				return items;
			}
			ParametersDefinitionProperty prop = project
					.getProperty(ParametersDefinitionProperty.class);
			if (prop != null) {
				ParameterDefinition def = prop.getParameterDefinition(param);
				if (def instanceof GitParameterDefinition) {
					GitParameterDefinition paramDef = (GitParameterDefinition) def;
					Map<String, String> paramList = paramDef.generateContents(
							project, scm);

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

		public FormValidation doCheckFilter(@QueryParameter String value) {
			return validateFilter(value) != null ? FormValidation.ok() : FormValidation.error("The pattern '" + value + "' is not valid.");
		}
	}

	/**
	 * Take a glob or regex filter string and return a regex, if valid.
	 * @param value the filter to test
	 * @return A valid regex, or null if invalid.
	 */
	private static String validateFilter(String value) {
		if (isNullOrWhitespace(value)) {
			return createRefRegexFromGlob("*");
		}
		try {
			Pattern.compile(value); // Validate we've got a valid regex.
		} catch (PatternSyntaxException e) {
			try {
				value = createRefRegexFromGlob(value);
				Pattern.compile(value);
			} catch (PatternSyntaxException e2) {
				return null;
			}
		}
		return value;
	}

	/* Taken wholesale from org.jenkinsci.plugins.gitclient.JGitAPIImpl. Might make sense to have this in some common component? */
	/* Adapted from http://stackoverflow.com/questions/1247772/is-there-an-equivalent-of-java-util-regex-for-glob-type-patterns */
    private static String createRefRegexFromGlob(String glob)
    {
        StringBuilder out = new StringBuilder();
        if(glob.startsWith("refs/")) {
            out.append("^");
        } else {
            out.append("^.*/");
        }

        for (int i = 0; i < glob.length(); ++i) {
            final char c = glob.charAt(i);
            switch(c) {
            case '*':
                out.append(".*");
                break;
            case '?':
                out.append('.');
                break;
            case '.':
                out.append("\\.");
                break;
            case '\\':
                out.append("\\\\");
                break;
            default:
                out.append(c);
                break;
            }
        }
        out.append('$');
        return out.toString();
    }
}
