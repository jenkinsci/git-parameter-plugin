package net.uaznia.lukanus.hudson.plugins.gitparameter.scms;

import java.lang.reflect.Method;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.scm.SCM;

public class MultiSCM implements SCMWrapper {
    public static final String MULTI_SCM_CLASS_NAME = "org.jenkinsci.plugins.multiplescms.MultiSCM";
    private static final Logger LOGGER = Logger.getLogger(MultiSCM.class.getName());
    private SCM scm;

    public MultiSCM(SCM scm) {
        this.scm = scm;
    }

    @Override
    public List<SCM> getSCMs() {
        //((MultiSCM)scm).getConfiguredSCMs()
        try {
            Class<?> clazz = scm.getClass();
            Method getProjectScmMethod = clazz.getDeclaredMethod("getConfiguredSCMs");

            List<SCM> projectSCM = (List<SCM>) getProjectScmMethod.invoke(scm);
            return projectSCM;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, Messages.MultiSCM_getMultiScmFailed(), e);
        }
        return null;
    }
}
