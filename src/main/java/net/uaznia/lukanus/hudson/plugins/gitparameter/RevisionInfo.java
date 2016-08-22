package net.uaznia.lukanus.hudson.plugins.gitparameter;

public class RevisionInfo {
    private String sha1;
    private String revisionInfo;

    public RevisionInfo(String sha1, String revisionInfo) {
        this.sha1 = sha1;
        this.revisionInfo = revisionInfo;
    }

    public String getSha1() {
        return sha1;
    }

    public String getRevisionInfo() {
        return revisionInfo;
    }
}
