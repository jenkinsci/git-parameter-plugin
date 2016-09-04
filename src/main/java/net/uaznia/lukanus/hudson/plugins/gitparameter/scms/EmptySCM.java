package net.uaznia.lukanus.hudson.plugins.gitparameter.scms;

import hudson.scm.SCM;

public class EmptySCM implements SCMWrapper {
    @Override
    public SCM getSCM() {
        return null;
    }
}
