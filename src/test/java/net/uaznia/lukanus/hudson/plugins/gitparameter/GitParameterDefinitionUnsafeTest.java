package net.uaznia.lukanus.hudson.plugins.gitparameter;

import static net.uaznia.lukanus.hudson.plugins.gitparameter.Constants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hudson.cli.CLICommand;
import hudson.cli.ConsoleCommand;
import hudson.model.ParameterValue;
import net.sf.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.StaplerRequest2;

/**
 * @author lukanus
 */
@WithJenkins
class GitParameterDefinitionUnsafeTest {
    @BeforeEach
    void disableParameterValidation() {
        // Disable SECURITY-3419 change for tests in this class
        GitParameterDefinition.allowAnyParameterValue = true;
    }

    @AfterEach
    void enableParameterValidation() {
        GitParameterDefinition.allowAnyParameterValue = false;
    }

    // -------------------

    @Test
    void testRequiredParameterStaplerPass(JenkinsRule jenkins) {
        GitParameterDefinition def = new GitParameterDefinition(
                "testName",
                Consts.PARAMETER_TYPE_BRANCH,
                null,
                "testDescription",
                null,
                ".*master.*",
                "*",
                SortMode.ASCENDING,
                SelectedValue.NONE,
                null,
                false);
        def.setRequiredParameter(true);
        StaplerRequest2 request = mock(StaplerRequest2.class);
        String[] result = new String[] {"master"};
        when(request.getParameterValues("testName")).thenReturn(result);
        def.createValue(request);
    }

    @Test
    void testRequiredParameterJSONPass(JenkinsRule jenkins) {
        GitParameterDefinition def = new GitParameterDefinition(
                "testName",
                Consts.PARAMETER_TYPE_BRANCH,
                null,
                "testDescription",
                null,
                ".*master.*",
                "*",
                SortMode.ASCENDING,
                SelectedValue.NONE,
                null,
                false);
        def.setRequiredParameter(true);
        JSONObject o = new JSONObject();
        o.put("value", "master");
        o.put("name", "testName");
        def.createValue((StaplerRequest2) null, o);
    }

    @Test
    void testCreateValue_CLICommand(JenkinsRule jenkins) throws Exception {
        CLICommand cliCommand = new ConsoleCommand();
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

        String value = "test";
        ParameterValue result = instance.createValue(cliCommand, value);
        assertEquals(new GitParameterValue(NAME, value), result);
    }

    @Test
    void testCreateRequiredValuePass_CLICommand(JenkinsRule jenkins) throws Exception {
        CLICommand cliCommand = new ConsoleCommand();
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
        instance.setRequiredParameter(true);
        String value = "test";
        ParameterValue result = instance.createValue(cliCommand, value);
        assertEquals(new GitParameterValue(NAME, value), result);
    }

    @Test
    void testCreateValue_CLICommand_EmptyValue(JenkinsRule jenkins) throws Exception {
        CLICommand cliCommand = new ConsoleCommand();
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

        ParameterValue result = instance.createValue(cliCommand, null);
        assertEquals(new GitParameterValue(NAME, DEFAULT_VALUE), result);
    }
}
