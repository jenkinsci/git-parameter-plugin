package net.uaznia.lukanus.hudson.plugins.gitparameter.scms;

import static net.uaznia.lukanus.hudson.plugins.gitparameter.scms.MultiSCM.MULTI_SCM_CLASS_NAME;
import static net.uaznia.lukanus.hudson.plugins.gitparameter.scms.ProxySCM.PROXY_SCM_CLASS_NAME;
import static net.uaznia.lukanus.hudson.plugins.gitparameter.scms.RepoSCM.REPO_SCM_CLASS_NAME;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.google.common.base.Strings;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.UserRemoteConfig;
import hudson.scm.SCM;
import net.uaznia.lukanus.hudson.plugins.gitparameter.jobs.JobWrapper;

public class SCMFactory {
    private static final SCMWrapper EMPTY_SCM = new EmptySCM();
    private static final Logger LOGGER = Logger.getLogger(SCMFactory.class.getName());

    public static List<GitSCM> getGitSCMs(JobWrapper jobWrapper, String repositoryRegExpName) {

        List<SCM> scms = getSCMs(jobWrapper);
        if (scms.isEmpty()) {
            return Collections.EMPTY_LIST;
        }
        if (Strings.isNullOrEmpty(repositoryRegExpName)) {
            return getFirstGitSCM(scms);
        } else {
            Pattern repositoryNamePattern;
            try {
                repositoryNamePattern = Pattern.compile(repositoryRegExpName);
            } catch (Exception e) {
                LOGGER.log(Level.INFO, Messages.SCMFactory_invalidUseRepositoryPattern(repositoryRegExpName), e.getMessage());
                return getFirstGitSCM(scms);
            }
            return matchAndGetGitSCM(scms, repositoryNamePattern);
        }
    }

    private static List<GitSCM> matchAndGetGitSCM(List<SCM> scms, Pattern repositoryNamePattern) {
        List<GitSCM> gitSCMs = new ArrayList<>();
        for (SCM scm : scms) {
            if (scm instanceof GitSCM && anyUserRemoteConfigMatch(scm, repositoryNamePattern)) {
                gitSCMs.add((GitSCM) scm);
            }
        }
        return gitSCMs;
    }

    private static boolean anyUserRemoteConfigMatch(SCM scm, Pattern repositoryNamePattern) {
        List<UserRemoteConfig> userRemoteConfigs = ((GitSCM) scm).getUserRemoteConfigs();
        for (UserRemoteConfig userRemoteConfig : userRemoteConfigs) {
            if (repositoryNamePattern.matcher(userRemoteConfig.getUrl()).find()) {
                return true;
            }
        }
        return false;
    }

    private static List<GitSCM> getFirstGitSCM(List<SCM> scms) {
        SCM scm = scms.get(0);
        if (scm instanceof GitSCM) {
            return Collections.singletonList((GitSCM) scm);
        }
        return Collections.EMPTY_LIST;
    }

    private static List<SCM> getSCMs(JobWrapper jobWrapper) {
        if (jobWrapper == null) {
            return EMPTY_SCM.getSCMs();
        }

        List<SCM> projectSCMsFromJob = jobWrapper.getScms();

        if (projectSCMsFromJob == null || projectSCMsFromJob.isEmpty()) {
            return EMPTY_SCM.getSCMs();
        }

        return transformSCMs(projectSCMsFromJob);
    }

    private static List<SCM> transformSCMs(List<SCM> SCMs) {
        List<SCM> projectSCMs = new ArrayList<>();
        for (SCM scm : SCMs) {
            String projectSCMClassName = scm.getClass().getName();
            if (PROXY_SCM_CLASS_NAME.equals(projectSCMClassName)) {
                List<SCM> SCMsFromProxy = new ProxySCM(scm).getSCMs();
                return transformSCMs(SCMsFromProxy);
            }

            SCMWrapper scmWrapper;
            if (REPO_SCM_CLASS_NAME.equals(projectSCMClassName)) {
                scmWrapper = new RepoSCM(scm);
            } else if (MULTI_SCM_CLASS_NAME.equals(projectSCMClassName)) {
                scmWrapper = new MultiSCM(scm);
            } else {
                scmWrapper = new SingleSCM(scm);
            }
            projectSCMs.addAll(scmWrapper.getSCMs());
        }
        return projectSCMs;
    }

}
