/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.uaznia.lukanus.hudson.plugins.gitparameter;

import static org.mockito.Mockito.mock;
import hudson.model.ParameterValue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.sf.json.JSONObject;
import net.uaznia.lukanus.hudson.plugins.gitparameter.GitParameterDefinition.SortMode;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;
import org.kohsuke.stapler.StaplerRequest;

/**
 *
 * @author lukanus
 */
public class GitParameterDefinitionTest extends HudsonTestCase  {
    
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
              
        GitParameterDefinition instance = new GitParameterDefinition("name","PT_REVISION","defaultValue","description","branch", "*",GitParameterDefinition.SortMode.NONE);
        
        StaplerRequest request = mock(StaplerRequest.class);
        ParameterValue result = instance.createValue(request);
        
        assertEquals(result, new GitParameterValue("name", "defaultValue"));
    }

    @Test
    public void testConstructorInitializesTagFilterToAsteriskWhenNull() {
    	GitParameterDefinition instance = new GitParameterDefinition("name","PT_REVISION","defaultValue","description","branch",null,GitParameterDefinition.SortMode.NONE);
    	assertEquals("*", instance.getTagFilter());
    }
    
    @Test
    public void testConstructorInitializesTagFilterToAsteriskWhenWhitespace() {
    	GitParameterDefinition instance = new GitParameterDefinition("name","PT_REVISION","defaultValue","description","branch","  ",GitParameterDefinition.SortMode.NONE);
    	assertEquals("*", instance.getTagFilter());
    }
    
    @Test
    public void testConstructorInitializesTagFilterToAsteriskWhenEmpty() {
    	GitParameterDefinition instance = new GitParameterDefinition("name","PT_REVISION","defaultValue","description","branch","",GitParameterDefinition.SortMode.NONE);
    	assertEquals("*", instance.getTagFilter());
    }
    
    @Test
    public void testConstructorInitializesTagToGivenValueWhenNotNullOrWhitespace() {
    	GitParameterDefinition instance = new GitParameterDefinition("name","PT_REVISION","defaultValue","description","branch","foobar",GitParameterDefinition.SortMode.NONE);
    	assertEquals("foobar", instance.getTagFilter());
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
    	GitParameterDefinition instance = new GitParameterDefinition("name","PT_REVISION","defaultValue","description","branch",null,GitParameterDefinition.SortMode.ASCENDING_SMART);
    	Set<String> tags = new HashSet<String>();
    	tags.add("v_1.0.0.2");
    	tags.add("v_1.0.0.5");
    	tags.add("v_1.0.1.1");
    	tags.add("v_1.0.0.0");
    	tags.add("v_1.0.0.10");
    	
    	ArrayList<String> orderedTags = instance.sortTagNames(tags);
    	
    	assertEquals("v_1.0.0.0", orderedTags.get(0));
    	assertEquals("v_1.0.0.2", orderedTags.get(1));
    	assertEquals("v_1.0.0.5", orderedTags.get(2));
    	assertEquals("v_1.0.0.10", orderedTags.get(3));
    	assertEquals("v_1.0.1.1", orderedTags.get(4));
    }
    
    @Test
    public void testSortTagsYieldsCorrectOrderWithSmartSortDisabled() {
    	GitParameterDefinition instance = new GitParameterDefinition("name","PT_REVISION","defaultValue","description","branch",null,GitParameterDefinition.SortMode.ASCENDING);
    	Set<String> tags = new HashSet<String>();
    	tags.add("v_1.0.0.2");
    	tags.add("v_1.0.0.5");
    	tags.add("v_1.0.1.1");
    	tags.add("v_1.0.0.0");
    	tags.add("v_1.0.0.10");
    	
    	ArrayList<String> orderedTags = instance.sortTagNames(tags);
    	
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
        
        
        GitParameterDefinition instance = new GitParameterDefinition("name","PT_REVISION","defaultValue","description","branch","*",GitParameterDefinition.SortMode.NONE);
       
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
        GitParameterDefinition instance = new GitParameterDefinition("name",expResult,"defaultValue","description","branch","*",GitParameterDefinition.SortMode.NONE);
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
        GitParameterDefinition instance = new GitParameterDefinition("name","asdf","defaultValue","description","branch","*",GitParameterDefinition.SortMode.NONE);
        
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
        
        GitParameterDefinition instance = new GitParameterDefinition("name","asdf", expResult,"description","branch","*",GitParameterDefinition.SortMode.NONE);       
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
        
        GitParameterDefinition instance = new GitParameterDefinition("name","asdf", "other" ,"description","branch","*",GitParameterDefinition.SortMode.NONE);       
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

    /**
     * Test of getRevisionMap method, of class GitParameterDefinition.
     */
    @Test
    public void testGetRevisionMap() {
   //     System.out.println("getRevisionMap");
   //     GitParameterDefinition instance = null;
   //     Map expResult = null;
   //     Map result = instance.getRevisionMap();
   //     assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        
    }

    /**
     * Test of getTagMap method, of class GitParameterDefinition.
     */
    @Test
    public void testGetTagMap() {
        System.out.println("Test of getTagMap method");
   /*      GitParameterDefinition instance = new GitParameterDefinition("name","PT_TAG","defaultValue","description","branch");
       
        Map<String,String> result = instance.getTagMap();
        //assertEquals(expResult, result);
        if(!result.isEmpty()) {
            fail();
        }*/
    }
}
