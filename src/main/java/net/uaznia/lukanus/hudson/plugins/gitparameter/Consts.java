package net.uaznia.lukanus.hudson.plugins.gitparameter;

import java.util.regex.Pattern;

public class Consts {
    public static final String DEFAULT_LIST_SIZE = "5";
    public static final String DEFAULT_REMOTE = "origin";
    public static final String REFS_TAGS_PATTERN = ".*refs/tags/";

    public static final String PARAMETER_TYPE_TAG = "PT_TAG";
    public static final String PARAMETER_TYPE_REVISION = "PT_REVISION";
    public static final String PARAMETER_TYPE_BRANCH = "PT_BRANCH";
    public static final String PARAMETER_TYPE_TAG_BRANCH = "PT_BRANCH_TAG";
    public static final String PARAMETER_TYPE_PULL_REQUEST = "PT_PULL_REQUEST";

    public static final Pattern PULL_REQUEST_REFS_PATTERN = Pattern.compile("refs/pull.*/(\\d+)/[from|head]");

    public static final String TEMPORARY_DIRECTORY_PREFIX = "git_parameter_";
    public static final String EMPTY_JOB_NAME = "EMPTY_JOB_NAME";

    public static boolean isParameterTypeCorrect(String type) {
        return type.equals(PARAMETER_TYPE_TAG) || type.equals(PARAMETER_TYPE_REVISION)
                || type.equals(PARAMETER_TYPE_BRANCH) || type.equals(PARAMETER_TYPE_TAG_BRANCH)
                || type.equals(PARAMETER_TYPE_PULL_REQUEST);
    }

    public static boolean isRevisionType(String type) {
        return type.equalsIgnoreCase(PARAMETER_TYPE_REVISION);
    }

    public static boolean isBranchType(String type) {
        return type.equalsIgnoreCase(PARAMETER_TYPE_BRANCH) || type.equalsIgnoreCase(PARAMETER_TYPE_TAG_BRANCH);
    }

    public static boolean isTagType(String type) {
        return type.equalsIgnoreCase(PARAMETER_TYPE_TAG) || type.equalsIgnoreCase(PARAMETER_TYPE_TAG_BRANCH);
    }

    public static boolean isPullRequestType(String type) {
        return type.equalsIgnoreCase(PARAMETER_TYPE_PULL_REQUEST);
    }
}
