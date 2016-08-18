package net.uaznia.lukanus.hudson.plugins.gitparameter;

import hudson.plugins.git.GitException;
import hudson.plugins.git.Revision;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.lib.ObjectId;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RevisionInfoFactory {

    private static final Logger LOGGER = Logger.getLogger(RevisionInfoFactory.class.getName());
    public static final Pattern AUTHOR_LINE_PATTERN = Pattern.compile("author (.* <.*@.*>) (\\d{10}) ([\\+-]\\d{4})");

    private GitClient gitClient;
    private String branch;

    public RevisionInfoFactory(GitClient gitClient, String branch) {
        this.gitClient = gitClient;
        this.branch = branch;
    }

    public List<RevisionInfo> getRevisions() throws InterruptedException {
        List<ObjectId> objectIds;

        if (StringUtils.isEmpty(branch)) {
            objectIds = gitClient.revListAll();
        } else {
            objectIds = gitClient.revList(branch);
        }

        ArrayList<RevisionInfo> revisionInfoList = new ArrayList<RevisionInfo>(objectIds.size());
        for (ObjectId objectId : objectIds) {
            Revision revision = new Revision(objectId);
            revisionInfoList.add(new RevisionInfo(revision.getSha1String(), prettyRevisionInfo(revision)));
        }

        return revisionInfoList;
    }

    private String prettyRevisionInfo(Revision revision) {
        List<String> raw = null;
        try {
            raw = gitClient.showRevision(revision.getSha1());
        } catch (GitException e1) {
            LOGGER.log(Level.SEVERE, Messages.GitParameterDefinition_unexpectedError(), e1);
            return "";
        } catch (InterruptedException e1) {
            LOGGER.log(Level.SEVERE, Messages.GitParameterDefinition_unexpectedError(), e1);
            return "";
        }

        String authorLine = getAuthorLine(raw);
        Matcher matcher = AUTHOR_LINE_PATTERN.matcher(authorLine);
        if (matcher.find()) {
            String author = matcher.group(1);
            String timestamp = matcher.group(2);
            DateTime date = new DateTime(Long.parseLong(timestamp) * 1000); //Convert UNIX timestamp to date
            return revision.getSha1String().substring(0, 8) + " " + date.toString("yyyy:MM:dd HH:mm") + " " + author;
        } else {
            LOGGER.log(Level.WARNING, Messages.GitParameterDefinition_notFindAuthorPattern(authorLine));
            return "";
        }
    }

    private String getAuthorLine(List<String> rows) {
        for (String row : rows) {
            if (StringUtils.isNotEmpty(row) && row.toLowerCase().startsWith("author")) {
                return row;
            }
        }
        return "";
    }
}
