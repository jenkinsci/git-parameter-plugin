package net.uaznia.lukanus.hudson.plugins.gitparameter;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Date;

import java.text.SimpleDateFormat;



import hudson.EnvVars;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.ParameterValue;
import hudson.model.ParameterDefinition;
import hudson.model.Hudson;
import hudson.model.TaskListener;
import hudson.scm.SCM;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;



import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.lib.ObjectId;

import hudson.plugins.git.GitSCM;
import hudson.plugins.git.IGitAPI;
import hudson.plugins.git.GitAPI;
import hudson.plugins.git.Revision;


public class GitParameterDefinition extends ParameterDefinition  {
	private static final long serialVersionUID = 9157832967140868127L;

	public static final String PARAMETER_TYPE_TAG = "PT_TAG";

	public static final String PARAMETER_TYPE_REVISION = "PT_REVISION";

	@Extension
	public static class DescriptorImpl extends ParameterDescriptor {
		@Override
		public String getDisplayName() {
			return "Git Parameter";
		}
	}

              
	private String type;
        private String errorMessage;
        
	private String defaultValue;        
        
        private Map<String, String> revisionMap;
        private Map<String, String> tagMap;

	@DataBoundConstructor
	public GitParameterDefinition(String name,
        String type, String defaultValue,
        String description
        ) {
		super(name, description);
		this.type = type;
		this.defaultValue = defaultValue;
                              
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
			for(int i = 0; i < jsonValues.size(); i++) {
				strValue += jsonValues.getString(i);
				if (i < jsonValues.size() - 1) {
					strValue += ",";
				}
			}
		}
                
                if("".equals(strValue)) {
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
		this.type = type;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public void generateContents(String contenttype) {
            
	        for (AbstractProject<?,?> project : Hudson.getInstance().getItems(AbstractProject.class)) {
                    if (project.getSomeWorkspace() == null) {
                        errorMessage = "noWorkspace";
                        break;
                    }                    
                    
                    SCM scm = project.getScm();
                    
                    if (scm instanceof GitSCM); else continue;
                    
                    GitSCM git = (GitSCM) scm;
                    
                    String defaultGitExe = File.separatorChar != '/' ? "git.exe" : "git";
                    GitSCM.DescriptorImpl desc = (GitSCM.DescriptorImpl) git.getDescriptor();
                    if (desc.getOldGitExe() != null) {
                        defaultGitExe = desc.getOldGitExe();
                    }
                    
                    EnvVars environment = null;
                    
                    try {
                         environment = project.getSomeBuildWithWorkspace().getEnvironment(TaskListener.NULL);
                    } catch(Exception e) {}
                    
                    for (RemoteConfig repository : git.getRepositories()) {
                        for (URIish remoteURL : repository.getURIs()) {
                            
                        IGitAPI newgit = new GitAPI(defaultGitExe, project.getSomeWorkspace(), TaskListener.NULL, environment);
                        newgit.fetch();
                        
                        if(type.equalsIgnoreCase(PARAMETER_TYPE_REVISION)) {
                            revisionMap = new HashMap<String, String>();
                            
                            List<ObjectId> oid = newgit.revListAll();                        
                                
                            for(ObjectId noid: oid) {
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

                                 
                                } catch (Exception e) {
                                    e.toString();
                                }
                                revisionMap.put(r.getSha1String(), r.getSha1String() + " " + author + " " + goodDate);
                            }
                        } else if(type.equalsIgnoreCase(PARAMETER_TYPE_TAG)) {         
                            tagMap = new HashMap<String, String>();
                             
                            //Set<String> tagNameList = newgit.getTagNames("*");
                            for(String tagName: newgit.getTagNames("*")) {
                                tagMap.put(tagName, tagName);
                            }
                        }                                
                            
                        }
                    }
                 }
                
	}
        
	public String getErrorMessage() {
            return errorMessage;
        }
        
	public Map<String, String> getRevisionMap() {
            if( revisionMap == null || revisionMap.isEmpty()){
                generateContents(PARAMETER_TYPE_REVISION);
            }
            return revisionMap;
        }
        
        public Map<String, String> getTagMap() {
            if( tagMap == null || tagMap.isEmpty()){
                generateContents(PARAMETER_TYPE_TAG);
            }
            return tagMap;
        }
        

}
