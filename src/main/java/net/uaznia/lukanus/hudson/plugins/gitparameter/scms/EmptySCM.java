package net.uaznia.lukanus.hudson.plugins.gitparameter.scms;

import java.util.Collections;
import java.util.List;

import hudson.scm.SCM;

public class EmptySCM implements SCMWrapper {
    @Override
    public List<SCM> getSCMs() {
        return Collections.emptyList();
    }
}
