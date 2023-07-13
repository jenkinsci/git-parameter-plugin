package net.uaznia.lukanus.hudson.plugins.gitparameter.scms;

import hudson.scm.SCM;
import java.util.Collections;
import java.util.List;

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
