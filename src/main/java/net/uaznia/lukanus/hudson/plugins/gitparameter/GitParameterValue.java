package net.uaznia.lukanus.hudson.plugins.gitparameter;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.model.StringParameterValue;

public class GitParameterValue extends StringParameterValue {
    private static final long serialVersionUID = -8244244942726975701L;

    @DataBoundConstructor
    public GitParameterValue(String name, String value) {
        super(name, value);
    }
}
