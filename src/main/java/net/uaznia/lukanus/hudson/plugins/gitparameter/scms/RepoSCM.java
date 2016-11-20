package net.uaznia.lukanus.hudson.plugins.gitparameter.scms;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.plugins.git.GitSCM;
import hudson.plugins.git.UserRemoteConfig;
import hudson.scm.SCM;

public class RepoSCM implements SCMWrapper {
    public static final String REPO_SCM_CLASS_NAME = "hudson.plugins.repo.RepoScm";
    private static final String REPO_SCM_NAME = "repo";
    private static final String REPO_MANIFESTS_DIR = ".repo/manifests";
    private static final Logger LOGGER = Logger.getLogger(RepoSCM.class.getName());
    private SCM scm;

    public RepoSCM(SCM scm) {
        this.scm = scm;
    }

    @Override
    public List<SCM> getSCMs() {
        try {
            Class<?> clazz = scm.getClass();
            Method method = clazz.getDeclaredMethod("getManifestRepositoryUrl");
            String repositoryUrl = (String) method.invoke(scm);
            UserRemoteConfig config = new UserRemoteConfig(repositoryUrl, REPO_SCM_NAME, null, null);
            List<UserRemoteConfig> configs = new ArrayList<>();
            configs.add(config);
            SCM gitSCM = new GitSCM(configs, null, false, null, null, null, null);
            return Collections.singletonList(gitSCM);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, Messages.RepoSCM_getRepoScmFailed(), e);
        }
        return null;
    }

    public static boolean isRepoSCM(String repositoryName) {
        return REPO_SCM_NAME.equals(repositoryName);
    }

    public static String getRepoMainfestsDir() {
        return REPO_MANIFESTS_DIR;
    }
}
