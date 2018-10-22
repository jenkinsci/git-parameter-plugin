package net.uaznia.lukanus.hudson.plugins.gitparameter;

import hudson.cli.CLICommand;
import hudson.cli.ConsoleCommand;
import hudson.model.ParameterValue;
import net.sf.json.JSONObject;
import org.junit.Test;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import static net.uaznia.lukanus.hudson.plugins.gitparameter.Constants.DEFAULT_VALUE;
import static net.uaznia.lukanus.hudson.plugins.gitparameter.Constants.NAME;
import static net.uaznia.lukanus.hudson.plugins.gitparameter.Constants.PT_REVISION;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This test don't use jenkis rule
 */
public class BasicTests {
    /**
     * Test of createValue method, of class GitParameterDefinition.
     */
    @Test
    public void testCreateValue_StaplerRequest() {
        GitParameterDefinition instance = new GitParameterDefinition(NAME, PT_REVISION, DEFAULT_VALUE, "description", "branch", ".*", "*", SortMode.NONE, SelectedValue.NONE, null, false);

        StaplerRequest request = mock(StaplerRequest.class);
        ParameterValue result = instance.createValue(request);

        assertEquals(result, new GitParameterValue(NAME, DEFAULT_VALUE));
    }

    @Test
    public void matchesWithBitbucketPullRequestRefs() {
        Matcher matcher = Consts.PULL_REQUEST_REFS_PATTERN.matcher("refs/pull-requests/186/from");
        matcher.find();
        assertEquals(matcher.group(1), "186");
    }

    @Test
    public void matchesWithGithubPullRequestRefs() {
        Matcher matcher = Consts.PULL_REQUEST_REFS_PATTERN.matcher("refs/pull/45/head");
        matcher.find();
        assertEquals(matcher.group(1), "45");
    }

    @Test
    public void testCreateValue_StaplerRequest_ValueInRequest() {
        GitParameterDefinition instance = new GitParameterDefinition(NAME, PT_REVISION, DEFAULT_VALUE, "description", "branch", ".*", "*", SortMode.NONE, SelectedValue.NONE, null, false);

        StaplerRequest request = mock(StaplerRequest.class);
        when(request.getParameterValues(instance.getName())).thenReturn(new String[]{"master"});
        ParameterValue result = instance.createValue(request);

        assertEquals(result, new GitParameterValue(NAME, "master"));
    }

    @Test
    public void testConstructorInitializesTagFilterToAsteriskWhenNull() {
        GitParameterDefinition instance = new GitParameterDefinition(NAME, PT_REVISION, DEFAULT_VALUE, "description", "branch", null, null, SortMode.NONE, SelectedValue.NONE, null, false);
        assertEquals("*", instance.getTagFilter());
        assertEquals(".*", instance.getBranchFilter());
    }

    @Test
    public void testConstructorInitializesTagFilterToAsteriskWhenWhitespace() {
        GitParameterDefinition instance = new GitParameterDefinition(NAME, PT_REVISION, DEFAULT_VALUE, "description", "branch", "  ", "  ", SortMode.NONE, SelectedValue.NONE, null, false);
        assertEquals("*", instance.getTagFilter());
        assertEquals(".*", instance.getBranchFilter());
    }

    @Test
    public void testConstructorInitializesTagFilterToAsteriskWhenEmpty() {
        GitParameterDefinition instance = new GitParameterDefinition(NAME, PT_REVISION, DEFAULT_VALUE, "description", "branch", "", "", SortMode.NONE, SelectedValue.NONE, null, false);
        assertEquals("*", instance.getTagFilter());
        assertEquals(".*", instance.getBranchFilter());
    }

    @Test
    public void testConstructorInitializesTagToGivenValueWhenNotNullOrWhitespace() {
        GitParameterDefinition instance = new GitParameterDefinition(NAME, PT_REVISION, DEFAULT_VALUE, "description", "branch", "foobar", "foobar", SortMode.NONE, SelectedValue.NONE, null, false);
        assertEquals("foobar", instance.getTagFilter());
        assertEquals("foobar", instance.getBranchFilter());
    }

