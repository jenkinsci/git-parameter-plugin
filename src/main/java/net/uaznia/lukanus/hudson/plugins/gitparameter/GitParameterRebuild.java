package net.uaznia.lukanus.hudson.plugins.gitparameter;

import com.sonyericsson.rebuild.RebuildParameterPage;
import com.sonyericsson.rebuild.RebuildParameterProvider;
import hudson.Extension;
import hudson.model.ParameterValue;

@Extension(optional = true)
public class GitParameterRebuild extends RebuildParameterProvider {

    @Override
    public RebuildParameterPage getRebuildPage(ParameterValue parameterValue) {
        return new RebuildParameterPage(GitParameterValue.class,"value.jelly");
    }
}
