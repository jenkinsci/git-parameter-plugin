/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.uaznia.lukanus.hudson.plugins.gitparameter;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import hudson.model.FreeStyleBuild;
import hudson.model.ParameterValue;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersDefinitionProperty;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.UserRemoteConfig;
import hudson.tasks.Shell;
import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;
import hudson.util.ListBoxModel;

import net.sf.json.JSONObject;
import net.uaznia.lukanus.hudson.plugins.gitparameter.GitParameterDefinition.DescriptorImpl;
import net.uaznia.lukanus.hudson.plugins.gitparameter.GitParameterDefinition.SortMode;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.MockFolder;
import org.kohsuke.stapler.StaplerRequest;

/**
 *
 * @author lukanus
 */
public class GitParameterDefinitionTest extends HudsonTestCase  {
    private final String repositoryUrl = "https://github.com/jenkinsci/git-parameter-plugin.git";
    private FreeStyleProject project;
    
    public GitParameterDefinitionTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
        
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of createValue method, of class GitParameterDefinition.
     */
    @Test
    public void testCreateValue_StaplerRequest() {
        System.out.println("createValue");
              
        GitParameterDefinition instance = new GitParameterDefinition("name","PT_REVISION","defaultValue","description","branch","*","*",GitParameterDefinition.SortMode.NONE);
        
        StaplerRequest request = mock(StaplerRequest.class);
        ParameterValue result = instance.createValue(request);
        
        assertEquals(result, new GitParameterValue("name", "defaultValue"));
    }

    @Test
    public void testConstructorInitializesTagFilterToAsteriskWhenNull() {
    	GitParameterDefinition instance = new GitParameterDefinition("name","PT_REVISION","defaultValue","description","branch",null,null,GitParameterDefinition.SortMode.NONE);
    	assertEquals("*", instance.getTagFilter());
    	assertEquals("*", instance.getBranchfilter());
    }
    
    @Test
    public void testConstructorInitializesTagFilterToAsteriskWhenWhitespace() {
    	GitParameterDefinition instance = new GitParameterDefinition("name","PT_REVISION","defaultValue","description","branch","  ","  ",GitParameterDefinition.SortMode.NONE);
    	assertEquals("*", instance.getTagFilter());
    	assertEquals("*", instance.getBranchfilter());
    }
    
    @Test
    public void testConstructorInitializesTagFilterToAsteriskWhenEmpty() {
    	GitParameterDefinition instance = new GitParameterDefinition("name","PT_REVISION","defaultValue","description","branch","","",GitParameterDefinition.SortMode.NONE);
    	assertEquals("*", instance.getTagFilter());
    	assertEquals("*", instance.getBranchfilter());
    }
    
    @Test
    public void testConstructorInitializesTagToGivenValueWhenNotNullOrWhitespace() {
    	GitParameterDefinition instance = new GitParameterDefinition("name","PT_REVISION","defaultValue","description","branch","foobar","foobar",GitParameterDefinition.SortMode.NONE);
    	assertEquals("foobar", instance.getTagFilter());
    	assertEquals("foobar", instance.getBranchfilter());
    }
   
    
    @Test
    public void testSmartNumberStringComparerWorksWithSameNumberComponents() {
    	Comparator<String> comparer = new GitParameterDefinition.SmartNumberStringComparer();
    	assertTrue(comparer.compare("v_1.1.0.2", "v_1.1.1.1") < 0);
    	assertTrue(comparer.compare("v_1.1.1.1", "v_1.1.1.1") == 0);
    	assertTrue(comparer.compare("v_1.1.1.1", "v_2.0.0.0") < 0);
    	assertTrue(comparer.compare("v_1.1.1.1", "v_1.1.1.0") > 0);
    }
    
    @Test
    public void testSmartNumberStringComparerWorksWithDifferentNumberComponents() {
    	Comparator<String> comparer = new GitParameterDefinition.SmartNumberStringComparer();
    	assertTrue(comparer.compare("v_1.1.1.1", "v_1.1.0") > 0);
    	assertTrue(comparer.compare("v_1.1.1.1", "v_1.1.2") < 0);
    	assertTrue(comparer.compare("v_1", "v_2.0.0.0") < 0);
    }
    