    /**
     * Test of createValue method, of class GitParameterDefinition.
     */
    @Test
    public void testCreateValue_StaplerRequest_JSONObject() {
        System.out.println("createValue");
        StaplerRequest request = mock(StaplerRequest.class);

        Map<String, String> jsonR = new HashMap<String, String>();
        jsonR.put("value", "Git_param_value");
        jsonR.put(NAME, "Git_param_name");

        JSONObject jO = JSONObject.fromObject(jsonR);


        GitParameterDefinition instance = new GitParameterDefinition(NAME, PT_REVISION, DEFAULT_VALUE, "description", "branch", ".*", "*", SortMode.NONE, SelectedValue.NONE, null, false);

        ParameterValue result = instance.createValue(request, jO);

        assertEquals(result, new GitParameterValue("Git_param_name", "Git_param_value"));
    }

    @Test
    public void testCreateValue_CLICommand() throws IOException, InterruptedException {
        CLICommand cliCommand = new ConsoleCommand();
        GitParameterDefinition instance = new GitParameterDefinition(NAME, PT_REVISION, DEFAULT_VALUE, "description", "branch", ".*", "*", SortMode.NONE, SelectedValue.NONE, null, false);

        String value = "test";
        ParameterValue result = instance.createValue(cliCommand, value);
        assertEquals(result, new GitParameterValue(NAME, value));
    }

    @Test
    public void testCreateValue_CLICommand_EmptyValue() throws IOException, InterruptedException {
        CLICommand cliCommand = new ConsoleCommand();
        GitParameterDefinition instance = new GitParameterDefinition(NAME, PT_REVISION, DEFAULT_VALUE, "description", "branch", ".*", "*", SortMode.NONE, SelectedValue.NONE, null, false);

        ParameterValue result = instance.createValue(cliCommand, null);
        assertEquals(result, new GitParameterValue(NAME, DEFAULT_VALUE));
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
        String expResult = PT_REVISION;
        GitParameterDefinition instance = new GitParameterDefinition(NAME, expResult, DEFAULT_VALUE, "description", "branch", ".*", "*", SortMode.NONE, SelectedValue.NONE, null, false);
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
        String expResult = PT_REVISION;
        GitParameterDefinition instance = new GitParameterDefinition(NAME, "asdf", DEFAULT_VALUE, "description", "branch", ".*", "*", SortMode.NONE, SelectedValue.NONE, null, false);

        instance.setType(expResult);
        String result = instance.getType();
        assertEquals(expResult, result);
    }

    @Test
    public void testWrongType() {
        GitParameterDefinition instance = new GitParameterDefinition(NAME, "asdf", DEFAULT_VALUE, "description", "branch", ".*", "*", SortMode.NONE, SelectedValue.NONE, null, false);

        String result = instance.getType();
        assertEquals("PT_BRANCH", result);
    }

    /**
     * Test of getDefaultValue method, of class GitParameterDefinition.
     */
    @Test
    public void testGetDefaultValue() {
        System.out.println("getDefaultValue");
        String expResult = DEFAULT_VALUE;

        GitParameterDefinition instance = new GitParameterDefinition(NAME, "asdf", expResult, "description", "branch", ".*", "*", SortMode.NONE, SelectedValue.NONE, null, false);
        String result = instance.getDefaultValue();
        assertEquals(expResult, result);
    }

    /**
     * Test of setDefaultValue method, of class GitParameterDefinition.
     */
    @Test
    public void testSetDefaultValue() {
        System.out.println("getDefaultValue");
        String expResult = DEFAULT_VALUE;

        GitParameterDefinition instance = new GitParameterDefinition(NAME, "asdf", "other", "description", "branch", ".*", "*", SortMode.NONE, SelectedValue.NONE, null, false);
        instance.setDefaultValue(expResult);

        String result = instance.getDefaultValue();
        assertEquals(expResult, result);
    }

    /**
     * Test of generateContents method, of class GitParameterDefinition.
     */
    @Test
    public void testGenerateContents() {
    }
}
