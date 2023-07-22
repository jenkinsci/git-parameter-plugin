package net.uaznia.lukanus.hudson.plugins.gitparameter.scms;

import hudson.scm.SCM;
import java.util.List;

public interface SCMWrapper {
    List<SCM> getSCMs();
}
