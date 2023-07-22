package net.uaznia.lukanus.hudson.plugins.gitparameter.scms;

import hudson.scm.SCM;
import java.util.Collections;
import java.util.List;

public class EmptySCM implements SCMWrapper {
    @Override
    public List<SCM> getSCMs() {
        return Collections.emptyList();
    }
}
