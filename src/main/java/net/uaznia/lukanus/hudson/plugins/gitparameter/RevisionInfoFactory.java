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
    private static final Pattern AUTHOR_LINE_PATTERN = Pattern.compile("author (.* <.*@.*>) (\\d{10}) ([\\+-]\\d{4})");
    private static final Pattern AUTHOR_LINE_PATTERN_GENERAL_DATE = Pattern.compile("author (.* <.*@.*>) (.*)");

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
        String shortSha1 = revision.getSha1String().substring(0, 8);

        List<String> raw;
        try {
            raw = gitClient.showRevision(revision.getSha1());
        } catch (GitException e1) {
            LOGGER.log(Level.SEVERE, Messages.GitParameterDefinition_unexpectedError(), e1);
            return shortSha1;
        } catch (InterruptedException e1) {
            LOGGER.log(Level.SEVERE, Messages.GitParameterDefinition_unexpectedError(), e1);
            return shortSha1;
        }

        String authorLine = getAuthorLine(raw);
        Matcher matcher = AUTHOR_LINE_PATTERN.matcher(authorLine);
        if (matcher.find()) {
            String author = matcher.group(1);
            String timestamp = matcher.group(2);
            DateTime date = new DateTime(Long.parseLong(timestamp) * 1000); //Convert UNIX timestamp to date
            return shortSha1 + " " + date.toString("yyyy-MM-dd HH:mm") + " " + author;
        }

        matcher = AUTHOR_LINE_PATTERN_GENERAL_DATE.matcher(authorLine);
        if (matcher.find()) {
            String author = matcher.group(1);
            String date = matcher.group(2);
            return shortSha1 + " " + date + " " + author;
        }

        LOGGER.log(Level.WARNING, Messages.GitParameterDefinition_notFindAuthorPattern(authorLine));
        return shortSha1;
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
