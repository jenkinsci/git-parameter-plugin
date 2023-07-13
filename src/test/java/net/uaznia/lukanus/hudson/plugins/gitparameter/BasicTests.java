package net.uaznia.lukanus.hudson.plugins.gitparameter;

import static net.uaznia.lukanus.hudson.plugins.gitparameter.Constants.DEFAULT_VALUE;
import static net.uaznia.lukanus.hudson.plugins.gitparameter.Constants.NAME;
import static net.uaznia.lukanus.hudson.plugins.gitparameter.Constants.PT_REVISION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hudson.model.ParameterValue;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import net.sf.json.JSONObject;
import org.junit.Test;
import org.kohsuke.stapler.StaplerRequest;

/**
 * This test don't use jenkis rule
 */
public class BasicTests {
    /**
     * Test of createValue method, of class GitParameterDefinition.
     */
    @Test
    public void testCreateValue_StaplerRequest() {
        GitParameterDefinition instance = new GitParameterDefinition(
                NAME,
                PT_REVISION,
                DEFAULT_VALUE,
                "description",
                "branch",
                ".*",
                "*",
                SortMode.NONE,
                SelectedValue.NONE,
                null,
                false);

        StaplerRequest request = mock(StaplerRequest.class);
        ParameterValue result = instance.createValue(request);

        assertEquals(result, new GitParameterValue(NAME, DEFAULT_VALUE));
    }

    @Test
    public void matchesWithBitbucketPullRequestRefs() {
        Matcher matcher = Consts.PULL_REQUEST_REFS_PATTERN.matcher("refs/pull-requests/186/from");
        assertTrue(matcher.find());
        assertEquals(matcher.group(1), "186");
    }

    @Test
    public void matchesWithGithubPullRequestRefs() {
        Matcher matcher = Consts.PULL_REQUEST_REFS_PATTERN.matcher("refs/pull/45/head");
        assertTrue(matcher.find());
        assertEquals(matcher.group(1), "45");
    }

    @Test
    public void testCreateValue_StaplerRequest_ValueInRequest() {
        GitParameterDefinition instance = new GitParameterDefinition(
                NAME,
                PT_REVISION,
                DEFAULT_VALUE,
                "description",
                "branch",
                ".*",
                "*",
                SortMode.NONE,
                SelectedValue.NONE,
                null,
                false);

        StaplerRequest request = mock(StaplerRequest.class);
        when(request.getParameterValues(instance.getName())).thenReturn(new String[] {"master"});
        ParameterValue result = instance.createValue(request);

        assertEquals(result, new GitParameterValue(NAME, "master"));
    }

    @Test
    public void testConstructorInitializesTagFilterToAsteriskWhenNull() {
        GitParameterDefinition instance = new GitParameterDefinition(
                NAME,
                PT_REVISION,
                DEFAULT_VALUE,
                "description",
                "branch",
                null,
                null,
                SortMode.NONE,
                SelectedValue.NONE,
                null,
                false);
        assertEquals("*", instance.getTagFilter());
        assertEquals(".*", instance.getBranchFilter());
    }

    @Test
    public void testConstructorInitializesTagFilterToAsteriskWhenWhitespace() {
        GitParameterDefinition instance = new GitParameterDefinition(
                NAME,
                PT_REVISION,
                DEFAULT_VALUE,
                "description",
                "branch",
                "  ",
                "  ",
                SortMode.NONE,
                SelectedValue.NONE,
                null,
                false);
        assertEquals("*", instance.getTagFilter());
        assertEquals(".*", instance.getBranchFilter());
    }

    @Test
    public void testConstructorInitializesTagFilterToAsteriskWhenEmpty() {
        GitParameterDefinition instance = new GitParameterDefinition(
                NAME,
                PT_REVISION,
                DEFAULT_VALUE,
                "description",
                "branch",
                "",
                "",
                SortMode.NONE,
                SelectedValue.NONE,
                null,
                false);
        assertEquals("*", instance.getTagFilter());
        assertEquals(".*", instance.getBranchFilter());
    }

