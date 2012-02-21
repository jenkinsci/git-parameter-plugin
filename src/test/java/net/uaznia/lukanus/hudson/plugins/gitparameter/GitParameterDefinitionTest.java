/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.uaznia.lukanus.hudson.plugins.gitparameter;

import hudson.model.ParameterValue;
import org.jvnet.hudson.test.HudsonTestCase;
import org.apache.commons.io.FileUtils;
import hudson.model.*;
import hudson.tasks.Shell;

import static org.mockito.Mockito.*;


import java.util.Map;
import java.util.HashMap;
import net.sf.json.JSONObject;
import org.junit.*;
import static org.junit.Assert.*;
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
              
        GitParameterDefinition instance = new GitParameterDefinition("name","PT_REVISION","defaultValue","description","branch");
       
        StaplerRequest request = mock(StaplerRequest.class);
        ParameterValue result = instance.createValue(request);
        
        assertEquals(result, new GitParameterValue("name", "defaultValue"));
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
        
        
        GitParameterDefinition instance = new GitParameterDefinition("name","PT_REVISION","defaultValue","description","branch");
       
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
        GitParameterDefinition instance = new GitParameterDefinition("name",expResult,"defaultValue","description","branch");
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
        GitParameterDefinition instance = new GitParameterDefinition("name","asdf","defaultValue","description","branch");
        
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
        
        GitParameterDefinition instance = new GitParameterDefinition("name","asdf", expResult,"description","branch");       
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
        
        GitParameterDefinition instance = new GitParameterDefinition("name","asdf", "other" ,"description","branch");       
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
