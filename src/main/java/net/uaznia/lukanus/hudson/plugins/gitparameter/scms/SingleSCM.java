package net.uaznia.lukanus.hudson.plugins.gitparameter.scms;

import java.util.Collections;
import java.util.List;

import hudson.scm.SCM;

public class SingleSCM implements SCMWrapper {
    private SCM scm;

    public SingleSCM(SCM scm) {
        this.scm = scm;
    }

    @Override
    public List<SCM> getSCMs() {
        return Collections.singletonList(scm);
    }
}