    @Test
    public void testSortTagsYieldsCorrectOrderWithSmartSortEnabled() {
    	GitParameterDefinition instance = new GitParameterDefinition("name","PT_REVISION","defaultValue","description","branch",null,null,GitParameterDefinition.SortMode.ASCENDING_SMART);
    	Set<String> tags = new HashSet<String>();
    	tags.add("v_1.0.0.2");
    	tags.add("v_1.0.0.5");
    	tags.add("v_1.0.1.1");
    	tags.add("v_1.0.0.0");
    	tags.add("v_1.0.0.10");
    	
    	ArrayList<String> orderedTags = instance.sortByName(tags);
    	
    	assertEquals("v_1.0.0.0", orderedTags.get(0));
    	assertEquals("v_1.0.0.2", orderedTags.get(1));
    	assertEquals("v_1.0.0.5", orderedTags.get(2));
    	assertEquals("v_1.0.0.10", orderedTags.get(3));
    	assertEquals("v_1.0.1.1", orderedTags.get(4));
    }
    
    @Test
    public void testSortTagsYieldsCorrectOrderWithSmartSortDisabled() {
    	GitParameterDefinition instance = new GitParameterDefinition("name","PT_REVISION","defaultValue","description","branch",null,null,GitParameterDefinition.SortMode.ASCENDING);
    	Set<String> tags = new HashSet<String>();
    	tags.add("v_1.0.0.2");
    	tags.add("v_1.0.0.5");
    	tags.add("v_1.0.1.1");
    	tags.add("v_1.0.0.0");
    	tags.add("v_1.0.0.10");
    	
    	ArrayList<String> orderedTags = instance.sortByName(tags);
    	
    	assertEquals("v_1.0.0.0", orderedTags.get(0));
    	assertEquals("v_1.0.0.10", orderedTags.get(1));
    	assertEquals("v_1.0.0.2", orderedTags.get(2));
    	assertEquals("v_1.0.0.5", orderedTags.get(3));
    	assertEquals("v_1.0.1.1", orderedTags.get(4));
    }
    
    @Test
    public void testSortMode_getIsUsingSmartSort() {
    	assertFalse(SortMode.NONE.getIsUsingSmartSort());
    	assertFalse(SortMode.ASCENDING.getIsUsingSmartSort());
    	assertTrue(SortMode.ASCENDING_SMART.getIsUsingSmartSort());
    	assertFalse(SortMode.DESCENDING.getIsUsingSmartSort());
    	assertTrue(SortMode.DESCENDING_SMART.getIsUsingSmartSort());
    }
    
    @Test
    public void testSortMode_getIsDescending() {
    	assertFalse(SortMode.NONE.getIsDescending());
    	assertFalse(SortMode.ASCENDING.getIsDescending());
    	assertFalse(SortMode.ASCENDING_SMART.getIsDescending());
    	assertTrue(SortMode.DESCENDING.getIsDescending());
    	assertTrue(SortMode.DESCENDING_SMART.getIsDescending());
    }
    
    @Test
    public void testSortMode_getIsSorting() {
    	assertFalse(SortMode.NONE.getIsSorting());
    	assertTrue(SortMode.ASCENDING.getIsSorting());
    	assertTrue(SortMode.ASCENDING_SMART.getIsSorting());
    	assertTrue(SortMode.DESCENDING.getIsSorting());
    	assertTrue(SortMode.DESCENDING_SMART.getIsSorting());
    }
    
    /**
     * Test of createValue method, of class GitParameterDefinition.
     */
    @Test
    public void testCreateValue_StaplerRequest_JSONObject() {
        System.out.println("createValue");
        StaplerRequest request = mock(StaplerRequest.class);
        
        Map<String,String> jsonR = new HashMap<String,String>();
        jsonR.put("value", "Git_param_value");
        jsonR.put("name", "Git_param_name");
        
        JSONObject jO = JSONObject.fromObject(jsonR);
        
        
        GitParameterDefinition instance = new GitParameterDefinition("name","PT_REVISION","defaultValue","description","branch","*","*",GitParameterDefinition.SortMode.NONE);
       
        ParameterValue result = instance.createValue(request,jO);
        
        assertEquals(result, new GitParameterValue("Git_param_name", "Git_param_value"));
        
        
    }

    /**
     * Test of getDefaultParameterValue method, of class GitParameterDefinition.
     */
    @Test
    public void testGetDefaultParameterValue() {
       
        // TODO review the generated test code and remove the default call to fail.
        //fail("The test case is a prototype.");
    }

