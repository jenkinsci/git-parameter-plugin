package net.uaznia.lukanus.hudson.plugins.gitparameter.scms;

import java.util.List;

import hudson.scm.SCM;

public interface SCMWrapper {
    List<SCM> getSCMs();
}