    @Test
    public void testConstructorInitializesTagToGivenValueWhenNotNullOrWhitespace() {
        GitParameterDefinition instance = new GitParameterDefinition(
                NAME,
                PT_REVISION,
                DEFAULT_VALUE,
                "description",
                "branch",
                "foobar",
                "foobar",
                SortMode.NONE,
                SelectedValue.NONE,
                null,
                false);
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

        Map<String, String> jsonR = new HashMap<>();
        jsonR.put("value", "Git_param_value");
        jsonR.put(NAME, "Git_param_name");

        JSONObject jO = JSONObject.fromObject(jsonR);

        GitParameterDefinition instance = new GitParameterDefinition(
                NAME,
                PT_REVISION,
                DEFAULT_VALUE,
                "description",
                "branch",
                ".*",
                "*",
                SortMode.NONE,
                SelectedValue.NONE,
                null,
                false);

        ParameterValue result = instance.createValue(request, jO);

        assertEquals(result, new GitParameterValue("Git_param_name", "Git_param_value"));
    }

    /**
     * Test of getDefaultParameterValue method, of class GitParameterDefinition.
     */
    @Test
    public void testGetDefaultParameterValue() {

        // TODO review the generated test code and remove the default call to fail.
        // fail("The test case is a prototype.");
    }

    /**
     * Test of getType method, of class GitParameterDefinition.
     */
    @Test
    public void testGetParameterType() {
        String expResult = PT_REVISION;
        GitParameterDefinition instance = new GitParameterDefinition(
                NAME,
                expResult,
                DEFAULT_VALUE,
                "description",
                "branch",
                ".*",
                "*",
                SortMode.NONE,
                SelectedValue.NONE,
                null,
                false);
        String result = instance.getParameterType();
        assertEquals(expResult, result);

        instance.setParameterType(expResult);
        result = instance.getParameterType();
        assertEquals(expResult, result);
    }

    /**
     * Test of setType method, of class GitParameterDefinition.
     */
    @Test
    public void testSetParameterType() {
        String expResult = PT_REVISION;
        GitParameterDefinition instance = new GitParameterDefinition(
                NAME,
                "asdf",
                DEFAULT_VALUE,
                "description",
                "branch",
                ".*",
                "*",
                SortMode.NONE,
                SelectedValue.NONE,
                null,
                false);

        instance.setParameterType(expResult);
        String result = instance.getParameterType();
        assertEquals(expResult, result);
    }

    @Test
    public void testWrongType() {
        GitParameterDefinition instance = new GitParameterDefinition(
                NAME,
                "asdf",
                DEFAULT_VALUE,
                "description",
                "branch",
                ".*",
                "*",
                SortMode.NONE,
                SelectedValue.NONE,
                null,
                false);

        String result = instance.getParameterType();
        assertEquals("PT_BRANCH", result);
    }

    /**
     * Test of getDefaultValue method, of class GitParameterDefinition.
     */
    @Test
    public void testGetDefaultValue() {
        System.out.println("getDefaultValue");
        String expResult = DEFAULT_VALUE;

        GitParameterDefinition instance = new GitParameterDefinition(
                NAME,
                "asdf",
                expResult,
                "description",
                "branch",
                ".*",
                "*",
                SortMode.NONE,
                SelectedValue.NONE,
                null,
                false);
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

        GitParameterDefinition instance = new GitParameterDefinition(
                NAME,
                "asdf",
                "other",
                "description",
                "branch",
                ".*",
                "*",
                SortMode.NONE,
                SelectedValue.NONE,
                null,
                false);
        instance.setDefaultValue(expResult);

        String result = instance.getDefaultValue();
        assertEquals(expResult, result);
    }

    /**
     * Test of generateContents method, of class GitParameterDefinition.
     */
    @Test
    public void testGenerateContents() {}
}