    /**
     * Test of getType method, of class GitParameterDefinition.
     */
    @Test 
    public void testGetType() {
        System.out.println("Test of getType method.");
        String expResult = "PT_REVISION";        
        GitParameterDefinition instance = new GitParameterDefinition("name",expResult,"defaultValue","description","branch","*","*",GitParameterDefinition.SortMode.NONE);
        String result = instance.getType();
        assertEquals(expResult, result);
        
        
        instance.setType(expResult);  
        result = instance.getType();              
        assertEquals(expResult, result);
        
    }

    /**
     * Test of setType method, of class GitParameterDefinition.
     */
    @Test
    public void testSetType() {
        System.out.println("Test of setType method.");
        String expResult = "PT_REVISION";        
        GitParameterDefinition instance = new GitParameterDefinition("name","asdf","defaultValue","description","branch","*","*",GitParameterDefinition.SortMode.NONE);
        
        instance.setType(expResult);        
        String result = instance.getType();        
        assertEquals(expResult, result);
    }

    /**
     * Test of getDefaultValue method, of class GitParameterDefinition.
     */
    @Test
    public void testGetDefaultValue() {
        System.out.println("getDefaultValue");
        String expResult = "defaultValue";
        
        GitParameterDefinition instance = new GitParameterDefinition("name","asdf", expResult,"description","branch","*","*",GitParameterDefinition.SortMode.NONE);       
        String result = instance.getDefaultValue();
        assertEquals(expResult, result);
    }

    /**
     * Test of setDefaultValue method, of class GitParameterDefinition.
     */
    @Test
    public void testSetDefaultValue() {
        System.out.println("getDefaultValue");
        String expResult = "defaultValue";
        
        GitParameterDefinition instance = new GitParameterDefinition("name","asdf", "other" ,"description","branch","*","*",GitParameterDefinition.SortMode.NONE);       
        instance.setDefaultValue(expResult);
        
        String result = instance.getDefaultValue();
        assertEquals(expResult, result);
        
    }

    /**
     * Test of generateContents method, of class GitParameterDefinition.
     */
    @Test
    public void testGenerateContents() {
        
//        System.out.println("generateContents");
  //      String contenttype = "";
    //    GitParameterDefinition instance = null;
      //  instance.generateContents(contenttype);


    }

    /**
     * Test of getErrorMessage method, of class GitParameterDefinition.
     */
    @Test
    public void testGetErrorMessage() {
        System.out.println("getErrorMessage");
    /*    GitParameterDefinition instance = null;
        String expResult = "";
        String result = instance.getErrorMessage();
        assertEquals(expResult, result);*/
        // TODO review the generated test code and remove the default call to fail.
        
    }

    // Test Descriptor.getProjectSCM()
    @Test
    public void testGetProjectSCM() throws Exception {
        super.setUp();
        FreeStyleProject testJob = jenkins.createProject(FreeStyleProject.class, "testGetProjectSCM");
        GitSCM git = new GitSCM(repositoryUrl);
        GitParameterDefinition def = new GitParameterDefinition("testName",
                "PT_REVISION",
                "testDefaultValue",
                "testDescription",
                "testBranch",
                "*",
                "*",
                SortMode.NONE);
        testJob.addProperty(new ParametersDefinitionProperty(def));
        assertTrue(def.getDescriptor().getProjectSCM(testJob) == null);

        testJob.setScm(git);
        assertTrue(git.equals(def.getDescriptor().getProjectSCM(testJob)));
        super.tearDown();
    }

    // Test Descriptor.doFillValueItems() with no SCM configured
    @Test
    public void testDoFillValueItems_withoutSCM() throws Exception {
        super.setUp();
        FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "testListTags");
        GitParameterDefinition def = new GitParameterDefinition("testName",
                "PT_TAG",
                "testDefaultValue",
                "testDescription",
                "testBranch",
                "*",
                "*",
                SortMode.NONE);
        project.addProperty(new ParametersDefinitionProperty(def));

        ListBoxModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "No Git repository configured in SCM configuration"));

        super.tearDown();
    }

    // Test Descriptor.doFillValueItems() with listing tags
    @Test
    public void testDoFillValueItems_listTags() throws Exception {
        super.setUp();
        project = jenkins.createProject(FreeStyleProject.class, "testListTags");
        project.getBuildersList().add(new Shell("echo test"));
        setupGit();

        GitParameterDefinition def = new GitParameterDefinition("testName",
                "PT_TAG",
                "testDefaultValue",
                "testDescription",
                "testBranch",
                "*",
                "*",
                SortMode.NONE);
        project.addProperty(new ParametersDefinitionProperty(def));

        // Run the build once to get the workspace
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        ListBoxModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "git-parameter-0.2"));

        super.tearDown();
    }
    
    // Test Descriptor.doFillValueItmes() with listing branches
    @Test
    public void testDoFillValueItems_listBranches() throws Exception {
        super.setUp();
        project = jenkins.createProject(FreeStyleProject.class, "testListTags");
        project.getBuildersList().add(new Shell("echo test"));
        setupGit();

        GitParameterDefinition def = new GitParameterDefinition("testName",
                "PT_BRANCH",
                "testDefaultValue",
                "testDescription",
                "testBranch",
                "*",
                "*",
                SortMode.NONE);
        project.addProperty(new ParametersDefinitionProperty(def));

        // Run the build once to get the workspace
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        ListBoxModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "master"));

        super.tearDown();
    }
    
    // Test Descriptor.doFillValueItmes() with listing tags and branches
    @Test
    public void testDoFillValueItems_listTagsAndBranches() throws Exception {
        super.setUp();
        project = jenkins.createProject(FreeStyleProject.class, "testListTags");
        project.getBuildersList().add(new Shell("echo test"));
        setupGit();

        GitParameterDefinition def = new GitParameterDefinition("testName",
                "PT_BRANCH_TAG",
                "testDefaultValue",
                "testDescription",
                "testBranch",
                "*",
                "*",
                SortMode.NONE);
        project.addProperty(new ParametersDefinitionProperty(def));

        // Run the build once to get the workspace
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        ListBoxModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "master"));
        assertTrue(isListBoxItem(items, "git-parameter-0.2"));

        super.tearDown();
    }

    // Test Descriptor.doFillValueItems() with listing revisions
    @Test
    public void testDoFillValueItems_listRevisions() throws Exception {
        super.setUp();
        project = jenkins.createProject(FreeStyleProject.class, "testListRevisions");
        project.getBuildersList().add(new Shell("echo test"));
        setupGit();

        GitParameterDefinition def = new GitParameterDefinition("testName",
                "PT_REVISION",
                "testDefaultValue",
                "testDescription",
                null,
                "*",
                "*",
                SortMode.NONE);

        project.addProperty(new ParametersDefinitionProperty(def));

        // Run the build once to get the workspace
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        ListBoxModel items = def.getDescriptor().doFillValueItems(project, def.getName());
        assertTrue(isListBoxItem(items, "00a8385c"));

        super.tearDown();
    }

    @Test
    public void testSearchInFolders() throws Exception {
        super.setUp();
        MockFolder folder = jenkins.createProject(MockFolder.class, "folder");
        FreeStyleProject job1 = folder.createProject(FreeStyleProject.class, "job1");

        GitParameterDefinition gitParameterDefinition = new GitParameterDefinition("name",
                "asdf",
                "other",
                "description",
                "branch",
                "*",
                "*",
                SortMode.NONE);
        job1.addProperty(new ParametersDefinitionProperty(gitParameterDefinition));
        assertEquals("folder/job1", gitParameterDefinition.getParentProject().getFullName());
        super.tearDown();
    }
    
    @Test
    public void testBranchFilterValidation() {
    	final DescriptorImpl descriptor = new DescriptorImpl();
    	final FormValidation okPsuedoWildcard = descriptor.doCheckBranchfilter("*");
    	final FormValidation okWildcard = descriptor.doCheckBranchfilter("*");
    	final FormValidation badWildcard = descriptor.doCheckBranchfilter(".**");
    	
    	assertTrue(okPsuedoWildcard.kind == Kind.OK);
    	assertTrue(okWildcard.kind == Kind.OK);
    	assertTrue(badWildcard.kind == Kind.ERROR);
    }
    

    private boolean isListBoxItem(ListBoxModel items, String item) {
        boolean itemExists = false;
        for (int i=0; i<items.size(); i++) {
            if (items.get(i).value.contains(item)) {
                itemExists = true;
            }
        }
        return itemExists;
    }

    private void setupGit() throws IOException {
        UserRemoteConfig config = new UserRemoteConfig(repositoryUrl, null, null, null);
        List<UserRemoteConfig> configs = new ArrayList<UserRemoteConfig>();
        configs.add(config);
        GitSCM git = new GitSCM(configs, null, false, null, null, null, null);
        project.setScm(git);
    }
}
