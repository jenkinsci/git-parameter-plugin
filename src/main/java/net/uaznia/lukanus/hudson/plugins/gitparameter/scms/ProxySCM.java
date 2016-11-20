package net.uaznia.lukanus.hudson.plugins.gitparameter.scms;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.scm.SCM;

public class ProxySCM implements SCMWrapper {
    public static final String PROXY_SCM_CLASS_NAME = "hudson.plugins.templateproject.ProxySCM";
    private static final Logger LOGGER = Logger.getLogger(ProxySCM.class.getName());
    private SCM scm;

    public ProxySCM(SCM scm) {
        this.scm = scm;
    }

    @Override
    public List<SCM> getSCMs() {
        //((ProxySCM)projectScm).getProjectScm()
        try {
            Class<?> clazz = scm.getClass();
            Method getProjectScmMethod = clazz.getDeclaredMethod("getProjectScm");

            SCM projectSCM = (SCM) getProjectScmMethod.invoke(scm);
            return Collections.singletonList(projectSCM);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, Messages.ProxySCM_getSCMFromProxySCM(), e);
        }
        return null;
    }
}
