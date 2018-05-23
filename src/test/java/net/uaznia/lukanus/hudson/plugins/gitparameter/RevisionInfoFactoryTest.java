package net.uaznia.lukanus.hudson.plugins.gitparameter;

import hudson.plugins.git.GitException;
import org.eclipse.jgit.lib.ObjectId;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RevisionInfoFactoryTest {
    private static final String COMMIT_HASH_1 = "ee650d9b5dbc39ec1bdfc6608f49db94ce8d7be4";
    private static final ObjectId SHA1_1 = ObjectId.fromString(COMMIT_HASH_1);
    private static final String[] RAW_1 = {"tree ee650d9b5dbc39ec1bdfc6608f49db94ce8d7be4",
            "parent 3409e0e7a3c0a2887b14c95d803b90f9314606aa",
            "author klimas7 <klimas7@gmail.com> 1523905899 +0200",
            "committer klimas7 <klimas7@gmail.com> 1523905899 +0200"};


    private static final String COMMIT_HASH_2 = "b9a246e842a5478fe01b52eb93e0e23cdb79f616";
    private static final ObjectId SHA1_2 = ObjectId.fromString(COMMIT_HASH_2);
    private static final String[] RAW_2 = {"tree b9a246e842a5478fe01b52eb93e0e23cdb79f616",
            "parent 3409e0e7a3c0a2887b14c95d803b90f9314606aa",
            "author klimas7 <klimas7@gmail.com> 2018-05-23T15:28:13+0200",
            "committer klimas7 <klimas7@gmail.com> 2018-05-23T15:28:13+0200"};

    private static final String COMMIT_HASH_3 = "b9a246e842a5478fe01b52eb93e0e23cdb79f616";
    private static final ObjectId SHA1_3 = ObjectId.fromString(COMMIT_HASH_3);
    private static final String[] RAW_NO_AUTHOR = {"tree b9a246e842a5478fe01b52eb93e0e23cdb79f616",
            "parent 3409e0e7a3c0a2887b14c95d803b90f9314606aa",
            "committer klimas7 <klimas7@gmail.com> 2018-05-23T15:28:13+0200"};
    private static final String SHORT_COMMIT_HASH_3 = COMMIT_HASH_3.substring(0, 8);

    @Test
    public void testGetRevisions() throws InterruptedException {
        GitClient gitClient = mock(GitClient.class);
        when(gitClient.revListAll()).thenReturn(Arrays.asList(SHA1_1, SHA1_2));
        when(gitClient.showRevision(SHA1_1)).thenReturn(Arrays.asList(RAW_1));
        when(gitClient.showRevision(SHA1_2)).thenReturn(Arrays.asList(RAW_2));

        RevisionInfoFactory revisionInfoFactory = new RevisionInfoFactory(gitClient, null);
        List<RevisionInfo> revisions = revisionInfoFactory.getRevisions();

        assertEquals(2, revisions.size());
        assertEquals("ee650d9b 2018-04-16 21:11 klimas7 <klimas7@gmail.com>", revisions.get(0).getRevisionInfo());
        assertEquals("b9a246e8 2018-05-23T15:28:13+0200 klimas7 <klimas7@gmail.com>", revisions.get(1).getRevisionInfo());
    }

    @Test
    public void testNoAuthor() throws InterruptedException {
        GitClient gitClient = mock(GitClient.class);
        when(gitClient.revListAll()).thenReturn(Arrays.asList(SHA1_3));
        when(gitClient.showRevision(SHA1_3)).thenReturn(Arrays.asList(RAW_NO_AUTHOR));

        RevisionInfoFactory revisionInfoFactory = new RevisionInfoFactory(gitClient, null);
        List<RevisionInfo> revisions = revisionInfoFactory.getRevisions();

        assertEquals(1, revisions.size());
        RevisionInfo revisionInfo = revisions.get(0);
        assertEquals(SHORT_COMMIT_HASH_3, revisionInfo.getRevisionInfo());
        assertEquals(COMMIT_HASH_3, revisionInfo.getSha1());
    }

    @Test
    public void testGitException() throws InterruptedException {
        GitClient gitClient = mock(GitClient.class);
        when(gitClient.revListAll()).thenReturn(Arrays.asList(SHA1_3));
        when(gitClient.showRevision(SHA1_3)).thenThrow(new GitException());

        RevisionInfoFactory revisionInfoFactory = new RevisionInfoFactory(gitClient, null);
        List<RevisionInfo> revisions = revisionInfoFactory.getRevisions();

        assertEquals(1, revisions.size());
        RevisionInfo revisionInfo = revisions.get(0);
        assertEquals(SHORT_COMMIT_HASH_3, revisionInfo.getRevisionInfo());
        assertEquals(COMMIT_HASH_3, revisionInfo.getSha1());
    }
}