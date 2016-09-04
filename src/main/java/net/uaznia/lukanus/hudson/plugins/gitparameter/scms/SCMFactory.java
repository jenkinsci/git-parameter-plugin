package net.uaznia.lukanus.hudson.plugins.gitparameter.scms;

import static net.uaznia.lukanus.hudson.plugins.gitparameter.scms.ProxySCM.PROXY_SCM_CLASS_NAME;
import static net.uaznia.lukanus.hudson.plugins.gitparameter.scms.RepoSCM.REPO_SCM_CLASS_NAME;

import hudson.plugins.git.GitSCM;
import hudson.scm.SCM;
import net.uaznia.lukanus.hudson.plugins.gitparameter.jobs.JobWrapper;

public class SCMFactory {
    private static final SCMWrapper EMPTY_SCM = new EmptySCM();

    public static GitSCM getGitSCM(JobWrapper jobWrapper) {
        SCM scm = getSCM(jobWrapper);
        if (scm instanceof GitSCM) {
            return (GitSCM) scm;
        }

        return null;
    }

    private static SCM getSCM(JobWrapper jobWrapper) {
        if (jobWrapper == null) {
            return EMPTY_SCM.getSCM();
        }

        SCM projectSCM = jobWrapper.getScm();

        if (projectSCM == null) {
            return EMPTY_SCM.getSCM();
        }

        String projectSCMClassName = projectSCM.getClass().getName();
        if (PROXY_SCM_CLASS_NAME.equals(projectSCMClassName)) {
            projectSCM = new ProxySCM(projectSCM).getSCM();
            projectSCMClassName = projectSCM.getClass().getName();
        }

        if (REPO_SCM_CLASS_NAME.equals(projectSCMClassName)) {
            projectSCM = new RepoSCM(projectSCM).getSCM();
        }

        return projectSCM;
    }

}
