package net.uaznia.lukanus.hudson.plugins.gitparameter;

import static net.uaznia.lukanus.hudson.plugins.gitparameter.Constants.DEFAULT_VALUE;
import static net.uaznia.lukanus.hudson.plugins.gitparameter.Constants.NAME;
import static net.uaznia.lukanus.hudson.plugins.gitparameter.Constants.PT_REVISION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hudson.model.Failure;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import net.sf.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.kohsuke.stapler.StaplerRequest2;

/**
 * This test don't use jenkins rule
 */
class BasicSafeTests {
    /**
     * Test of createValue method, of class GitParameterDefinition.
     */
    @Test
    void testCreateValue_StaplerRequest2() {
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

        StaplerRequest2 request = mock(StaplerRequest2.class);
        /* Invalid default value is intentionally allowed because
         * other parameter types do not defend themselves against
         * invalid parameter types
         */
        assertEquals(DEFAULT_VALUE, instance.createValue(request).getValue());
    }

    @Test
    void matchesWithBitbucketPullRequestRefs() {
        Matcher matcher = Consts.PULL_REQUEST_REFS_PATTERN.matcher("refs/pull-requests/186/from");
        assertTrue(matcher.find());
        assertEquals("186", matcher.group(2));
    }

    @Test
    void matchesWithGithubPullRequestRefs() {
        Matcher matcher = Consts.PULL_REQUEST_REFS_PATTERN.matcher("refs/pull/45/head");
        assertTrue(matcher.find());
        assertEquals("45", matcher.group(2));
    }

    @Test
    void matchesWithGitLabMergeRequestRefs() {
        Matcher matcher = Consts.PULL_REQUEST_REFS_PATTERN.matcher("refs/merge-requests/42/head");
        assertTrue(matcher.find());
        assertEquals("42", matcher.group(2));
    }

    @Test
    void testCreateValue_StaplerRequest2_ValueInRequest() {
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

        StaplerRequest2 request = mock(StaplerRequest2.class);
        when(request.getParameterValues(instance.getName())).thenReturn(new String[] {"master"});
        Failure failure = assertThrows(Failure.class, () -> instance.createValue(request));
        assertEquals("Parameter name provided value 'master' is invalid", failure.getMessage());
    }

    @Test
    void testConstructorInitializesTagFilterToAsteriskWhenNull() {
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
    void testConstructorInitializesTagFilterToAsteriskWhenWhitespace() {
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
    void testConstructorInitializesTagFilterToAsteriskWhenEmpty() {
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
    void testConstructorInitializesTagToGivenValueWhenNotNullOrWhitespace() {
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
    void testCreateValue_StaplerRequest2_JSONObject() {
        StaplerRequest2 request = mock(StaplerRequest2.class);

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

        Failure failure = assertThrows(Failure.class, () -> instance.createValue(request, jO));
        assertEquals("Parameter Git_param_name value 'Git_param_value' is invalid", failure.getMessage());
    }

    /**
     * Test of getType method, of class GitParameterDefinition.
     */
    @Test
    void testGetParameterType() {
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
    void testSetParameterType() {
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
    void testWrongType() {
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
    void testGetDefaultValue() {
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
    void testSetDefaultValue() {
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
}
